package com.busbooking.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://busbooking.local/errors/not-found"));
        problem.setProperty("code", "RESOURCE_NOT_FOUND");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Resource");
        problem.setType(URI.create("https://busbooking.local/errors/duplicate-resource"));
        problem.setProperty("code", "DUPLICATE_RESOURCE");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ProblemDetail handleResourceConflict(ResourceConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create("https://busbooking.local/errors/conflict"));
        problem.setProperty("code", "RESOURCE_CONFLICT");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request parameters");
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://busbooking.local/errors/validation-failed"));
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now().toString());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Argument");
        problem.setType(URI.create("https://busbooking.local/errors/invalid-argument"));
        problem.setProperty("code", "INVALID_ARGUMENT");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("https://busbooking.local/errors/unauthorized"));
        problem.setProperty("code", "INVALID_CREDENTIALS");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://busbooking.local/errors/forbidden"));
        problem.setProperty("code", "ACCESS_DENIED");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body: " + ex.getMessage());
        problem.setTitle("Malformed Request");
        problem.setType(URI.create("https://busbooking.local/errors/malformed-request"));
        problem.setProperty("code", "MALFORMED_REQUEST");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid parameter: " + ex.getName());
        problem.setTitle("Type Mismatch");
        problem.setType(URI.create("https://busbooking.local/errors/type-mismatch"));
        problem.setProperty("code", "TYPE_MISMATCH");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName());
        problem.setTitle("Missing Parameter");
        problem.setType(URI.create("https://busbooking.local/errors/missing-parameter"));
        problem.setProperty("code", "MISSING_PARAMETER");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unhandled exception caught by GlobalExceptionHandler: ", ex);
        System.err.println("CRITICAL UNHANDLED EXCEPTION: " + ex.getClass().getName() + ": " + ex.getMessage());
        ex.printStackTrace(System.err);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://busbooking.local/errors/internal-error"));
        problem.setProperty("code", "INTERNAL_SERVER_ERROR");
        problem.setProperty("exceptionClass", ex.getClass().getName());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
