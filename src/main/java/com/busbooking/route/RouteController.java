package com.busbooking.route;

import com.busbooking.route.dto.CreateRouteRequest;
import com.busbooking.route.dto.RouteResponse;
import com.busbooking.route.dto.UpdateRouteRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/api/v1/routes")
    public ResponseEntity<List<RouteResponse>> getActiveRoutes() {
        return ResponseEntity.ok(routeService.getAllActiveRoutes());
    }

    @GetMapping("/api/v1/routes/{id}")
    public ResponseEntity<RouteResponse> getRouteByIdPublic(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @GetMapping("/api/v1/admin/routes")
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @GetMapping("/api/v1/admin/routes/{id}")
    public ResponseEntity<RouteResponse> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @PostMapping("/api/v1/admin/routes")
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        RouteResponse response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/admin/routes/{id}")
    public ResponseEntity<RouteResponse> updateRoute(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateRouteRequest request) {
        RouteResponse response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/v1/admin/routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
