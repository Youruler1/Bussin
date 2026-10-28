package com.busbooking.common.exception;

public class BusCategoryNotFoundException extends ResourceNotFoundException {
    public BusCategoryNotFoundException(String message) {
        super(message);
    }

    public BusCategoryNotFoundException(Long id) {
        super("Bus category not found with id: " + id);
    }
}
