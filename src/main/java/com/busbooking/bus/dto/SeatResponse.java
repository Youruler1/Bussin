package com.busbooking.bus.dto;

public record SeatResponse(
    Long id,
    Long busId,
    String seatNumber,
    int rowNumber,
    int columnNumber,
    String seatType,
    String deck,
    boolean isActive
) {}
