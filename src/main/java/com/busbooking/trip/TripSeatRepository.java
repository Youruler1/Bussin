package com.busbooking.trip;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripSeatRepository extends JpaRepository<TripSeat, Long> {
    List<TripSeat> findByTripId(Long tripId);
    long countByTripId(Long tripId);
    Optional<TripSeat> findByTripIdAndSeatId(Long tripId, Long seatId);
    void deleteByTripId(Long tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TripSeat ts WHERE ts.trip.id = :tripId AND ts.seat.id IN :seatIds ORDER BY ts.trip.id ASC, ts.seat.id ASC")
    List<TripSeat> findAndLockByTripIdAndSeatIdsOrdered(@Param("tripId") Long tripId, @Param("seatIds") List<Long> seatIds);
}
