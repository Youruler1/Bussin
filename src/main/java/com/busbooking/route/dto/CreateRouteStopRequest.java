package com.busbooking.route.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateRouteStopRequest(
    @NotNull(message = "Station ID is required")
    Long stationId,

    @NotNull(message = "Stop order is required")
    @Min(value = 0, message = "Stop order cannot be negative")
    int stopOrder,

    @DecimalMin(value = "0.0", message = "Distance cannot be negative")
    BigDecimal distanceFromPrevKm,

    @Min(value = 0, message = "Duration cannot be negative")
    int typicalDurationMinutes
) {}
