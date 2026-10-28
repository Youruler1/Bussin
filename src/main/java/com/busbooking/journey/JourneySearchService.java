package com.busbooking.journey;

import com.busbooking.booking.SeatAllocationRepository;
import com.busbooking.common.exception.StationNotFoundException;
import com.busbooking.common.exception.TripNotFoundException;
import com.busbooking.journey.dto.*;
import com.busbooking.station.Station;
import com.busbooking.station.StationRepository;
import com.busbooking.trip.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class JourneySearchService {

    private final TripRepository tripRepository;
    private final TripStopFareRepository tripStopFareRepository;
    private final TripSeatRepository tripSeatRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final StationRepository stationRepository;

    public JourneySearchService(TripRepository tripRepository,
                                TripStopFareRepository tripStopFareRepository,
                                TripSeatRepository tripSeatRepository,
                                SeatAllocationRepository seatAllocationRepository,
                                StationRepository stationRepository) {
        this.tripRepository = tripRepository;
        this.tripStopFareRepository = tripStopFareRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.stationRepository = stationRepository;
    }

    public JourneySearchResult searchJourneys(Long originStationId,
                                             Long destinationStationId,
                                             LocalDate travelDate,
                                             LocalTime departureFrom,
                                             LocalTime departureTo,
                                             int page,
                                             int size) {
        if (originStationId == null || destinationStationId == null) {
            throw new IllegalArgumentException("Origin and destination station IDs are required");
        }
        if (originStationId.equals(destinationStationId)) {
            throw new IllegalArgumentException("Origin and destination stations must be distinct");
        }
        if (travelDate == null) {
            throw new IllegalArgumentException("Travel date is required");
        }
        if (departureFrom != null && departureTo != null && departureTo.isBefore(departureFrom)) {
            throw new IllegalArgumentException("departureTo cannot be before departureFrom");
        }

        Station origin = stationRepository.findById(originStationId)
                .orElseThrow(() -> new StationNotFoundException("Origin station not found with ID: " + originStationId));

        Station destination = stationRepository.findById(destinationStationId)
                .orElseThrow(() -> new StationNotFoundException("Destination station not found with ID: " + destinationStationId));

        ZoneId zoneId = ZoneId.of(origin.getTimezone() != null ? origin.getTimezone() : "Asia/Kolkata");

        LocalTime fromTime = (departureFrom != null) ? departureFrom : LocalTime.MIN;
        LocalTime toTime = (departureTo != null) ? departureTo : LocalTime.MAX;

        Instant departureFromUtc = travelDate.atTime(fromTime).atZone(zoneId).toInstant();
        Instant departureToUtc = travelDate.atTime(toTime).atZone(zoneId).toInstant();

        int p = Math.max(0, page);
        int s = (size > 0 && size <= 100) ? size : 20;
        Pageable pageable = PageRequest.of(p, s);

        // 1. Direct Journey Search First
        List<JourneyOption> directOptions = findDirectJourneys(
                originStationId,
                destinationStationId,
                departureFromUtc,
                departureToUtc,
                pageable
        );

        if (!directOptions.isEmpty()) {
            return new JourneySearchResult("DIRECT", directOptions, directOptions.size(), p, s);
        }

        // 2. Automated Fallback: Connecting Journey Search (Single Transfer Point, 30m <= layover <= 480m)
        List<JourneyOption> connectingOptions = findConnectingJourneys(
                originStationId,
                destinationStationId,
                departureFromUtc,
                departureToUtc,
                pageable
        );

        if (!connectingOptions.isEmpty()) {
            return new JourneySearchResult("CONNECTING", connectingOptions, connectingOptions.size(), p, s);
        }

        return new JourneySearchResult("NONE", Collections.emptyList(), 0, p, s);
    }

    private List<JourneyOption> findDirectJourneys(Long originStationId,
                                                  Long destinationStationId,
                                                  Instant departureFromUtc,
                                                  Instant departureToUtc,
                                                  Pageable pageable) {
        List<Object[]> rows = tripRepository.findDirectTripsBetweenStations(
                originStationId,
                destinationStationId,
                departureFromUtc,
                departureToUtc,
                pageable
        );

        List<JourneyOption> options = new ArrayList<>();

        for (Object[] row : rows) {
            Trip trip = (Trip) row[0];
            TripStop boardStop = (TripStop) row[1];
            TripStop alightStop = (TripStop) row[2];

            JourneyLegSummary legSummary = buildLegSummary(trip, boardStop, alightStop);

            int durationMinutes = (int) Duration.between(
                    boardStop.getScheduledDepartureUtc(),
                    alightStop.getScheduledArrivalUtc()
            ).toMinutes();

            JourneyOption option = new JourneyOption(
                    "DIRECT",
                    durationMinutes,
                    legSummary.segmentFareInr(),
                    List.of(legSummary)
            );
            options.add(option);
        }
        return options;
    }

    private List<JourneyOption> findConnectingJourneys(Long originStationId,
                                                      Long destinationStationId,
                                                      Instant departureFromUtc,
                                                      Instant departureToUtc,
                                                      Pageable pageable) {
        List<Object[]> firstLegRows = tripRepository.findCandidateFirstLegs(
                originStationId,
                departureFromUtc,
                departureToUtc
        );

        List<JourneyOption> options = new ArrayList<>();

        for (Object[] row1 : firstLegRows) {
            Trip trip1 = (Trip) row1[0];
            TripStop board1 = (TripStop) row1[1];
            TripStop alight1 = (TripStop) row1[2];

            Long transferStationId = alight1.getStation().getId();
            // Disallow transfer station being origin or destination
            if (transferStationId.equals(originStationId) || transferStationId.equals(destinationStationId)) {
                continue;
            }

            Instant arrivalAtTransfer = alight1.getScheduledArrivalUtc();
            if (arrivalAtTransfer == null) {
                continue;
            }

            // Layover constraint: 30 minutes minimum, 8 hours (480 minutes) maximum
            Instant minTransferDeparture = arrivalAtTransfer.plus(Duration.ofMinutes(30));
            Instant maxTransferDeparture = arrivalAtTransfer.plus(Duration.ofMinutes(480));

            List<Object[]> secondLegRows = tripRepository.findCandidateSecondLegs(
                    transferStationId,
                    destinationStationId,
                    minTransferDeparture,
                    maxTransferDeparture,
                    trip1.getId()
            );

            for (Object[] row2 : secondLegRows) {
                Trip trip2 = (Trip) row2[0];
                TripStop board2 = (TripStop) row2[1];
                TripStop alight2 = (TripStop) row2[2];

                JourneyLegSummary leg1 = buildLegSummary(trip1, board1, alight1);
                JourneyLegSummary leg2 = buildLegSummary(trip2, board2, alight2);

                int totalDurationMinutes = (int) Duration.between(
                        board1.getScheduledDepartureUtc(),
                        alight2.getScheduledArrivalUtc()
                ).toMinutes();

                BigDecimal totalFare = leg1.segmentFareInr().add(leg2.segmentFareInr());

                JourneyOption option = new JourneyOption(
                        "CONNECTING",
                        totalDurationMinutes,
                        totalFare,
                        List.of(leg1, leg2)
                );
                options.add(option);
            }
        }

        // Sort by total duration ascending
        options.sort(Comparator.comparingInt(JourneyOption::totalDurationMinutes));

        // Apply pagination
        int start = (int) pageable.getOffset();
        if (start >= options.size()) {
            return Collections.emptyList();
        }
        int end = Math.min(start + pageable.getPageSize(), options.size());
        return options.subList(start, end);
    }

    private JourneyLegSummary buildLegSummary(Trip trip, TripStop boardStop, TripStop alightStop) {
        int boardOrder = boardStop.getStopOrder();
        int alightOrder = alightStop.getStopOrder();

        BigDecimal segmentFare = tripStopFareRepository
                .findByTripIdAndBoardStopOrderAndAlightStopOrder(trip.getId(), boardOrder, alightOrder)
                .map(TripStopFare::getBaseFareInr)
                .orElse(BigDecimal.valueOf(100.00));

        long totalSeats = tripSeatRepository.countByTripId(trip.getId());
        long occupiedSeats = seatAllocationRepository.countOccupiedTripSeats(trip.getId(), boardOrder, alightOrder);
        int availableSeats = Math.max(0, (int) (totalSeats - occupiedSeats));

        return new JourneyLegSummary(
                trip.getId(),
                trip.getTripNumber(),
                trip.getBus().getCategory().getName(),
                trip.getBus().getRegistrationNumber(),
                boardStop.getStation().getId(),
                boardStop.getStation().getName(),
                alightStop.getStation().getId(),
                alightStop.getStation().getName(),
                boardOrder,
                alightOrder,
                boardStop.getScheduledDepartureUtc(),
                alightStop.getScheduledArrivalUtc(),
                availableSeats,
                segmentFare
        );
    }

    public TripSeatMapResponse getTripSeats(Long tripId, int boardIndex, int alightIndex) {
        if (boardIndex >= alightIndex) {
            throw new IllegalArgumentException("boardIndex must be strictly less than alightIndex");
        }
        if (boardIndex < 0) {
            throw new IllegalArgumentException("boardIndex cannot be negative");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trip not found with ID: " + tripId));

        if (alightIndex >= trip.getStops().size()) {
            throw new IllegalArgumentException("alightIndex exceeds maximum stop order (" + (trip.getStops().size() - 1) + ")");
        }

        List<TripSeat> tripSeats = tripSeatRepository.findByTripId(tripId);
        List<Long> tripSeatIds = tripSeats.stream().map(TripSeat::getId).toList();

        Set<Long> occupiedIds = tripSeatIds.isEmpty()
                ? Collections.emptySet()
                : seatAllocationRepository.findOccupiedTripSeatIds(tripSeatIds, boardIndex, alightIndex);

        List<SeatItem> seatItems = tripSeats.stream()
                .map(ts -> {
                    boolean isAvailable = ts.getSeat().isActive() && !occupiedIds.contains(ts.getId());
                    return new SeatItem(
                            ts.getSeat().getId(),
                            ts.getId(),
                            ts.getSeat().getSeatNumber(),
                            ts.getSeat().getRowNumber(),
                            ts.getSeat().getColumnNumber(),
                            ts.getSeat().getSeatType(),
                            ts.getSeat().getDeck(),
                            isAvailable
                    );
                })
                .sorted(Comparator.comparingInt(SeatItem::rowNumber).thenComparingInt(SeatItem::columnNumber))
                .toList();

        int availableSeatsCount = (int) seatItems.stream().filter(SeatItem::available).count();

        return new TripSeatMapResponse(
                trip.getId(),
                trip.getTripNumber(),
                trip.getBus().getTotalCapacity(),
                boardIndex,
                alightIndex,
                availableSeatsCount,
                seatItems
        );
    }
}
