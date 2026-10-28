package com.busbooking.journey.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record JourneyLegSummary(
    Long tripId,
    String tripNumber,
    String busCategory,
    String busRegistration,
    Long originStationId,
    String originStationName,
    Long destinationStationId,
    String destinationStationName,
    int boardStopOrder,
    int alightStopOrder,
    Instant departureTime,
    Instant arrivalTime,
    int availableSeats,
    BigDecimal segmentFareInr
) {}
