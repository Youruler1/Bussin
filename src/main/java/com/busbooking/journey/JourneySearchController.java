package com.busbooking.journey;

import com.busbooking.journey.dto.JourneySearchResult;
import com.busbooking.journey.dto.TripSeatMapResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
public class JourneySearchController {

    private final JourneySearchService journeySearchService;

    public JourneySearchController(JourneySearchService journeySearchService) {
        this.journeySearchService = journeySearchService;
    }

    @GetMapping("/api/v1/journeys/search")
    public ResponseEntity<JourneySearchResult> searchJourneys(
            @RequestParam Long originStationId,
            @RequestParam Long destinationStationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departureFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departureTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        JourneySearchResult result = journeySearchService.searchJourneys(
                originStationId,
                destinationStationId,
                travelDate,
                departureFrom,
                departureTo,
                page,
                size
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/v1/trips/{id}/seats")
    public ResponseEntity<TripSeatMapResponse> getTripSeats(
            @PathVariable Long id,
            @RequestParam int boardIndex,
            @RequestParam int alightIndex) {

        TripSeatMapResponse response = journeySearchService.getTripSeats(id, boardIndex, alightIndex);
        return ResponseEntity.ok(response);
    }
}
