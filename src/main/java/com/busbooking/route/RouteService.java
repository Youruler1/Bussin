package com.busbooking.route;

import com.busbooking.common.exception.DuplicateResourceException;
import com.busbooking.common.exception.ResourceConflictException;
import com.busbooking.common.exception.RouteNotFoundException;
import com.busbooking.common.exception.StationNotFoundException;
import com.busbooking.route.dto.*;
import com.busbooking.station.Station;
import com.busbooking.station.StationRepository;
import com.busbooking.station.StationService;
import com.busbooking.trip.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StationRepository stationRepository;
    private final StationService stationService;
    private final TripRepository tripRepository;

    public RouteService(RouteRepository routeRepository,
                        RouteStopRepository routeStopRepository,
                        StationRepository stationRepository,
                        StationService stationService,
                        TripRepository tripRepository) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.stationRepository = stationRepository;
        this.stationService = stationService;
        this.tripRepository = tripRepository;
    }

    public List<RouteResponse> getAllActiveRoutes() {
        return routeRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public RouteResponse getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + id));
        return toResponse(route);
    }

    @Transactional
    public RouteResponse createRoute(CreateRouteRequest request) {
        String routeCode = request.routeCode().trim().toUpperCase();

        if (request.originStationId().equals(request.destinationStationId())) {
            throw new IllegalArgumentException("Origin and destination stations must be distinct");
        }

        if (routeRepository.existsByRouteCode(routeCode)) {
            throw new DuplicateResourceException("Route with code '" + routeCode + "' already exists");
        }

        Station origin = stationRepository.findById(request.originStationId())
                .orElseThrow(() -> new StationNotFoundException("Origin station not found with ID: " + request.originStationId()));

        Station destination = stationRepository.findById(request.destinationStationId())
                .orElseThrow(() -> new StationNotFoundException("Destination station not found with ID: " + request.destinationStationId()));

        if (request.stops() == null || request.stops().size() < 2) {
            throw new IllegalArgumentException("Route must contain at least 2 stops");
        }

        List<CreateRouteStopRequest> sortedStops = request.stops().stream()
                .sorted(Comparator.comparingInt(CreateRouteStopRequest::stopOrder))
                .toList();

        for (int i = 0; i < sortedStops.size(); i++) {
            if (sortedStops.get(i).stopOrder() != i) {
                throw new IllegalArgumentException("Route stops must have sequential orders starting from 0 without gaps or duplicates");
            }
        }

        Set<Long> visitedStations = new HashSet<>();
        for (CreateRouteStopRequest stop : sortedStops) {
            if (!visitedStations.add(stop.stationId())) {
                throw new IllegalArgumentException("Repeated station ID " + stop.stationId() + " in route stops is not permitted");
            }
        }

        if (!sortedStops.get(0).stationId().equals(request.originStationId())) {
            throw new IllegalArgumentException("First route stop (order 0) must be the origin station");
        }

        if (!sortedStops.get(sortedStops.size() - 1).stationId().equals(request.destinationStationId())) {
            throw new IllegalArgumentException("Last route stop must be the destination station");
        }

        Map<Long, Station> stationMap = new HashMap<>();
        stationMap.put(origin.getId(), origin);
        stationMap.put(destination.getId(), destination);

        for (CreateRouteStopRequest stopReq : sortedStops) {
            if (!stationMap.containsKey(stopReq.stationId())) {
                Station st = stationRepository.findById(stopReq.stationId())
                        .orElseThrow(() -> new StationNotFoundException("Station not found with ID: " + stopReq.stationId()));
                stationMap.put(st.getId(), st);
            }
        }

        BigDecimal calculatedDistance = BigDecimal.ZERO;
        for (CreateRouteStopRequest stopReq : sortedStops) {
            if (stopReq.distanceFromPrevKm() != null) {
                calculatedDistance = calculatedDistance.add(stopReq.distanceFromPrevKm());
            }
        }

        BigDecimal totalDistance = (request.totalDistanceKm() != null && request.totalDistanceKm().compareTo(BigDecimal.ZERO) > 0)
                ? request.totalDistanceKm()
                : calculatedDistance;

        Route route = new Route(routeCode, request.name().trim(), origin, destination, totalDistance);
        route = routeRepository.save(route);

        for (CreateRouteStopRequest stopReq : sortedStops) {
            Station st = stationMap.get(stopReq.stationId());
            RouteStop stop = new RouteStop(
                    route,
                    st,
                    stopReq.stopOrder(),
                    stopReq.distanceFromPrevKm(),
                    stopReq.typicalDurationMinutes()
            );
            route.addStop(stop);
        }

        route = routeRepository.save(route);
        return toResponse(route);
    }

    @Transactional
    public RouteResponse updateRoute(Long id, UpdateRouteRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + id));

        route.setName(request.name().trim());
        if (request.isActive() != null) {
            route.setActive(request.isActive());
        }

        route = routeRepository.save(route);
        return toResponse(route);
    }

    @Transactional(noRollbackFor = ResourceConflictException.class)
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + id));

        if (tripRepository.existsByRouteId(id)) {
            route.setActive(false);
            routeRepository.save(route);
            throw new ResourceConflictException("Cannot delete route referenced by scheduled trips. Route has been deactivated instead.");
        }

        routeRepository.delete(route);
    }

    public RouteResponse toResponse(Route route) {
        List<RouteStopResponse> stopResponses = (route.getStops() != null)
                ? route.getStops().stream().map(this::toStopResponse).toList()
                : Collections.emptyList();

        return new RouteResponse(
                route.getId(),
                route.getRouteCode(),
                route.getName(),
                stationService.toResponse(route.getOriginStation()),
                stationService.toResponse(route.getDestinationStation()),
                route.getTotalDistanceKm(),
                route.isActive(),
                route.getCreatedAt(),
                stopResponses
        );
    }

    public RouteStopResponse toStopResponse(RouteStop stop) {
        return new RouteStopResponse(
                stop.getId(),
                stop.getStation().getId(),
                stop.getStation().getCode(),
                stop.getStation().getName(),
                stop.getStation().getCity(),
                stop.getStopOrder(),
                stop.getDistanceFromPrevKm(),
                stop.getTypicalDurationMinutes()
        );
    }
}
