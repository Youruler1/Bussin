package com.busbooking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingSummaryResponse(
        Long id,
        String bookingReference,
        String status,
        BigDecimal totalAmountInr,
        Instant bookedAt,
        Instant cancelledAt,
        String routeSummary,
        int legCount,
        int passengerCount
) {}
