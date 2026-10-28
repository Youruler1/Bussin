package com.busbooking;

import com.busbooking.auth.UserRepository;
import com.busbooking.booking.BookingLegRepository;
import com.busbooking.booking.BookingRepository;
import com.busbooking.booking.SeatAllocationRepository;
import com.busbooking.bus.BusRepository;
import com.busbooking.bus.SeatRepository;
import com.busbooking.category.CategoryRepository;
import com.busbooking.route.RouteRepository;
import com.busbooking.route.RouteStopRepository;
import com.busbooking.station.StationRepository;
import com.busbooking.trip.TripRepository;
import com.busbooking.trip.TripSeatRepository;
import com.busbooking.trip.TripStopFareRepository;
import com.busbooking.trip.TripStopRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestDatabaseCleaner {

    private final SeatAllocationRepository seatAllocationRepository;
    private final BookingLegRepository bookingLegRepository;
    private final BookingRepository bookingRepository;
    private final TripSeatRepository tripSeatRepository;
    private final TripStopFareRepository tripStopFareRepository;
    private final TripStopRepository tripStopRepository;
    private final TripRepository tripRepository;
    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;
    private final SeatRepository seatRepository;
    private final BusRepository busRepository;
    private final StationRepository stationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TestDatabaseCleaner(SeatAllocationRepository seatAllocationRepository,
                               BookingLegRepository bookingLegRepository,
                               BookingRepository bookingRepository,
                               TripSeatRepository tripSeatRepository,
                               TripStopFareRepository tripStopFareRepository,
                               TripStopRepository tripStopRepository,
                               TripRepository tripRepository,
                               RouteStopRepository routeStopRepository,
                               RouteRepository routeRepository,
                               SeatRepository seatRepository,
                               BusRepository busRepository,
                               StationRepository stationRepository,
                               CategoryRepository categoryRepository,
                               UserRepository userRepository) {
        this.seatAllocationRepository = seatAllocationRepository;
        this.bookingLegRepository = bookingLegRepository;
        this.bookingRepository = bookingRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.tripStopFareRepository = tripStopFareRepository;
        this.tripStopRepository = tripStopRepository;
        this.tripRepository = tripRepository;
        this.routeStopRepository = routeStopRepository;
        this.routeRepository = routeRepository;
        this.seatRepository = seatRepository;
        this.busRepository = busRepository;
        this.stationRepository = stationRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void clearAll() {
        seatAllocationRepository.deleteAllInBatch();
        bookingLegRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        tripSeatRepository.deleteAllInBatch();
        tripStopFareRepository.deleteAllInBatch();
        tripStopRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        routeStopRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        busRepository.deleteAllInBatch();
        stationRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }
}
