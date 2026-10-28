package com.busbooking.bus.dto;

import java.time.Instant;
import java.util.List;

public record BusDetailResponse(
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
    Instant updatedAt,
    List<SeatResponse> seats
) {}
