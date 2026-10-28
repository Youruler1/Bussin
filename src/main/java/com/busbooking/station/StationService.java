package com.busbooking.station;

import com.busbooking.common.exception.DuplicateResourceException;
import com.busbooking.common.exception.ResourceConflictException;
import com.busbooking.common.exception.StationNotFoundException;
import com.busbooking.route.RouteRepository;
import com.busbooking.route.RouteStopRepository;
import com.busbooking.station.dto.CreateStationRequest;
import com.busbooking.station.dto.StationResponse;
import com.busbooking.station.dto.UpdateStationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StationService {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    public StationService(StationRepository stationRepository,
                          RouteRepository routeRepository,
                          RouteStopRepository routeStopRepository) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
    }

    public List<StationResponse> getAllActiveStations() {
        return stationRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<StationResponse> getAllStations() {
        return stationRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public StationResponse getStationById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new StationNotFoundException("Station not found with ID: " + id));
        return toResponse(station);
    }

    @Transactional
    public StationResponse createStation(CreateStationRequest request) {
        String code = request.code().trim().toUpperCase();
        if (stationRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Station with code '" + code + "' already exists");
        }

        Station station = new Station(
                code,
                request.name().trim(),
                request.city().trim(),
                request.state().trim(),
                request.timezone()
        );
        station = stationRepository.save(station);
        return toResponse(station);
    }

    @Transactional
    public StationResponse updateStation(Long id, UpdateStationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new StationNotFoundException("Station not found with ID: " + id));

        station.setName(request.name().trim());
        station.setCity(request.city().trim());
        station.setState(request.state().trim());
        if (request.timezone() != null && !request.timezone().isBlank()) {
            station.setTimezone(request.timezone().trim());
        }
        if (request.isActive() != null) {
            station.setActive(request.isActive());
        }

        station = stationRepository.save(station);
        return toResponse(station);
    }

    @Transactional(noRollbackFor = ResourceConflictException.class)
    public void deleteStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new StationNotFoundException("Station not found with ID: " + id));

        boolean isReferenced = routeRepository.existsByStationIdAsTerminal(id)
                || routeStopRepository.existsByStationId(id);

        if (isReferenced) {
            station.setActive(false);
            stationRepository.save(station);
            throw new ResourceConflictException("Cannot delete station referenced by routes or stops. Station has been deactivated instead.");
        }

        stationRepository.delete(station);
    }

    public StationResponse toResponse(Station station) {
        return new StationResponse(
                station.getId(),
                station.getCode(),
                station.getName(),
                station.getCity(),
                station.getState(),
                station.getTimezone(),
                station.isActive()
        );
    }
}
