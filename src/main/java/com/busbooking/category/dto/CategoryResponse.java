package com.busbooking.category.dto;

import com.busbooking.category.BusCategory;

import java.time.Instant;

public record CategoryResponse(
    Long id,
    String code,
    String name,
    String description,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public static CategoryResponse fromEntity(BusCategory category) {
        return new CategoryResponse(
            category.getId(),
            category.getCode(),
            category.getName(),
            category.getDescription(),
            category.isActive(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
