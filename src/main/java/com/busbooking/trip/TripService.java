package com.busbooking.trip;

import com.busbooking.booking.BookingLegRepository;
import com.busbooking.bus.Bus;
import com.busbooking.bus.BusRepository;
import com.busbooking.bus.Seat;
import com.busbooking.bus.SeatRepository;
import com.busbooking.common.exception.BusNotFoundException;
import com.busbooking.common.exception.DuplicateResourceException;
import com.busbooking.common.exception.ResourceConflictException;
import com.busbooking.common.exception.RouteNotFoundException;
import com.busbooking.common.exception.TripNotFoundException;
import com.busbooking.route.Route;
import com.busbooking.route.RouteRepository;
import com.busbooking.route.RouteStop;
import com.busbooking.route.RouteStopRepository;
import com.busbooking.trip.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final TripStopFareRepository tripStopFareRepository;
    private final TripSeatRepository tripSeatRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final BookingLegRepository bookingLegRepository;

    public TripService(TripRepository tripRepository,
                       TripStopRepository tripStopRepository,
                       TripStopFareRepository tripStopFareRepository,
                       TripSeatRepository tripSeatRepository,
                       RouteRepository routeRepository,
                       RouteStopRepository routeStopRepository,
                       BusRepository busRepository,
                       SeatRepository seatRepository,
                       BookingLegRepository bookingLegRepository) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.tripStopFareRepository = tripStopFareRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.busRepository = busRepository;
        this.seatRepository = seatRepository;
        this.bookingLegRepository = bookingLegRepository;
    }

    public List<TripResponse> getAllTrips() {
        return tripRepository.findAllByOrderByScheduledDepartureUtcDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public TripResponse getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new TripNotFoundException("Trip not found with ID: " + id));
        return toResponse(trip);
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        String tripNumber = request.tripNumber().trim().toUpperCase();
        if (tripRepository.existsByTripNumber(tripNumber)) {
            throw new DuplicateResourceException("Trip with number '" + tripNumber + "' already exists");
        }

        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + request.routeId()));

        if (!route.isActive()) {
            throw new IllegalArgumentException("Cannot schedule a trip on an inactive route");
        }

        List<RouteStop> routeStops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
        if (routeStops.size() < 2) {
            throw new IllegalArgumentException("Route must contain at least 2 stops to schedule a trip");
        }

        Bus bus = busRepository.findById(request.busId())
                .orElseThrow(() -> new BusNotFoundException("Bus not found with ID: " + request.busId()));

        if (!bus.isActive()) {
            throw new IllegalArgumentException("Cannot assign an inactive bus to a trip");
        }

        List<Seat> activeSeats = seatRepository.findByBusIdAndIsActiveTrueOrderByRowNumberAscColumnNumberAsc(bus.getId());
        if (activeSeats.isEmpty()) {
            throw new IllegalArgumentException("Assigned bus has no active seats");
        }

        int dwellMinutes = (request.dwellMinutes() != null && request.dwellMinutes() >= 0) ? request.dwellMinutes() : 10;
        BigDecimal baseRatePerKm = (request.baseFarePerKm() != null && request.baseFarePerKm().compareTo(BigDecimal.ZERO) > 0)
                ? request.baseFarePerKm()
                : BigDecimal.valueOf(2.00);

        int n = routeStops.size();
        Instant currentDeparture = request.scheduledDepartureUtc();
        BigDecimal currentDistance = BigDecimal.ZERO;

        List<TripStop> tripStops = new ArrayList<>(n);
        BigDecimal[] cumulativeDistances = new BigDecimal[n];

        // 1. Process Stop 0 (Origin)
        RouteStop stop0 = routeStops.get(0);
        cumulativeDistances[0] = BigDecimal.ZERO;
        TripStop tripStop0 = new TripStop(null, stop0.getStation(), 0, null, currentDeparture, BigDecimal.ZERO);
        tripStops.add(tripStop0);

        // 2. Process Intermediate and Destination Stops (1 .. n-1)
        for (int k = 1; k < n; k++) {
            RouteStop routeStop = routeStops.get(k);
            int durationMins = routeStop.getTypicalDurationMinutes();
            if (durationMins < 0) durationMins = 0;

            Instant arrival = currentDeparture.plus(Duration.ofMinutes(durationMins));
            if (!arrival.isAfter(currentDeparture) && durationMins > 0) {
                throw new IllegalArgumentException("Arrival time at stop " + k + " must be strictly after previous departure");
            }

            Instant nextDeparture = null;
            if (k < n - 1) {
                nextDeparture = arrival.plus(Duration.ofMinutes(dwellMinutes));
                if (!nextDeparture.isAfter(arrival) && dwellMinutes > 0) {
                    throw new IllegalArgumentException("Departure time at stop " + k + " must be strictly after arrival time");
                }
                currentDeparture = nextDeparture;
            }

            BigDecimal segmentDist = routeStop.getDistanceFromPrevKm() != null ? routeStop.getDistanceFromPrevKm() : BigDecimal.ZERO;
            currentDistance = currentDistance.add(segmentDist);
            cumulativeDistances[k] = currentDistance;

            TripStop tripStop = new TripStop(null, routeStop.getStation(), k, arrival, nextDeparture, currentDistance);
            tripStops.add(tripStop);
        }

        Instant finalArrival = tripStops.get(n - 1).getScheduledArrivalUtc();
        if (!finalArrival.isAfter(request.scheduledDepartureUtc())) {
            throw new IllegalArgumentException("Scheduled arrival time must be strictly after scheduled departure time");
        }

        Trip trip = new Trip(
                tripNumber,
                route,
                bus,
                request.serviceDate(),
                "SCHEDULED",
                request.scheduledDepartureUtc(),
                finalArrival
        );

        // Link trip stops
        for (TripStop ts : tripStops) {
            trip.addStop(ts);
        }

        // 3. Process Segment Fares
        Map<String, BigDecimal> customFareMap = new HashMap<>();
        if (request.customFares() != null) {
            for (CreateTripStopFareRequest fareReq : request.customFares()) {
                customFareMap.put(fareReq.boardStopOrder() + "_" + fareReq.alightStopOrder(), fareReq.baseFareInr());
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                String key = i + "_" + j;
                BigDecimal fareAmount = customFareMap.get(key);
                if (fareAmount == null) {
                    BigDecimal dist = cumulativeDistances[j].subtract(cumulativeDistances[i]);
                    fareAmount = dist.multiply(baseRatePerKm).setScale(2, RoundingMode.HALF_UP);
                    if (fareAmount.compareTo(BigDecimal.valueOf(50.00)) < 0) {
                        fareAmount = BigDecimal.valueOf(50.00);
                    }
                }
                TripStopFare fare = new TripStopFare(trip, i, j, fareAmount);
                trip.addFare(fare);
            }
        }

        // 4. Generate TripSeat Mutex Rows for all active seats
        for (Seat seat : activeSeats) {
            TripSeat tripSeat = new TripSeat(trip, seat);
            trip.addSeat(tripSeat);
        }

        trip = tripRepository.save(trip);
        return toResponse(trip);
    }

    @Transactional
    public TripResponse updateTripStatus(Long id, String newStatus) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new TripNotFoundException("Trip not found with ID: " + id));

        trip.setStatus(newStatus.trim().toUpperCase());
        trip = tripRepository.save(trip);
        return toResponse(trip);
    }

    @Transactional(noRollbackFor = ResourceConflictException.class)
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new TripNotFoundException("Trip not found with ID: " + id));
        if (!bookingLegRepository.findByTripId(id).isEmpty()) {
            trip.setStatus("CANCELLED");
            tripRepository.save(trip);
            throw new ResourceConflictException("Cannot delete trip referenced by existing bookings. Trip has been marked CANCELLED instead.");
        }
        tripRepository.delete(trip);
    }

    public TripResponse toResponse(Trip trip) {
        List<TripStopResponse> stopResponses = trip.getStops().stream()
                .map(this::toStopResponse)
                .toList();

        List<TripStopFareResponse> fareResponses = trip.getFares().stream()
                .map(this::toFareResponse)
                .toList();

        return new TripResponse(
                trip.getId(),
                trip.getTripNumber(),
                trip.getRoute().getId(),
                trip.getRoute().getRouteCode(),
                trip.getRoute().getName(),
                trip.getBus().getId(),
                trip.getBus().getRegistrationNumber(),
                trip.getBus().getCategory().getName(),
                trip.getBus().getTotalCapacity(),
                trip.getServiceDate(),
                trip.getStatus(),
                trip.getScheduledDepartureUtc(),
                trip.getScheduledArrivalUtc(),
                stopResponses,
                fareResponses,
                trip.getSeats().size(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }

    public TripStopResponse toStopResponse(TripStop stop) {
        return new TripStopResponse(
                stop.getId(),
                stop.getStation().getId(),
                stop.getStation().getCode(),
                stop.getStation().getName(),
                stop.getStation().getCity(),
                stop.getStopOrder(),
                stop.getScheduledArrivalUtc(),
                stop.getScheduledDepartureUtc(),
                stop.getCumulativeDistanceKm()
        );
    }

    public TripStopFareResponse toFareResponse(TripStopFare fare) {
        return new TripStopFareResponse(
                fare.getId(),
                fare.getBoardStopOrder(),
                fare.getAlightStopOrder(),
                fare.getBaseFareInr()
        );
    }
}
