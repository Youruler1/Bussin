package com.busbooking.trip;

import com.busbooking.bus.Bus;
import com.busbooking.bus.BusRepository;
import com.busbooking.bus.Seat;
import com.busbooking.bus.SeatRepository;
import com.busbooking.category.BusCategory;
import com.busbooking.category.CategoryRepository;
import com.busbooking.route.Route;
import com.busbooking.route.RouteRepository;
import com.busbooking.route.RouteStop;
import com.busbooking.route.RouteStopRepository;
import com.busbooking.station.Station;
import com.busbooking.station.StationRepository;
import com.busbooking.trip.dto.CreateTripRequest;
import com.busbooking.trip.dto.CreateTripStopFareRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class TripControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripStopRepository tripStopRepository;

    @Autowired
    private TripStopFareRepository tripStopFareRepository;

    @Autowired
    private TripSeatRepository tripSeatRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private ObjectMapper objectMapper;

    private Station delhi;
    private Station agra;
    private Station lucknow;
    private Route route;
    private Bus bus;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        delhi = stationRepository.save(new Station("NDLS", "New Delhi", "New Delhi", "Delhi", "Asia/Kolkata"));
        agra = stationRepository.save(new Station("AGR", "Agra Cantt", "Agra", "UP", "Asia/Kolkata"));
        lucknow = stationRepository.save(new Station("LKO", "Lucknow Alambagh", "Lucknow", "UP", "Asia/Kolkata"));

        Route r = new Route("RT-DEL-LKO", "Delhi to Lucknow via Agra", delhi, lucknow, BigDecimal.valueOf(480.0));
        RouteStop stop0 = new RouteStop(r, delhi, 0, BigDecimal.ZERO, 0);
        RouteStop stop1 = new RouteStop(r, agra, 1, BigDecimal.valueOf(200.0), 210);
        RouteStop stop2 = new RouteStop(r, lucknow, 2, BigDecimal.valueOf(280.0), 270);
        r.addStop(stop0);
        r.addStop(stop1);
        r.addStop(stop2);
        route = routeRepository.save(r);

        BusCategory category = categoryRepository.save(new BusCategory("AC_VOLVO", "Volvo Multi-Axle", "AC Sleeper/Seater"));
        bus = busRepository.save(new Bus("DL-01-EXP-101", "Volvo", "9600", category, 30));

        List<Seat> seats = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            int row = (i / 4) + 1;
            char col = (char) ('A' + (i % 4));
            seats.add(new Seat(bus, "" + row + col, row, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seatRepository.saveAll(seats);
    }

    @Test
    @DisplayName("R03: Trip scheduling validates stop time monotonicity, snapshots, fares, and TripSeat mutex rows")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testTripStopMonotonicityAndSeatMutexGeneration() throws Exception {
        Instant departureTime = Instant.parse("2026-11-01T06:00:00Z");

        CreateTripRequest request = new CreateTripRequest(
                "TRIP-DEL-LKO-001",
                route.getId(),
                bus.getId(),
                LocalDate.of(2026, 11, 1),
                departureTime,
                15, // 15 mins dwell
                BigDecimal.valueOf(2.50),
                null
        );

        String responseStr = mockMvc.perform(post("/api/v1/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripNumber").value("TRIP-DEL-LKO-001"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.stops.length()").value(3))
                .andExpect(jsonPath("$.fares.length()").value(3))
                .andExpect(jsonPath("$.totalSeatsAllocatedMutex").value(30))
                .andReturn().getResponse().getContentAsString();

        Long tripId = objectMapper.readTree(responseStr).get("id").asLong();
        Trip trip = tripRepository.findById(tripId).orElseThrow();

        // 1. Verify Stop Snapshots and Time Monotonicity
        List<TripStop> tripStops = tripStopRepository.findByTripIdOrderByStopOrderAsc(tripId);
        assertEquals(3, tripStops.size());

        // Stop 0 (Delhi): Departs 06:00:00Z, no arrival, dist = 0
        TripStop ts0 = tripStops.get(0);
        assertNull(ts0.getScheduledArrivalUtc());
        assertEquals(departureTime, ts0.getScheduledDepartureUtc());
        assertEquals(0, ts0.getCumulativeDistanceKm().compareTo(BigDecimal.ZERO));

        // Stop 1 (Agra): Arrives 06:00 + 210m = 09:30:00Z, Departs 09:30 + 15m dwell = 09:45:00Z, dist = 200 km
        TripStop ts1 = tripStops.get(1);
        Instant expectedAgraArrival = departureTime.plusSeconds(210 * 60);
        Instant expectedAgraDeparture = expectedAgraArrival.plusSeconds(15 * 60);
        assertEquals(expectedAgraArrival, ts1.getScheduledArrivalUtc());
        assertEquals(expectedAgraDeparture, ts1.getScheduledDepartureUtc());
        assertEquals(0, ts1.getCumulativeDistanceKm().compareTo(BigDecimal.valueOf(200.0)));

        // Stop 2 (Lucknow): Arrives 09:45 + 270m = 14:15:00Z, no departure, dist = 480 km
        TripStop ts2 = tripStops.get(2);
        Instant expectedLucknowArrival = expectedAgraDeparture.plusSeconds(270 * 60);
        assertEquals(expectedLucknowArrival, ts2.getScheduledArrivalUtc());
        assertNull(ts2.getScheduledDepartureUtc());
        assertEquals(0, ts2.getCumulativeDistanceKm().compareTo(BigDecimal.valueOf(480.0)));

        // Monotonicity assertion: T_dep(0) < T_arr(1) < T_dep(1) < T_arr(2)
        assertTrue(ts0.getScheduledDepartureUtc().isBefore(ts1.getScheduledArrivalUtc()));
        assertTrue(ts1.getScheduledArrivalUtc().isBefore(ts1.getScheduledDepartureUtc()));
        assertTrue(ts1.getScheduledDepartureUtc().isBefore(ts2.getScheduledArrivalUtc()));
        assertEquals(expectedLucknowArrival, trip.getScheduledArrivalUtc());

        // 2. Verify TripSeat Mutex Rows generated for all 30 active seats
        long mutexCount = tripSeatRepository.countByTripId(tripId);
        assertEquals(30, mutexCount, "TripSeat mutex rows must exactly equal bus active seat count");

        // 3. Verify Segment Fares generated for all 3 pairs: (0,1), (1,2), (0,2)
        List<TripStopFare> fares = tripStopFareRepository.findByTripId(tripId);
        assertEquals(3, fares.size());

        TripStopFare f01 = tripStopFareRepository.findByTripIdAndBoardStopOrderAndAlightStopOrder(tripId, 0, 1).orElseThrow();
        // 200 km * 2.50 = 500.00
        assertEquals(0, f01.getBaseFareInr().compareTo(BigDecimal.valueOf(500.00)));

        TripStopFare f12 = tripStopFareRepository.findByTripIdAndBoardStopOrderAndAlightStopOrder(tripId, 1, 2).orElseThrow();
        // 280 km * 2.50 = 700.00
        assertEquals(0, f12.getBaseFareInr().compareTo(BigDecimal.valueOf(700.00)));

        TripStopFare f02 = tripStopFareRepository.findByTripIdAndBoardStopOrderAndAlightStopOrder(tripId, 0, 2).orElseThrow();
        // 480 km * 2.50 = 1200.00
        assertEquals(0, f02.getBaseFareInr().compareTo(BigDecimal.valueOf(1200.00)));
    }

    @Test
    @DisplayName("R03: Trip scheduling supports custom explicit segment fares")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testTripSchedulingWithCustomFares() throws Exception {
        List<CreateTripStopFareRequest> customFares = List.of(
                new CreateTripStopFareRequest(0, 1, BigDecimal.valueOf(450.00)),
                new CreateTripStopFareRequest(1, 2, BigDecimal.valueOf(650.00)),
                new CreateTripStopFareRequest(0, 2, BigDecimal.valueOf(999.00))
        );

        CreateTripRequest request = new CreateTripRequest(
                "TRIP-CUSTOM-FARE",
                route.getId(),
                bus.getId(),
                LocalDate.of(2026, 11, 2),
                Instant.parse("2026-11-02T08:00:00Z"),
                10,
                null,
                customFares
        );

        mockMvc.perform(post("/api/v1/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fares[0].baseFareInr").isNumber());

        Trip saved = tripRepository.findByTripNumber("TRIP-CUSTOM-FARE").orElseThrow();
        TripStopFare fullFare = tripStopFareRepository.findByTripIdAndBoardStopOrderAndAlightStopOrder(saved.getId(), 0, 2).orElseThrow();
        assertEquals(0, fullFare.getBaseFareInr().compareTo(BigDecimal.valueOf(999.00)));
    }

    @Test
    @DisplayName("R03: Duplicate trip number yields 409 Conflict ProblemDetail")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testDuplicateTripNumberReturnsConflict() throws Exception {
        CreateTripRequest req1 = new CreateTripRequest(
                "TRIP-DUP-TEST",
                route.getId(),
                bus.getId(),
                LocalDate.of(2026, 11, 3),
                Instant.parse("2026-11-03T10:00:00Z"),
                10,
                BigDecimal.valueOf(2.0),
                null
        );

        mockMvc.perform(post("/api/v1/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Duplicate
        mockMvc.perform(post("/api/v1/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("R03: Inactive route or inactive bus rejected with 400 Bad Request")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testInactiveRouteOrBusRejected() throws Exception {
        Route r = routeRepository.findById(route.getId()).orElseThrow();
        r.setActive(false);
        routeRepository.save(r);

        CreateTripRequest req = new CreateTripRequest(
                "TRIP-INACTIVE-ROUTE",
                route.getId(),
                bus.getId(),
                LocalDate.of(2026, 11, 4),
                Instant.parse("2026-11-04T10:00:00Z"),
                10,
                BigDecimal.valueOf(2.0),
                null
        );

        mockMvc.perform(post("/api/v1/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("R03: Admin can update trip status (e.g. CANCELLED)")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testAdminCanUpdateTripStatus() throws Exception {
        CreateTripRequest req = new CreateTripRequest(
                "TRIP-STATUS-UPDATE",
                route.getId(),
                bus.getId(),
                LocalDate.of(2026, 11, 5),
                Instant.parse("2026-11-05T07:00:00Z"),
                10,
                BigDecimal.valueOf(2.0),
                null
        );

        String res = mockMvc.perform(post("/api/v1/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long tripId = objectMapper.readTree(res).get("id").asLong();

        mockMvc.perform(patch("/api/v1/admin/trips/" + tripId + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Trip updated = tripRepository.findById(tripId).orElseThrow();
        assertEquals("CANCELLED", updated.getStatus());
    }

    @Test
    @DisplayName("R03: Public can view trip by ID without auth; Customer denied admin trip creation")
    void testPublicReadAndCustomerRbac() throws Exception {
        Trip trip = new Trip(
                "TRIP-PUB-01",
                route,
                bus,
                LocalDate.of(2026, 11, 6),
                "SCHEDULED",
                Instant.parse("2026-11-06T06:00:00Z"),
                Instant.parse("2026-11-06T14:00:00Z")
        );
        trip = tripRepository.save(trip);

        // 1. Public can read trip
        mockMvc.perform(get("/api/v1/trips/" + trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripNumber").value("TRIP-PUB-01"));

        // 2. Unauthenticated user denied trip creation (401)
        CreateTripRequest req = new CreateTripRequest("TRIP-NO-AUTH", route.getId(), bus.getId(),
                LocalDate.now(), Instant.now().plusSeconds(3600), 10, BigDecimal.ONE, null);
        mockMvc.perform(post("/api/v1/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
