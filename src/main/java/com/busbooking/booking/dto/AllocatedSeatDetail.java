package com.busbooking.booking.dto;

import java.math.BigDecimal;

public record AllocatedSeatDetail(
        String seatNumber,
        String passengerName,
        int passengerAge,
        String passengerGender,
        BigDecimal fareInr
) {}
