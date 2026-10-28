package com.busbooking.common.exception;

public class BusNotFoundException extends ResourceNotFoundException {
    public BusNotFoundException(String message) {
        super(message);
    }

    public BusNotFoundException(Long id) {
        super("Bus not found with id: " + id);
    }
}
