package com.busbooking.trip.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TripStopResponse(
    Long id,
    Long stationId,
    String stationCode,
    String stationName,
    String city,
    int stopOrder,
    Instant scheduledArrivalUtc,
    Instant scheduledDepartureUtc,
    BigDecimal cumulativeDistanceKm
) {}
