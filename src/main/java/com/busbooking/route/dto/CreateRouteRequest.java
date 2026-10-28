package com.busbooking.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateRouteRequest(
    @NotBlank(message = "Route code is required")
    @Size(max = 50, message = "Route code cannot exceed 50 characters")
    String routeCode,

    @NotBlank(message = "Route name is required")
    @Size(max = 200, message = "Route name cannot exceed 200 characters")
    String name,

    @NotNull(message = "Origin station ID is required")
    Long originStationId,

    @NotNull(message = "Destination station ID is required")
    Long destinationStationId,

    @DecimalMin(value = "0.0", message = "Total distance cannot be negative")
    BigDecimal totalDistanceKm,

    @NotEmpty(message = "Route must contain at least 2 stops")
    @Size(min = 2, message = "Route must contain at least 2 stops")
    List<@Valid CreateRouteStopRequest> stops
) {}
