package com.busbooking.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PassengerLegBooking(
    @NotBlank(message = "Passenger name is required")
    @Size(max = 150, message = "Passenger name cannot exceed 150 characters")
    String passengerName,

    @Min(value = 0, message = "Passenger age must be at least 0")
    @Max(value = 125, message = "Passenger age cannot exceed 125")
    int passengerAge,

    @NotBlank(message = "Passenger gender is required")
    @Size(max = 20, message = "Passenger gender cannot exceed 20 characters")
    String passengerGender,

    @NotEmpty(message = "Leg allocations cannot be empty")
    List<@Valid LegAllocationRequest> legAllocations
) {}
