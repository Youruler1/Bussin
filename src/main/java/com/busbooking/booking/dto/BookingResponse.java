package com.busbooking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
    Long id,
    String bookingReference,
    String status,
    BigDecimal totalAmountInr,
    String contactEmail,
    String contactPhone,
    Instant bookedAt,
    Instant cancelledAt,
    List<BookingLegDetail> legs
) {}
