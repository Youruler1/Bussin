package com.busbooking.bus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBusRequest(
    @NotBlank(message = "Registration number is required")
    @Size(max = 50, message = "Registration number cannot exceed 50 characters")
    String registrationNumber,

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    String brand,

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    String model,

    @NotNull(message = "Category ID is required")
    Long categoryId,

    @Min(value = 1, message = "Total capacity must be at least 1")
    @Max(value = 80, message = "Total capacity cannot exceed 80")
    int totalCapacity
) {}
