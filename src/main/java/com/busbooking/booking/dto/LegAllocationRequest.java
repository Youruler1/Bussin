package com.busbooking.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LegAllocationRequest(
    @NotNull(message = "Trip ID is required")
    Long tripId,

    @NotNull(message = "Seat ID is required")
    Long seatId,

    @NotNull(message = "Board stop order is required")
    @Min(value = 0, message = "Board stop order cannot be negative")
    int boardStopOrder,

    @NotNull(message = "Alight stop order is required")
    @Min(value = 1, message = "Alight stop order must be at least 1")
    int alightStopOrder
) {}
