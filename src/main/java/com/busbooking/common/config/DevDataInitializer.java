package com.busbooking.common.config;

import com.busbooking.auth.Role;
import com.busbooking.auth.User;
import com.busbooking.auth.UserRepository;
import com.busbooking.bus.Bus;
import com.busbooking.bus.BusRepository;
import com.busbooking.bus.Seat;
import com.busbooking.bus.SeatRepository;
import com.busbooking.category.BusCategory;
import com.busbooking.category.CategoryRepository;
import com.busbooking.route.Route;
import com.busbooking.route.RouteRepository;
import com.busbooking.route.RouteStop;
import com.busbooking.station.Station;
import com.busbooking.station.StationRepository;
import com.busbooking.trip.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository,
                              CategoryRepository categoryRepository,
                              BusRepository busRepository,
                              SeatRepository seatRepository,
                              StationRepository stationRepository,
                              RouteRepository routeRepository,
                              TripRepository tripRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.busRepository = busRepository;
        this.seatRepository = seatRepository;
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            if (tripRepository.count() == 0) {
                seedTripsForExistingReferenceData();
            } else {
                log.info("Database already contains data; skipping automated demo seeding.");
            }
            return;
        }

        log.info("Seeding initial demo dataset for interactive testing...");

        // 1. Users
        String encodedPass = passwordEncoder.encode("Password@123");
        userRepository.save(new User("admin@busbooking.local", encodedPass, "System Administrator", "+919876543210", Role.ROLE_ADMIN));
        userRepository.save(new User("customer@demo.local", encodedPass, "Demo Customer", "+919123456789", Role.ROLE_CUSTOMER));

        // 2. Categories
        BusCategory acSleeper = categoryRepository.save(new BusCategory("AC_SLEEPER", "AC Sleeper Luxury", "Premium luxury sleeper berths with USB charging"));
        BusCategory acSeater = categoryRepository.save(new BusCategory("AC_SEATER", "AC Pushback Seater", "2x2 pushback air-conditioned coach"));
        BusCategory volvo = categoryRepository.save(new BusCategory("LUXURY_VOLVO", "Volvo Multi-Axle 9600", "Ultra-luxury multi-axle air suspension coach"));

        // 3. Buses & Seats
        Bus bus1 = busRepository.save(new Bus("DL-01-AB-1234", "Volvo", "9600 Multi-Axle", volvo, 20));
        List<Seat> bus1Seats = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            bus1Seats.add(new Seat(bus1, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seatRepository.saveAll(bus1Seats);

        Bus bus2 = busRepository.save(new Bus("UP-32-CD-5678", "Tata", "Starbus AC", acSeater, 20));
        List<Seat> bus2Seats = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            bus2Seats.add(new Seat(bus2, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seatRepository.saveAll(bus2Seats);

        // 4. Stations
        Station delhi = stationRepository.save(new Station("NDLS", "New Delhi Central ISBT", "New Delhi", "Delhi", "Asia/Kolkata"));
        Station agra = stationRepository.save(new Station("AGR", "Agra Cantt Bus Stand", "Agra", "Uttar Pradesh", "Asia/Kolkata"));
        Station lucknow = stationRepository.save(new Station("LKO", "Lucknow Alambagh Terminal", "Lucknow", "Uttar Pradesh", "Asia/Kolkata"));
        Station kanpur = stationRepository.save(new Station("KNP", "Kanpur Central Bus Station", "Kanpur", "Uttar Pradesh", "Asia/Kolkata"));
        Station jaipur = stationRepository.save(new Station("JPR", "Jaipur Sindhi Camp", "Jaipur", "Rajasthan", "Asia/Kolkata"));
        Station chandigarh = stationRepository.save(new Station("CDG", "Chandigarh Sector 43 ISBT", "Chandigarh", "Punjab", "Asia/Kolkata"));

        // 5. Routes
        // Route 1: Delhi -> Agra -> Lucknow (480 km)
        Route route1 = new Route("RT-DEL-LKO", "Delhi - Agra - Lucknow Express", delhi, lucknow, BigDecimal.valueOf(480.0));
        route1.addStop(new RouteStop(route1, delhi, 0, BigDecimal.ZERO, 0));
        route1.addStop(new RouteStop(route1, agra, 1, BigDecimal.valueOf(200.0), 210));
        route1.addStop(new RouteStop(route1, lucknow, 2, BigDecimal.valueOf(280.0), 270));
        route1 = routeRepository.save(route1);

        // Route 2: Lucknow -> Kanpur (85 km)
        Route route2 = new Route("RT-LKO-KNP", "Lucknow - Kanpur Shuttle", lucknow, kanpur, BigDecimal.valueOf(85.0));
        route2.addStop(new RouteStop(route2, lucknow, 0, BigDecimal.ZERO, 0));
        route2.addStop(new RouteStop(route2, kanpur, 1, BigDecimal.valueOf(85.0), 90));
        route2 = routeRepository.save(route2);

        // 6. Trips (Scheduled for 2026-11-01 and today)
        createScheduledTrip("TRIP-DEL-LKO-01", route1, bus1, LocalDate.of(2026, 11, 1),
                Instant.parse("2026-11-01T06:00:00Z"), Instant.parse("2026-11-01T14:00:00Z"),
                delhi, agra, lucknow, bus1Seats,
                BigDecimal.valueOf(400.00), BigDecimal.valueOf(560.00), BigDecimal.valueOf(950.00));

        createScheduledTrip("TRIP-DEL-LKO-02", route1, bus2, LocalDate.of(2026, 11, 1),
                Instant.parse("2026-11-01T14:30:00Z"), Instant.parse("2026-11-01T22:30:00Z"),
                delhi, agra, lucknow, bus2Seats,
                BigDecimal.valueOf(350.00), BigDecimal.valueOf(500.00), BigDecimal.valueOf(850.00));

        // Connecting trip: Lucknow -> Kanpur departing at 16:30 (2.5h after Trip 1 arrives at Lucknow)
        createConnectingTrip("TRIP-LKO-KNP-01", route2, bus1, LocalDate.of(2026, 11, 1),
                Instant.parse("2026-11-01T16:30:00Z"), Instant.parse("2026-11-01T18:00:00Z"),
                lucknow, kanpur, bus1Seats, BigDecimal.valueOf(180.00));

        log.info("Demo dataset seeded successfully! 2 Users, 3 Categories, 2 Buses, 6 Stations, 2 Routes, 3 Trips.");
    }

    private void seedTripsForExistingReferenceData() {
        var route1Opt = routeRepository.findByRouteCode("RT-DEL-LKO");
        var route2Opt = routeRepository.findByRouteCode("RT-LKO-KNP");
        var bus1Opt = busRepository.findByRegistrationNumber("DL-01-AB-1234");
        var bus2Opt = busRepository.findByRegistrationNumber("UP-32-CD-5678");
        var delhiOpt = stationRepository.findByCode("NDLS");
        var agraOpt = stationRepository.findByCode("AGR");
        var lucknowOpt = stationRepository.findByCode("LKO");
        var kanpurOpt = stationRepository.findByCode("KNP");

        if (route1Opt.isPresent() && route2Opt.isPresent()
                && bus1Opt.isPresent() && bus2Opt.isPresent()
                && delhiOpt.isPresent() && agraOpt.isPresent()
                && lucknowOpt.isPresent() && kanpurOpt.isPresent()) {
            log.info("Flyway reference data detected with 0 trips; seeding baseline demo trips for 2026-11-01...");
            Bus bus1 = bus1Opt.get();
            Bus bus2 = bus2Opt.get();
            List<Seat> bus1Seats = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId());
            List<Seat> bus2Seats = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus2.getId());

            createScheduledTrip("TRIP-DEL-LKO-01", route1Opt.get(), bus1, LocalDate.of(2026, 11, 1),
                    Instant.parse("2026-11-01T06:00:00Z"), Instant.parse("2026-11-01T14:00:00Z"),
                    delhiOpt.get(), agraOpt.get(), lucknowOpt.get(), bus1Seats,
                    BigDecimal.valueOf(400.00), BigDecimal.valueOf(560.00), BigDecimal.valueOf(950.00));

            createScheduledTrip("TRIP-DEL-LKO-02", route1Opt.get(), bus2, LocalDate.of(2026, 11, 1),
                    Instant.parse("2026-11-01T14:30:00Z"), Instant.parse("2026-11-01T22:30:00Z"),
                    delhiOpt.get(), agraOpt.get(), lucknowOpt.get(), bus2Seats,
                    BigDecimal.valueOf(350.00), BigDecimal.valueOf(500.00), BigDecimal.valueOf(850.00));

            createConnectingTrip("TRIP-LKO-KNP-01", route2Opt.get(), bus1, LocalDate.of(2026, 11, 1),
                    Instant.parse("2026-11-01T16:30:00Z"), Instant.parse("2026-11-01T18:00:00Z"),
                    lucknowOpt.get(), kanpurOpt.get(), bus1Seats, BigDecimal.valueOf(180.00));
            log.info("Demo trips seeded successfully on top of Flyway reference dataset!");
        }
    }

    private void createScheduledTrip(String tripNumber, Route route, Bus bus, LocalDate date,
                                     Instant dep, Instant arr,
                                     Station s0, Station s1, Station s2, List<Seat> seats,
                                     BigDecimal fare01, BigDecimal fare12, BigDecimal fare02) {
        Trip trip = new Trip(tripNumber, route, bus, date, "SCHEDULED", dep, arr);
        trip.addStop(new TripStop(trip, s0, 0, null, dep, BigDecimal.ZERO));
        trip.addStop(new TripStop(trip, s1, 1, dep.plusSeconds(3 * 3600), dep.plusSeconds(3 * 3600 + 900), BigDecimal.valueOf(200.0)));
        trip.addStop(new TripStop(trip, s2, 2, arr, null, BigDecimal.valueOf(480.0)));

        trip.addFare(new TripStopFare(trip, 0, 1, fare01));
        trip.addFare(new TripStopFare(trip, 1, 2, fare12));
        trip.addFare(new TripStopFare(trip, 0, 2, fare02));

        for (Seat s : seats) {
            trip.addSeat(new TripSeat(trip, s));
        }
        tripRepository.save(trip);
    }

    private void createConnectingTrip(String tripNumber, Route route, Bus bus, LocalDate date,
                                      Instant dep, Instant arr,
                                      Station s0, Station s1, List<Seat> seats,
                                      BigDecimal fare01) {
        Trip trip = new Trip(tripNumber, route, bus, date, "SCHEDULED", dep, arr);
        trip.addStop(new TripStop(trip, s0, 0, null, dep, BigDecimal.ZERO));
        trip.addStop(new TripStop(trip, s1, 1, arr, null, BigDecimal.valueOf(85.0)));

        trip.addFare(new TripStopFare(trip, 0, 1, fare01));

        for (Seat s : seats) {
            trip.addSeat(new TripSeat(trip, s));
        }
        tripRepository.save(trip);
    }
}
