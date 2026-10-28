package com.busbooking.common.exception;

public class RouteNotFoundException extends ResourceNotFoundException {
    public RouteNotFoundException(String message) {
        super(message);
    }
}
