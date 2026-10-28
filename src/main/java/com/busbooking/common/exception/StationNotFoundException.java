package com.busbooking.common.exception;

public class StationNotFoundException extends ResourceNotFoundException {
    public StationNotFoundException(String message) {
        super(message);
    }
}
