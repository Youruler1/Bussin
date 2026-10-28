package com.busbooking.booking;

import com.busbooking.auth.Role;
import com.busbooking.auth.User;
import com.busbooking.auth.UserRepository;
import com.busbooking.booking.dto.CreateBookingRequest;
import com.busbooking.booking.dto.LegAllocationRequest;
import com.busbooking.booking.dto.PassengerLegBooking;
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
import com.busbooking.trip.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class BookingServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripStopRepository tripStopRepository;

    @Autowired
    private TripStopFareRepository tripStopFareRepository;

    @Autowired
    private TripSeatRepository tripSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private ObjectMapper objectMapper;

    private Station delhi;
    private Station agra;
    private Station lucknow;
    private Station kanpur;

    private Route route1;
    private Route route2;
    private Bus bus1;
    private Bus bus2;

    private Trip tripLeg1;
    private Trip tripLeg2;

    private User customer;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        customer = userRepository.save(new User("customer@test.com", "hashedPass", "Customer Test", "+919876543210", Role.ROLE_CUSTOMER));

        delhi = stationRepository.save(new Station("NDLS", "New Delhi", "New Delhi", "Delhi", "Asia/Kolkata"));
        agra = stationRepository.save(new Station("AGR", "Agra Cantt", "Agra", "UP", "Asia/Kolkata"));
        lucknow = stationRepository.save(new Station("LKO", "Lucknow Alambagh", "Lucknow", "UP", "Asia/Kolkata"));
        kanpur = stationRepository.save(new Station("KNP", "Kanpur Central", "Kanpur", "UP", "Asia/Kolkata"));

        // Route 1: Delhi (0) -> Agra (1) -> Lucknow (2)
        Route r1 = new Route("RT-DEL-LKO", "Delhi to Lucknow via Agra", delhi, lucknow, BigDecimal.valueOf(480.0));
        r1.addStop(new RouteStop(r1, delhi, 0, BigDecimal.ZERO, 0));
        r1.addStop(new RouteStop(r1, agra, 1, BigDecimal.valueOf(200.0), 210));
        r1.addStop(new RouteStop(r1, lucknow, 2, BigDecimal.valueOf(280.0), 270));
        route1 = routeRepository.save(r1);

        // Route 2: Lucknow (0) -> Kanpur (1)
        Route r2 = new Route("RT-LKO-KNP", "Lucknow to Kanpur Express", lucknow, kanpur, BigDecimal.valueOf(85.0));
        r2.addStop(new RouteStop(r2, lucknow, 0, BigDecimal.ZERO, 0));
        r2.addStop(new RouteStop(r2, kanpur, 1, BigDecimal.valueOf(85.0), 90));
        route2 = routeRepository.save(r2);

        BusCategory category = categoryRepository.save(new BusCategory("AC_VOLVO", "Volvo Multi-Axle", "AC Coach"));
        bus1 = busRepository.save(new Bus("DL-01-V-100", "Volvo", "9600", category, 20));
        bus2 = busRepository.save(new Bus("UP-32-T-200", "Tata", "Starbus", category, 20));

        List<Seat> seats1 = new ArrayList<>();
        List<Seat> seats2 = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            seats1.add(new Seat(bus1, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
            seats2.add(new Seat(bus2, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seatRepository.saveAll(seats1);
        seatRepository.saveAll(seats2);

        // Trip 1: Delhi -> Agra -> Lucknow
        tripLeg1 = createTrip(route1, bus1, "TRIP-LEG-1",
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T14:00:00Z"),
                BigDecimal.valueOf(400.00), BigDecimal.valueOf(560.00), BigDecimal.valueOf(950.00));

        // Trip 2: Lucknow -> Kanpur
        tripLeg2 = createTrip(route2, bus2, "TRIP-LEG-2",
                Instant.parse("2026-11-01T15:30:00Z"),
                Instant.parse("2026-11-01T17:00:00Z"),
                BigDecimal.valueOf(200.00), null, null);
    }

    private Trip createTrip(Route route, Bus bus, String tripNum, Instant dep0, Instant arrEnd,
                            BigDecimal f01, BigDecimal f12, BigDecimal f02) {
        Trip trip = new Trip(tripNum, route, bus, LocalDate.of(2026, 11, 1), "SCHEDULED", dep0, arrEnd);
        List<RouteStop> rStops = route.getStops();
        for (int i = 0; i < rStops.size(); i++) {
            RouteStop rs = rStops.get(i);
            Instant arr = (i == 0) ? null : dep0.plusSeconds(rs.getTypicalDurationMinutes() * 60L);
            Instant dep = (i == rStops.size() - 1) ? null : dep0;
            trip.addStop(new TripStop(trip, rs.getStation(), rs.getStopOrder(), arr, dep, rs.getDistanceFromPrevKm()));
        }

        if (f01 != null) trip.addFare(new TripStopFare(trip, 0, 1, f01));
        if (f12 != null) trip.addFare(new TripStopFare(trip, 1, 2, f12));
        if (f02 != null) trip.addFare(new TripStopFare(trip, 0, 2, f02));

        for (Seat seat : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus.getId())) {
            trip.addSeat(new TripSeat(trip, seat));
        }
        return tripRepository.save(trip);
    }

    @Test
    @DisplayName("R06: Partial-route seats: Same seat can be reserved on disjoint intervals [0,1) and [1,2)")
    @WithMockUser(username = "customer@test.com", authorities = {"ROLE_CUSTOMER"})
    void testDisjointSeatAllocationsSucceed() throws Exception {
        Seat seat1 = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId()).get(0);

        // 1. Passenger A books Seat S1 on [0, 1) (Delhi -> Agra)
        CreateBookingRequest reqA = new CreateBookingRequest(
                "passengerA@test.com",
                "+919111111111",
                List.of(new PassengerLegBooking("Passenger A", 25, "MALE",
                        List.of(new LegAllocationRequest(tripLeg1.getId(), seat1.getId(), 0, 1))))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmountInr").value(400.00));

        // 2. Passenger B books exact same Seat S1 on DISJOINT interval [1, 2) (Agra -> Lucknow)
        CreateBookingRequest reqB = new CreateBookingRequest(
                "passengerB@test.com",
                "+919222222222",
                List.of(new PassengerLegBooking("Passenger B", 32, "FEMALE",
                        List.of(new LegAllocationRequest(tripLeg1.getId(), seat1.getId(), 1, 2))))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmountInr").value(560.00));

        // Verify database state: exactly 2 distinct bookings, and 2 active allocations on the exact same TripSeat
        assertEquals(2, bookingRepository.count());
        assertEquals(2, seatAllocationRepository.count());
    }

    @Test
    @DisplayName("R07: Overlap rejection: Reserving an overlapping segment for the same seat yields 409 Conflict")
    @WithMockUser(username = "customer@test.com", authorities = {"ROLE_CUSTOMER"})
    void testOverlappingSeatAllocationRejected() throws Exception {
        Seat seat1 = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId()).get(0);

        // 1. Initial booking on [0, 1) (Delhi -> Agra)
        CreateBookingRequest req1 = new CreateBookingRequest(
                "first@test.com",
                "+919111111111",
                List.of(new PassengerLegBooking("Passenger 1", 30, "MALE",
                        List.of(new LegAllocationRequest(tripLeg1.getId(), seat1.getId(), 0, 1))))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // 2. Conflicting attempt A: Exact same interval [0, 1)
        CreateBookingRequest conflictReqA = new CreateBookingRequest(
                "second@test.com",
                "+919222222222",
                List.of(new PassengerLegBooking("Passenger 2", 28, "FEMALE",
                        List.of(new LegAllocationRequest(tripLeg1.getId(), seat1.getId(), 0, 1))))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictReqA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        // 3. Conflicting attempt B: Overlapping interval [0, 2) (Delhi -> Lucknow)
        CreateBookingRequest conflictReqB = new CreateBookingRequest(
                "third@test.com",
                "+919333333333",
                List.of(new PassengerLegBooking("Passenger 3", 40, "MALE",
                        List.of(new LegAllocationRequest(tripLeg1.getId(), seat1.getId(), 0, 2))))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictReqB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        // Verify only 1 booking and 1 allocation exist in the DB
        assertEquals(1, bookingRepository.count());
        assertEquals(1, seatAllocationRepository.count());
    }

    @Test
    @DisplayName("R10: Multi-leg atomicity: Failure in Leg 2 leaves zero new active allocations (All-or-Nothing)")
    @WithMockUser(username = "customer@test.com", authorities = {"ROLE_CUSTOMER"})
    void testMultiLegAtomicRollbackOnSecondLegFailure() throws Exception {
        Seat seatLeg1 = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId()).get(0); // S1
        Seat seatLeg2 = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus2.getId()).get(0); // S1 on bus 2

        // Prior booking occupies seat S1 on Leg 2 [0, 1)
        CreateBookingRequest priorBookingReq = new CreateBookingRequest(
                "prior@test.com",
                "+919999900000",
                List.of(new PassengerLegBooking("Prior Occupant", 50, "MALE",
                        List.of(new LegAllocationRequest(tripLeg2.getId(), seatLeg2.getId(), 0, 1))))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priorBookingReq)))
                .andExpect(status().isCreated());

        assertEquals(1, bookingRepository.count());
        assertEquals(1, seatAllocationRepository.count());

        // Now, a new customer attempts a connecting booking (Leg 1 on seatLeg1, Leg 2 on seatLeg2)
        // Leg 1 seat is completely FREE, but Leg 2 seat is CONFLICTED!
        CreateBookingRequest connectingReq = new CreateBookingRequest(
                "connecting@test.com",
                "+919888877777",
                List.of(new PassengerLegBooking("Alice Traveler", 29, "FEMALE", List.of(
                        new LegAllocationRequest(tripLeg1.getId(), seatLeg1.getId(), 0, 2), // Leg 1: Delhi -> Lucknow
                        new LegAllocationRequest(tripLeg2.getId(), seatLeg2.getId(), 0, 1)  // Leg 2: Lucknow -> Kanpur (CONFLICT!)
                )))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectingReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        // ATOMICITY ASSERTION:
        // Because Leg 2 failed, the entire transaction MUST have rolled back.
        // Leg 1 MUST NOT have any new allocations left in the DB!
        assertEquals(1, bookingRepository.count(), "Only prior booking must remain");
        assertEquals(1, seatAllocationRepository.count(), "Only prior allocation must remain; zero allocations from failed connecting booking");
    }

    @Test
    @DisplayName("R12: Price snapshot: Subsequent fare edits do not alter the booked ticket price")
    @WithMockUser(username = "customer@test.com", authorities = {"ROLE_CUSTOMER"})
    void testSubsequentFareEditsDoNotChangeBookedPrice() throws Exception {
        Seat seat = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId()).get(5);

        // 1. Book Delhi -> Lucknow at initial fare ₹950
        CreateBookingRequest req = new CreateBookingRequest(
                "snapshot@test.com",
                "+919555544444",
                List.of(new PassengerLegBooking("Bob Snapshot", 35, "MALE",
                        List.of(new LegAllocationRequest(tripLeg1.getId(), seat.getId(), 0, 2))))
        );

        String responseStr = mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmountInr").value(950.00))
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(responseStr).get("id").asLong();

        // 2. Admin subsequently edits the segment fare in the database from 950 to 1800
        TripStopFare fare = tripStopFareRepository.findByTripIdAndBoardStopOrderAndAlightStopOrder(tripLeg1.getId(), 0, 2).orElseThrow();
        fare.setBaseFareInr(BigDecimal.valueOf(1800.00));
        tripStopFareRepository.save(fare);

        // 3. Customer reads their booking ticket /api/v1/bookings/{id}
        // Price MUST remain exactly ₹950.00 (snapshot immutable)
        mockMvc.perform(get("/api/v1/bookings/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmountInr").value(950.00))
                .andExpect(jsonPath("$.legs[0].allocatedSeats[0].fareInr").value(950.00));
    }

    @Test
    @DisplayName("Idempotency: Repeated request with same Idempotency-Key returns existing booking without duplicates")
    @WithMockUser(username = "customer@test.com", authorities = {"ROLE_CUSTOMER"})
    void testIdempotencyHeaderDeduplication() throws Exception {
        Seat seat = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId()).get(8);

        CreateBookingRequest req = new CreateBookingRequest(
                "idem@test.com",
                "+919444433333",
                List.of(new PassengerLegBooking("Idem User", 26, "FEMALE",
                        List.of(new LegAllocationRequest(tripLeg1.getId(), seat.getId(), 0, 1))))
        );

        String firstResponse = mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .header("Idempotency-Key", "unique-idem-token-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ref1 = objectMapper.readTree(firstResponse).get("bookingReference").asText();

        // Repeated invocation with identical idempotency key
        String secondResponse = mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .header("Idempotency-Key", "unique-idem-token-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ref2 = objectMapper.readTree(secondResponse).get("bookingReference").asText();

        assertEquals(ref1, ref2, "Idempotent response must return identical booking reference");
        assertEquals(1, bookingRepository.count(), "Only 1 booking must be saved in the database");
        assertEquals(1, seatAllocationRepository.count(), "Only 1 seat allocation must be saved in the database");
    }
}
