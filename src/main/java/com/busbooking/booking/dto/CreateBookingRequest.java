package com.busbooking.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateBookingRequest(
    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be a valid email address")
    String contactEmail,

    @NotBlank(message = "Contact phone is required")
    @Size(max = 30, message = "Contact phone cannot exceed 30 characters")
    String contactPhone,

    @NotEmpty(message = "Passenger bookings cannot be empty")
    List<@Valid PassengerLegBooking> passengerBookings
) {}
