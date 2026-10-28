package com.busbooking.station;

import com.busbooking.station.dto.CreateStationRequest;
import com.busbooking.station.dto.StationResponse;
import com.busbooking.station.dto.UpdateStationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/api/v1/stations")
    public ResponseEntity<List<StationResponse>> getActiveStations() {
        return ResponseEntity.ok(stationService.getAllActiveStations());
    }

    @GetMapping("/api/v1/stations/{id}")
    public ResponseEntity<StationResponse> getStationByIdPublic(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    @GetMapping("/api/v1/admin/stations")
    public ResponseEntity<List<StationResponse>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/api/v1/admin/stations/{id}")
    public ResponseEntity<StationResponse> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    @PostMapping("/api/v1/admin/stations")
    public ResponseEntity<StationResponse> createStation(@Valid @RequestBody CreateStationRequest request) {
        StationResponse response = stationService.createStation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/admin/stations/{id}")
    public ResponseEntity<StationResponse> updateStation(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateStationRequest request) {
        StationResponse response = stationService.updateStation(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/admin/stations/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}
