package com.busbooking.bus;

import com.busbooking.bus.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BusController {

    private final BusService busService;

    public BusController(BusService busService) {
        this.busService = busService;
    }

    @GetMapping("/api/v1/buses")
    public ResponseEntity<List<BusResponse>> getActiveBuses() {
        return ResponseEntity.ok(busService.getAllActiveBuses());
    }

    @GetMapping("/api/v1/buses/{id}")
    public ResponseEntity<BusDetailResponse> getBusByIdPublic(@PathVariable Long id) {
        return ResponseEntity.ok(busService.getBusById(id));
    }

    @GetMapping("/api/v1/admin/buses")
    public ResponseEntity<List<BusResponse>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }

    @GetMapping("/api/v1/admin/buses/{id}")
    public ResponseEntity<BusDetailResponse> getBusById(@PathVariable Long id) {
        return ResponseEntity.ok(busService.getBusById(id));
    }

    @PostMapping("/api/v1/admin/buses")
    public ResponseEntity<BusDetailResponse> createBus(@Valid @RequestBody CreateBusRequest request) {
        BusDetailResponse response = busService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/admin/buses/{id}")
    public ResponseEntity<BusResponse> updateBus(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateBusRequest request) {
        BusResponse response = busService.updateBus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/admin/buses/{id}")
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        busService.deleteBus(id);
        return ResponseEntity.noContent().build();
    }
}
