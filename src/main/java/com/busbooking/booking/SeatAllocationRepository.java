package com.busbooking.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {

    @Query("""
        SELECT DISTINCT sa.tripSeat.id FROM SeatAllocation sa
        WHERE sa.tripSeat.id IN :tripSeatIds
          AND sa.status IN ('CONFIRMED', 'HELD')
          AND (sa.boardStopOrder < :alightOrder AND :boardOrder < sa.alightStopOrder)
    """)
    Set<Long> findOccupiedTripSeatIds(@Param("tripSeatIds") Collection<Long> tripSeatIds,
                                      @Param("boardOrder") int boardOrder,
                                      @Param("alightOrder") int alightOrder);

    @Query("""
        SELECT COUNT(DISTINCT sa.tripSeat.id) FROM SeatAllocation sa
        WHERE sa.tripSeat.trip.id = :tripId
          AND sa.status IN ('CONFIRMED', 'HELD')
          AND (sa.boardStopOrder < :alightOrder AND :boardOrder < sa.alightStopOrder)
    """)
    long countOccupiedTripSeats(@Param("tripId") Long tripId,
                                @Param("boardOrder") int boardOrder,
                                @Param("alightOrder") int alightOrder);

    @Query("""
        SELECT sa FROM SeatAllocation sa
        WHERE sa.tripSeat.id = :tripSeatId
          AND sa.status IN ('CONFIRMED', 'HELD')
          AND (sa.boardStopOrder < :alightOrder AND :boardOrder < sa.alightStopOrder)
    """)
    List<SeatAllocation> findOverlappingAllocations(@Param("tripSeatId") Long tripSeatId,
                                                   @Param("boardOrder") int boardOrder,
                                                   @Param("alightOrder") int alightOrder);
}
