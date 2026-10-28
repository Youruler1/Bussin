package com.busbooking.station.dto;

public record StationResponse(
    Long id,
    String code,
    String name,
    String city,
    String state,
    String timezone,
    boolean isActive
) {}
