package com.busbooking.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripStopFareRepository extends JpaRepository<TripStopFare, Long> {
    List<TripStopFare> findByTripId(Long tripId);
    Optional<TripStopFare> findByTripIdAndBoardStopOrderAndAlightStopOrder(Long tripId, int boardStopOrder, int alightStopOrder);
    void deleteByTripId(Long tripId);
}
