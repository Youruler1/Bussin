package com.busbooking.route.dto;

import java.math.BigDecimal;

public record RouteStopResponse(
    Long id,
    Long stationId,
    String stationCode,
    String stationName,
    String city,
    int stopOrder,
    BigDecimal distanceFromPrevKm,
    int typicalDurationMinutes
) {}
