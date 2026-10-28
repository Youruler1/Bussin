package com.busbooking.trip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CreateTripRequest(
    @NotBlank(message = "Trip number is required")
    @Size(max = 50, message = "Trip number cannot exceed 50 characters")
    String tripNumber,

    @NotNull(message = "Route ID is required")
    Long routeId,

    @NotNull(message = "Bus ID is required")
    Long busId,

    @NotNull(message = "Service date is required")
    LocalDate serviceDate,

    @NotNull(message = "Scheduled departure time is required")
    Instant scheduledDepartureUtc,

    @Min(value = 0, message = "Dwell minutes cannot be negative")
    Integer dwellMinutes,

    @DecimalMin(value = "0.0", message = "Base fare per km cannot be negative")
    BigDecimal baseFarePerKm,

    List<@Valid CreateTripStopFareRequest> customFares
) {}
