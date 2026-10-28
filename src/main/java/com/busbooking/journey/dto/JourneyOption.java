package com.busbooking.journey.dto;

import java.math.BigDecimal;
import java.util.List;

public record JourneyOption(
    String itineraryType,
    int totalDurationMinutes,
    BigDecimal totalFareInr,
    List<JourneyLegSummary> legs
) {}
