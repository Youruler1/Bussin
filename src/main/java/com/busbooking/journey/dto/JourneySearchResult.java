package com.busbooking.journey.dto;

import java.util.List;

public record JourneySearchResult(
    String journeyType,
    List<JourneyOption> options,
    int totalResults,
    int page,
    int size
) {}
