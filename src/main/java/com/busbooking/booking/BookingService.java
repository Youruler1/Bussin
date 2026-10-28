package com.busbooking.booking;

import com.busbooking.auth.User;
import com.busbooking.auth.UserRepository;
import com.busbooking.booking.dto.*;
import com.busbooking.common.exception.CustomerNotFoundException;
import com.busbooking.common.exception.ResourceConflictException;
import com.busbooking.common.exception.ResourceNotFoundException;
import com.busbooking.station.Station;
import com.busbooking.trip.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingLegRepository bookingLegRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final TripStopFareRepository tripStopFareRepository;
    private final TripSeatRepository tripSeatRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingLegRepository bookingLegRepository,
                          SeatAllocationRepository seatAllocationRepository,
                          TripRepository tripRepository,
                          TripStopRepository tripStopRepository,
                          TripStopFareRepository tripStopFareRepository,
                          TripSeatRepository tripSeatRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingLegRepository = bookingLegRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.tripStopFareRepository = tripStopFareRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(Long customerId, CreateBookingRequest request, String idempotencyKey) {
        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // 2. Customer Validation
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        if (request.passengerBookings() == null || request.passengerBookings().isEmpty()) {
            throw new IllegalArgumentException("Booking must contain at least one passenger");
        }

        // 3. Collect and Lock Mutex Rows in Deterministic Order (ORDER BY trip_id ASC, seat_id ASC)
        Map<Long, Set<Long>> tripToSeatIds = new TreeMap<>(); // TreeMap guarantees ascending trip_id order

        for (PassengerLegBooking passenger : request.passengerBookings()) {
            if (passenger.legAllocations() == null || passenger.legAllocations().isEmpty()) {
                throw new IllegalArgumentException("Each passenger must have at least one leg allocation");
            }
            for (LegAllocationRequest legReq : passenger.legAllocations()) {
                if (legReq.boardStopOrder() >= legReq.alightStopOrder()) {
                    throw new IllegalArgumentException("Board stop order must be strictly less than alight stop order");
                }
                tripToSeatIds.computeIfAbsent(legReq.tripId(), k -> new TreeSet<>()).add(legReq.seatId());
            }
        }

        // Acquire row mutex locks in global ascending (tripId, seatId) order
        Map<String, TripSeat> lockedTripSeats = new HashMap<>();

        for (Map.Entry<Long, Set<Long>> entry : tripToSeatIds.entrySet()) {
            Long tripId = entry.getKey();
            List<Long> sortedSeatIds = new ArrayList<>(entry.getValue()); // TreeSet provides ascending order

            List<TripSeat> lockedSeats = tripSeatRepository.findAndLockByTripIdAndSeatIdsOrdered(tripId, sortedSeatIds);
            for (TripSeat ts : lockedSeats) {
                lockedTripSeats.put(tripId + "_" + ts.getSeat().getId(), ts);
            }

            // Verify all requested seats exist for this trip
            for (Long reqSeatId : sortedSeatIds) {
                if (!lockedTripSeats.containsKey(tripId + "_" + reqSeatId)) {
                    throw new ResourceNotFoundException("Seat ID " + reqSeatId + " not found for trip ID " + tripId);
                }
            }
        }

        // 4. Overlap Validation (DB allocations + Intra-booking collisions)
        Map<Long, List<int[]>> intraBookingAllocatedIntervals = new HashMap<>();

        for (PassengerLegBooking passenger : request.passengerBookings()) {
            for (LegAllocationRequest legReq : passenger.legAllocations()) {
                TripSeat tripSeat = lockedTripSeats.get(legReq.tripId() + "_" + legReq.seatId());

                if (!tripSeat.getSeat().isActive()) {
                    throw new ResourceConflictException("Seat " + tripSeat.getSeat().getSeatNumber() + " is inactive and cannot be booked");
                }

                // Check DB overlapping active allocations
                List<SeatAllocation> overlapping = seatAllocationRepository.findOverlappingAllocations(
                        tripSeat.getId(),
                        legReq.boardStopOrder(),
                        legReq.alightStopOrder()
                );

                if (!overlapping.isEmpty()) {
                    throw new ResourceConflictException("Seat " + tripSeat.getSeat().getSeatNumber()
                            + " is already booked for an overlapping segment on trip "
                            + tripSeat.getTrip().getTripNumber());
                }

                // Check Intra-booking collision
                List<int[]> currentIntervals = intraBookingAllocatedIntervals.computeIfAbsent(tripSeat.getId(), k -> new ArrayList<>());
                for (int[] allocated : currentIntervals) {
                    if (legReq.boardStopOrder() < allocated[1] && allocated[0] < legReq.alightStopOrder()) {
                        throw new ResourceConflictException("Duplicate seat " + tripSeat.getSeat().getSeatNumber()
                                + " requested for overlapping segments within the same booking");
                    }
                }
                currentIntervals.add(new int[]{legReq.boardStopOrder(), legReq.alightStopOrder()});
            }
        }

        // 5. Build Itinerary Legs & Snapshot Fares
        // Determine distinct legs across the booking: grouped by (tripId, boardStopOrder, alightStopOrder)
        Map<String, LegTemplate> distinctLegs = new LinkedHashMap<>();

        for (PassengerLegBooking passenger : request.passengerBookings()) {
            for (LegAllocationRequest legReq : passenger.legAllocations()) {
                String legKey = legReq.tripId() + "_" + legReq.boardStopOrder() + "_" + legReq.alightStopOrder();
                if (!distinctLegs.containsKey(legKey)) {
                    Trip trip = tripRepository.findById(legReq.tripId()).orElseThrow();
                    TripStop boardStop = tripStopRepository.findByTripIdAndStopOrder(legReq.tripId(), legReq.boardStopOrder())
                            .orElseThrow(() -> new ResourceNotFoundException("Board stop not found for trip " + legReq.tripId()));
                    TripStop alightStop = tripStopRepository.findByTripIdAndStopOrder(legReq.tripId(), legReq.alightStopOrder())
                            .orElseThrow(() -> new ResourceNotFoundException("Alight stop not found for trip " + legReq.tripId()));

                    BigDecimal singleFare = tripStopFareRepository
                            .findByTripIdAndBoardStopOrderAndAlightStopOrder(legReq.tripId(), legReq.boardStopOrder(), legReq.alightStopOrder())
                            .map(TripStopFare::getBaseFareInr)
                            .orElse(BigDecimal.valueOf(100.00));

                    distinctLegs.put(legKey, new LegTemplate(trip, boardStop, alightStop, singleFare));
                }
            }
        }

        // Sort legs chronologically by boarding scheduled departure
        List<Map.Entry<String, LegTemplate>> sortedLegEntries = new ArrayList<>(distinctLegs.entrySet());
        sortedLegEntries.sort(Comparator.comparing(e -> e.getValue().boardStop().getScheduledDepartureUtc()));

        String reference = "BK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        String savedIdempotencyKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : null;

        Booking booking = new Booking(
                reference,
                customer,
                "CONFIRMED",
                BigDecimal.ZERO, // will be updated with sum of leg fares
                request.contactEmail().trim(),
                request.contactPhone().trim(),
                savedIdempotencyKey
        );

        BigDecimal totalBookingAmount = BigDecimal.ZERO;
        int legSeq = 1;

        Map<String, BookingLeg> legKeyToBookingLeg = new HashMap<>();

        for (Map.Entry<String, LegTemplate> entry : sortedLegEntries) {
            String legKey = entry.getKey();
            LegTemplate tmpl = entry.getValue();

            BookingLeg bookingLeg = new BookingLeg(
                    booking,
                    tmpl.trip(),
                    legSeq++,
                    tmpl.boardStop().getStation(),
                    tmpl.alightStop().getStation(),
                    tmpl.boardStop().getStopOrder(),
                    tmpl.alightStop().getStopOrder(),
                    tmpl.farePerPassenger() // snapshot fare per passenger
            );
            booking.addLeg(bookingLeg);
            legKeyToBookingLeg.put(legKey, bookingLeg);
        }

        // 6. Allocate Seats to Passengers
        for (PassengerLegBooking passenger : request.passengerBookings()) {
            for (LegAllocationRequest legReq : passenger.legAllocations()) {
                String legKey = legReq.tripId() + "_" + legReq.boardStopOrder() + "_" + legReq.alightStopOrder();
                BookingLeg leg = legKeyToBookingLeg.get(legKey);
                TripSeat tripSeat = lockedTripSeats.get(legReq.tripId() + "_" + legReq.seatId());

                SeatAllocation allocation = new SeatAllocation(
                        leg,
                        tripSeat,
                        legReq.boardStopOrder(),
                        legReq.alightStopOrder(),
                        passenger.passengerName().trim(),
                        passenger.passengerAge(),
                        passenger.passengerGender().trim(),
                        "CONFIRMED"
                );
                leg.addAllocation(allocation);
                totalBookingAmount = totalBookingAmount.add(leg.getFareSnapshotInr());
            }
        }

        booking.setTotalAmountInr(totalBookingAmount);
        booking = bookingRepository.save(booking);

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Long currentUserId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        if (!isAdmin && !booking.getCustomer().getId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have access to view this booking");
        }

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, Long currentUserId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        if (!isAdmin && !booking.getCustomer().getId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to cancel this booking");
        }

        // Idempotent cancellation: if already CANCELLED, return existing state
        if ("CANCELLED".equals(booking.getStatus())) {
            return toResponse(booking);
        }

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(Instant.now());

        for (BookingLeg leg : booking.getLegs()) {
            for (SeatAllocation allocation : leg.getAllocations()) {
                allocation.setStatus("CANCELLED");
            }
        }

        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getCustomerBookings(Long customerId) {
        List<Booking> bookings = bookingRepository.findByCustomerIdOrderByBookedAtDesc(customerId);
        return bookings.stream().map(this::toSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "bookedAt"));
        return bookings.stream().map(this::toSummaryResponse).toList();
    }

    public BookingSummaryResponse toSummaryResponse(Booking booking) {
        String routeSummary = "";
        int legCount = booking.getLegs().size();
        int passengerCount = 0;

        if (legCount > 0) {
            BookingLeg firstLeg = booking.getLegs().get(0);
            passengerCount = firstLeg.getAllocations().size();
            if (legCount == 1) {
                routeSummary = firstLeg.getBoardStation().getName() + " -> " + firstLeg.getAlightStation().getName();
            } else {
                List<String> stations = new ArrayList<>();
                stations.add(firstLeg.getBoardStation().getName());
                for (BookingLeg leg : booking.getLegs()) {
                    stations.add(leg.getAlightStation().getName());
                }
                routeSummary = String.join(" -> ", stations);
            }
        }

        return new BookingSummaryResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus(),
                booking.getTotalAmountInr(),
                booking.getBookedAt(),
                booking.getCancelledAt(),
                routeSummary,
                legCount,
                passengerCount
        );
    }

    public BookingResponse toResponse(Booking booking) {
        List<BookingLegDetail> legDetails = booking.getLegs().stream().map(leg -> {
            List<AllocatedSeatDetail> seatDetails = leg.getAllocations().stream().map(alloc ->
                    new AllocatedSeatDetail(
                            alloc.getTripSeat().getSeat().getSeatNumber(),
                            alloc.getPassengerName(),
                            alloc.getPassengerAge(),
                            alloc.getPassengerGender(),
                            leg.getFareSnapshotInr()
                    )
            ).toList();

            TripStop boardStop = tripStopRepository.findByTripIdAndStopOrder(leg.getTrip().getId(), leg.getBoardStopOrder()).orElse(null);
            TripStop alightStop = tripStopRepository.findByTripIdAndStopOrder(leg.getTrip().getId(), leg.getAlightStopOrder()).orElse(null);

            Instant depTime = boardStop != null ? boardStop.getScheduledDepartureUtc() : leg.getTrip().getScheduledDepartureUtc();
            Instant arrTime = alightStop != null ? alightStop.getScheduledArrivalUtc() : leg.getTrip().getScheduledArrivalUtc();

            return new BookingLegDetail(
                    leg.getTrip().getId(),
                    leg.getTrip().getTripNumber(),
                    leg.getBoardStation().getName(),
                    leg.getAlightStation().getName(),
                    depTime,
                    arrTime,
                    seatDetails
            );
        }).toList();

        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus(),
                booking.getTotalAmountInr(),
                booking.getContactEmail(),
                booking.getContactPhone(),
                booking.getBookedAt(),
                booking.getCancelledAt(),
                legDetails
        );
    }

    private record LegTemplate(
            Trip trip,
            TripStop boardStop,
            TripStop alightStop,
            BigDecimal farePerPassenger
    ) {}
}
