package com.busbooking.trip;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByTripNumber(String tripNumber);
    boolean existsByTripNumber(String tripNumber);
    boolean existsByBusId(Long busId);
    boolean existsByRouteId(Long routeId);
    List<Trip> findByServiceDateAndStatus(LocalDate serviceDate, String status);
    List<Trip> findByRouteId(Long routeId);
    List<Trip> findAllByOrderByScheduledDepartureUtcDesc();

    @Query("""
        SELECT t, bs, as_stop
        FROM Trip t
        JOIN t.stops bs
        JOIN t.stops as_stop
        WHERE bs.station.id = :originStationId
          AND as_stop.station.id = :destinationStationId
          AND bs.stopOrder < as_stop.stopOrder
          AND t.status = 'SCHEDULED'
          AND bs.scheduledDepartureUtc >= :departureFromUtc
          AND bs.scheduledDepartureUtc <= :departureToUtc
        ORDER BY bs.scheduledDepartureUtc ASC
    """)
    List<Object[]> findDirectTripsBetweenStations(
        @Param("originStationId") Long originStationId,
        @Param("destinationStationId") Long destinationStationId,
        @Param("departureFromUtc") Instant departureFromUtc,
        @Param("departureToUtc") Instant departureToUtc,
        Pageable pageable
    );

    @Query("""
        SELECT t, bs, as_stop
        FROM Trip t
        JOIN t.stops bs
        JOIN t.stops as_stop
        WHERE bs.station.id = :originStationId
          AND bs.stopOrder < as_stop.stopOrder
          AND t.status = 'SCHEDULED'
          AND bs.scheduledDepartureUtc >= :departureFromUtc
          AND bs.scheduledDepartureUtc <= :departureToUtc
        ORDER BY bs.scheduledDepartureUtc ASC
    """)
    List<Object[]> findCandidateFirstLegs(
        @Param("originStationId") Long originStationId,
        @Param("departureFromUtc") Instant departureFromUtc,
        @Param("departureToUtc") Instant departureToUtc
    );

    @Query("""
        SELECT t, bs, as_stop
        FROM Trip t
        JOIN t.stops bs
        JOIN t.stops as_stop
        WHERE bs.station.id = :transferStationId
          AND as_stop.station.id = :destinationStationId
          AND bs.stopOrder < as_stop.stopOrder
          AND t.status = 'SCHEDULED'
          AND bs.scheduledDepartureUtc >= :minDepartureUtc
          AND bs.scheduledDepartureUtc <= :maxDepartureUtc
          AND t.id <> :excludeTripId
        ORDER BY bs.scheduledDepartureUtc ASC
    """)
    List<Object[]> findCandidateSecondLegs(
        @Param("transferStationId") Long transferStationId,
        @Param("destinationStationId") Long destinationStationId,
        @Param("minDepartureUtc") Instant minDepartureUtc,
        @Param("maxDepartureUtc") Instant maxDepartureUtc,
        @Param("excludeTripId") Long excludeTripId
    );
}
