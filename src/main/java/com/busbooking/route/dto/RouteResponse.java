package com.busbooking.route.dto;

import com.busbooking.station.dto.StationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RouteResponse(
    Long id,
    String routeCode,
    String name,
    StationResponse originStation,
    StationResponse destinationStation,
    BigDecimal totalDistanceKm,
    boolean isActive,
    Instant createdAt,
    List<RouteStopResponse> stops
) {}
