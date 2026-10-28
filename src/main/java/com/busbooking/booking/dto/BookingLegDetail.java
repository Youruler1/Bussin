package com.busbooking.booking.dto;

import java.time.Instant;
import java.util.List;

public record BookingLegDetail(
    Long tripId,
    String tripNumber,
    String boardStationName,
    String alightStationName,
    Instant departureTime,
    Instant arrivalTime,
    List<AllocatedSeatDetail> allocatedSeats
) {}
