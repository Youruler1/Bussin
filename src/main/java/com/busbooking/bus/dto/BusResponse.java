package com.busbooking.bus.dto;

import java.time.Instant;

public record BusResponse(
    Long id,
    String registrationNumber,
    String brand,
    String model,
    Long categoryId,
    String categoryCode,
    String categoryName,
    int totalCapacity,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
