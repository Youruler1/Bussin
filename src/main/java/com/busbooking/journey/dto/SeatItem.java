package com.busbooking.journey.dto;

public record SeatItem(
    Long seatId,
    Long tripSeatId,
    String seatNumber,
    int rowNumber,
    int columnNumber,
    String seatType,
    String deck,
    boolean available
) {}
