package com.busbooking.common.exception;

public class TripNotFoundException extends ResourceNotFoundException {
    public TripNotFoundException(String message) {
        super(message);
    }
}
