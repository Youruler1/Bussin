package com.busbooking.journey.dto;

import java.util.List;

public record TripSeatMapResponse(
    Long tripId,
    String tripNumber,
    int totalCapacity,
    int boardStopOrder,
    int alightStopOrder,
    int availableSeatsCount,
    List<SeatItem> seats
) {}
