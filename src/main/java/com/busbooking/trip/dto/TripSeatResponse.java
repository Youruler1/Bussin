package com.busbooking.trip.dto;

public record TripSeatResponse(
    Long id,
    Long seatId,
    String seatNumber,
    int rowNumber,
    int columnNumber,
    String seatType,
    String deck,
    boolean isActive
) {}
