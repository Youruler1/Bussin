package com.busbooking.trip.dto;

import java.math.BigDecimal;

public record TripStopFareResponse(
    Long id,
    int boardStopOrder,
    int alightStopOrder,
    BigDecimal baseFareInr
) {}
