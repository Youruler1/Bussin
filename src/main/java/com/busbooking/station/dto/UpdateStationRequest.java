package com.busbooking.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStationRequest(
    @NotBlank(message = "Station name is required")
    @Size(max = 150, message = "Station name cannot exceed 150 characters")
    String name,

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    String city,

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    String state,

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    String timezone,

    Boolean isActive
) {}
