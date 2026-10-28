package com.busbooking.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripStopRepository extends JpaRepository<TripStop, Long> {
    List<TripStop> findByTripIdOrderByStopOrderAsc(Long tripId);
    Optional<TripStop> findByTripIdAndStopOrder(Long tripId, int stopOrder);
    Optional<TripStop> findByTripIdAndStationId(Long tripId, Long stationId);
    boolean existsByStationId(Long stationId);
    void deleteByTripId(Long tripId);
}
