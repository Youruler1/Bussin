package com.busbooking.booking;

import com.busbooking.auth.User;
import com.busbooking.auth.UserRepository;
import com.busbooking.booking.dto.BookingResponse;
import com.busbooking.booking.dto.BookingSummaryResponse;
import com.busbooking.booking.dto.CreateBookingRequest;
import com.busbooking.common.exception.CustomerNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {

        User user = getUser(userDetails);
        BookingResponse response = bookingService.createBooking(user.getId(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = getUser(userDetails);
        boolean isAdmin = isAdmin(userDetails);
        BookingResponse response = bookingService.getBookingById(id, user.getId(), isAdmin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        User user = getUser(userDetails);
        boolean isAdmin = isAdmin(userDetails);
        BookingResponse response = bookingService.cancelBooking(id, user.getId(), isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/me/bookings", "/bookings/my"})
    public ResponseEntity<List<BookingSummaryResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        List<BookingSummaryResponse> response = bookingService.getCustomerBookings(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingSummaryResponse>> getAllBookings() {
        List<BookingSummaryResponse> response = bookingService.getAllBookings();
        return ResponseEntity.ok(response);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomerNotFoundException("User not found with email: " + userDetails.getUsername()));
    }

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
