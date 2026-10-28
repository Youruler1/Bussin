package com.busbooking.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRouteRequest(
    @NotBlank(message = "Route name is required")
    @Size(max = 200, message = "Route name cannot exceed 200 characters")
    String name,

    Boolean isActive
) {}
