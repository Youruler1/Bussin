package com.busbooking.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingLegRepository extends JpaRepository<BookingLeg, Long> {
    List<BookingLeg> findByBookingIdOrderByLegSequenceAsc(Long bookingId);
    List<BookingLeg> findByTripId(Long tripId);
}
