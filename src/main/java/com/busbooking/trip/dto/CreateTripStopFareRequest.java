package com.busbooking.trip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateTripStopFareRequest(
    @NotNull(message = "Board stop order is required")
    @Min(0)
    int boardStopOrder,

    @NotNull(message = "Alight stop order is required")
    @Min(1)
    int alightStopOrder,

    @NotNull(message = "Base fare is required")
    @DecimalMin(value = "0.0", message = "Base fare cannot be negative")
    BigDecimal baseFareInr
) {}
