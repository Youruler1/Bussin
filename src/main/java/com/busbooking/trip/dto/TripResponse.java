package com.busbooking.trip.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TripResponse(
    Long id,
    String tripNumber,
    Long routeId,
    String routeCode,
    String routeName,
    Long busId,
    String busRegistration,
    String busCategoryName,
    int totalCapacity,
    LocalDate serviceDate,
    String status,
    Instant scheduledDepartureUtc,
    Instant scheduledArrivalUtc,
    List<TripStopResponse> stops,
    List<TripStopFareResponse> fares,
    int totalSeatsAllocatedMutex,
    Instant createdAt,
    Instant updatedAt
) {}
