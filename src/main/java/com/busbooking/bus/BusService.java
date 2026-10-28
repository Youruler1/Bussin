package com.busbooking.bus;

import com.busbooking.bus.dto.*;
import com.busbooking.category.BusCategory;
import com.busbooking.category.CategoryRepository;
import com.busbooking.common.exception.BusCategoryNotFoundException;
import com.busbooking.common.exception.BusNotFoundException;
import com.busbooking.common.exception.DuplicateResourceException;
import com.busbooking.common.exception.ResourceConflictException;
import com.busbooking.trip.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BusService {

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final CategoryRepository categoryRepository;
    private final TripRepository tripRepository;

    public BusService(BusRepository busRepository,
                      SeatRepository seatRepository,
                      CategoryRepository categoryRepository,
                      TripRepository tripRepository) {
        this.busRepository = busRepository;
        this.seatRepository = seatRepository;
        this.categoryRepository = categoryRepository;
        this.tripRepository = tripRepository;
    }

    public List<BusResponse> getAllActiveBuses() {
        return busRepository.findAll().stream()
                .filter(Bus::isActive)
                .map(this::toResponse)
                .toList();
    }

    public List<BusResponse> getAllBuses() {
        return busRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public BusDetailResponse getBusById(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new BusNotFoundException("Bus not found with ID: " + id));
        List<SeatResponse> seats = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(id).stream()
                .map(this::toSeatResponse)
                .toList();
        return toDetailResponse(bus, seats);
    }

    @Transactional
    public BusDetailResponse createBus(CreateBusRequest request) {
        String regNumber = request.registrationNumber().trim().toUpperCase();
        if (busRepository.existsByRegistrationNumber(regNumber)) {
            throw new DuplicateResourceException("Bus with registration number '" + regNumber + "' already exists");
        }

        BusCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusCategoryNotFoundException("Bus category not found with ID: " + request.categoryId()));

        if (request.totalCapacity() < 1 || request.totalCapacity() > 80) {
            throw new IllegalArgumentException("Total capacity must be between 1 and 80");
        }

        Bus bus = new Bus(
                regNumber,
                request.brand().trim(),
                request.model().trim(),
                category,
                request.totalCapacity()
        );
        bus = busRepository.save(bus);

        List<Seat> generatedSeats = generateSeats(bus, category, request.totalCapacity());
        seatRepository.saveAll(generatedSeats);

        List<SeatResponse> seatResponses = generatedSeats.stream()
                .map(this::toSeatResponse)
                .toList();

        return toDetailResponse(bus, seatResponses);
    }

    @Transactional
    public BusResponse updateBus(Long id, UpdateBusRequest request) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new BusNotFoundException("Bus not found with ID: " + id));

        BusCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusCategoryNotFoundException("Bus category not found with ID: " + request.categoryId()));

        bus.setBrand(request.brand().trim());
        bus.setModel(request.model().trim());
        bus.setCategory(category);
        if (request.isActive() != null) {
            bus.setActive(request.isActive());
        }

        bus = busRepository.save(bus);
        return toResponse(bus);
    }

    @Transactional(noRollbackFor = ResourceConflictException.class)
    public void deleteBus(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new BusNotFoundException("Bus not found with ID: " + id));

        if (tripRepository.existsByBusId(id)) {
            bus.setActive(false);
            busRepository.save(bus);
            throw new ResourceConflictException("Cannot delete bus referenced by scheduled trips. Bus has been deactivated instead.");
        }

        seatRepository.deleteByBusId(id);
        busRepository.delete(bus);
    }

    private List<Seat> generateSeats(Bus bus, BusCategory category, int capacity) {
        List<Seat> seats = new ArrayList<>();
        boolean isSleeper = (category.getCode() != null && category.getCode().toUpperCase().contains("SLEEPER"))
                || (category.getName() != null && category.getName().toUpperCase().contains("SLEEPER"));

        int columnsPerRow = isSleeper ? 2 : 4;
        String seatType = isSleeper ? "SLEEPER" : "SEATER";

        for (int i = 0; i < capacity; i++) {
            int rowNumber = (i / columnsPerRow) + 1;
            int colIndex = i % columnsPerRow;
            char colChar = (char) ('A' + colIndex);
            String seatNumber = (isSleeper ? "S" : "") + rowNumber + colChar;

            Seat seat = new Seat(bus, seatNumber, rowNumber, colIndex + 1, seatType, "LOWER");
            seats.add(seat);
        }
        return seats;
    }

    public BusResponse toResponse(Bus bus) {
        return new BusResponse(
                bus.getId(),
                bus.getRegistrationNumber(),
                bus.getBrand(),
                bus.getModel(),
                bus.getCategory().getId(),
                bus.getCategory().getCode(),
                bus.getCategory().getName(),
                bus.getTotalCapacity(),
                bus.isActive(),
                bus.getCreatedAt(),
                bus.getUpdatedAt()
        );
    }

    public SeatResponse toSeatResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getBus().getId(),
                seat.getSeatNumber(),
                seat.getRowNumber(),
                seat.getColumnNumber(),
                seat.getSeatType(),
                seat.getDeck(),
                seat.isActive()
        );
    }

    public BusDetailResponse toDetailResponse(Bus bus, List<SeatResponse> seats) {
        return new BusDetailResponse(
                bus.getId(),
                bus.getRegistrationNumber(),
                bus.getBrand(),
                bus.getModel(),
                bus.getCategory().getId(),
                bus.getCategory().getCode(),
                bus.getCategory().getName(),
                bus.getTotalCapacity(),
                bus.isActive(),
                bus.getCreatedAt(),
                bus.getUpdatedAt(),
                seats
        );
    }
}
