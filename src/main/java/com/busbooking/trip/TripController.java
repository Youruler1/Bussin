package com.busbooking.trip;

import com.busbooking.trip.dto.CreateTripRequest;
import com.busbooking.trip.dto.TripResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/api/v1/trips/{id}")
    public ResponseEntity<TripResponse> getTripByIdPublic(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @GetMapping("/api/v1/admin/trips")
    public ResponseEntity<List<TripResponse>> getAllTrips() {
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    @GetMapping("/api/v1/admin/trips/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @PostMapping("/api/v1/admin/trips")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/v1/admin/trips/{id}/status")
    public ResponseEntity<TripResponse> updateTripStatus(@PathVariable Long id,
                                                         @RequestBody Map<String, String> statusBody) {
        String newStatus = statusBody.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        TripResponse response = tripService.updateTripStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/admin/trips/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}
