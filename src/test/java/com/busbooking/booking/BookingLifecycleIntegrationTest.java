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

import static org.hamcrest.Matchers.*;
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
class BookingLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripStopRepository tripStopRepository;

    @Autowired
    private TripStopFareRepository tripStopFareRepository;

    @Autowired
    private TripSeatRepository tripSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    private Station delhi;
    private Station agra;
    private Station lucknow;
    private Trip trip;
    private Seat seat1;
    private Seat seat2;
    private User customer1;
    private User customer2;
    private User admin;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        customer1 = userRepository.save(new User("cust1@test.com", "pass", "Customer One", "+919000000001", Role.ROLE_CUSTOMER));
        customer2 = userRepository.save(new User("cust2@test.com", "pass", "Customer Two", "+919000000002", Role.ROLE_CUSTOMER));
        admin = userRepository.save(new User("admin@test.com", "pass", "Admin User", "+919000000000", Role.ROLE_ADMIN));

        delhi = stationRepository.save(new Station("NDLS", "Delhi", "Delhi", "Delhi", "Asia/Kolkata"));
        agra = stationRepository.save(new Station("AGR", "Agra", "Agra", "UP", "Asia/Kolkata"));
        lucknow = stationRepository.save(new Station("LKO", "Lucknow", "Lucknow", "UP", "Asia/Kolkata"));

        Route route = new Route("RT-LIFE", "Delhi-Agra-Lucknow", delhi, lucknow, BigDecimal.valueOf(480.0));
        route.addStop(new RouteStop(route, delhi, 0, BigDecimal.ZERO, 0));
        route.addStop(new RouteStop(route, agra, 1, BigDecimal.valueOf(200.0), 200));
        route.addStop(new RouteStop(route, lucknow, 2, BigDecimal.valueOf(280.0), 280));
        route = routeRepository.save(route);

        BusCategory category = categoryRepository.save(new BusCategory("AC_SEATER", "AC Seater", "AC"));
        Bus bus = busRepository.save(new Bus("DL-LIFE-01", "Tata", "Starbus", category, 10));

        List<Seat> seats = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            seats.add(new Seat(bus, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seats = seatRepository.saveAll(seats);
        seat1 = seats.get(0);
        seat2 = seats.get(1);

        trip = new Trip("TRIP-LIFE-01", route, bus, LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T14:00:00Z"));
        trip.addStop(new TripStop(trip, delhi, 0, null, Instant.parse("2026-11-01T06:00:00Z"), BigDecimal.ZERO));
        trip.addStop(new TripStop(trip, agra, 1, Instant.parse("2026-11-01T09:30:00Z"), Instant.parse("2026-11-01T09:45:00Z"), BigDecimal.valueOf(200.0)));
        trip.addStop(new TripStop(trip, lucknow, 2, Instant.parse("2026-11-01T14:00:00Z"), null, BigDecimal.valueOf(480.0)));
        trip.addFare(new TripStopFare(trip, 0, 1, BigDecimal.valueOf(400.00)));
        trip.addFare(new TripStopFare(trip, 1, 2, BigDecimal.valueOf(560.00)));
        trip.addFare(new TripStopFare(trip, 0, 2, BigDecimal.valueOf(950.00)));

        for (Seat s : seats) {
            trip.addSeat(new TripSeat(trip, s));
        }
        trip = tripRepository.save(trip);
    }

    private Long createTestBooking(Long customerId) {
        CreateBookingRequest req = new CreateBookingRequest(
                "cust1@test.com",
                "+919000000001",
                List.of(new PassengerLegBooking("Parijaat Gaur", 28, "MALE",
                        List.of(new LegAllocationRequest(trip.getId(), seat1.getId(), 0, 2))))
        );
        return bookingService.createBooking(customerId, req, null).id();
    }

    @Test
    @DisplayName("R11: Whole-itinerary cancellation marks booking CANCELLED, sets cancelledAt, and releases seat allocations")
    @WithMockUser(username = "cust1@test.com", roles = {"CUSTOMER"})
    void testCancelBookingReleasesSeats() throws Exception {
        Long bookingId = createTestBooking(customer1.getId());

        // Prior to cancellation: seat1 is occupied on [0, 2)
        var occupied = seatAllocationRepository.findOccupiedTripSeatIds(List.of(tripSeatRepository.findByTripIdAndSeatId(trip.getId(), seat1.getId()).get().getId()), 0, 2);
        assertFalse(occupied.isEmpty());

        // Cancel booking
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        // Assert database state: allocations are CANCELLED
        Booking dbBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals("CANCELLED", dbBooking.getStatus());
        assertNotNull(dbBooking.getCancelledAt());
        List<SeatAllocation> allocations = seatAllocationRepository.findAll();
        assertFalse(allocations.isEmpty());
        for (SeatAllocation alloc : allocations) {
            assertEquals("CANCELLED", alloc.getStatus());
        }

        // Seats are now released (occupied count should be 0)
        var occupiedAfter = seatAllocationRepository.findOccupiedTripSeatIds(List.of(tripSeatRepository.findByTripIdAndSeatId(trip.getId(), seat1.getId()).get().getId()), 0, 2);
        assertTrue(occupiedAfter.isEmpty());
    }

    @Test
    @DisplayName("R11: Cancellation is idempotent: calling cancel on an already CANCELLED booking returns 200 without error")
    @WithMockUser(username = "cust1@test.com", roles = {"CUSTOMER"})
    void testCancelBookingIdempotent() throws Exception {
        Long bookingId = createTestBooking(customer1.getId());

        // First cancel
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Second cancel
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("R11: Released seat can be immediately re-booked by another customer")
    @WithMockUser(username = "cust2@test.com", roles = {"CUSTOMER"})
    void testReleasedSeatCanBeRebooked() throws Exception {
        Long bookingId = createTestBooking(customer1.getId());

        // Customer 1 cancels
        bookingService.cancelBooking(bookingId, customer1.getId(), false);

        // Customer 2 now books the exact same seat
        CreateBookingRequest req2 = new CreateBookingRequest(
                "cust2@test.com",
                "+919000000002",
                List.of(new PassengerLegBooking("Second Passenger", 25, "FEMALE",
                        List.of(new LegAllocationRequest(trip.getId(), seat1.getId(), 0, 2))))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());
    }

    @Test
    @DisplayName("R14: Customer cannot view or cancel another customer's booking (403 Forbidden)")
    @WithMockUser(username = "cust2@test.com", roles = {"CUSTOMER"})
    void testCustomerCannotAccessAnotherCustomersBooking() throws Exception {
        Long bookingId = createTestBooking(customer1.getId());

        // Customer 2 tries to GET customer 1's booking
        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isForbidden());

        // Customer 2 tries to CANCEL customer 1's booking
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("R14: Admin can view and cancel any customer's booking")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void testAdminCanViewAndCancelAnyBooking() throws Exception {
        Long bookingId = createTestBooking(customer1.getId());

        // Admin views Customer 1's booking
        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Admin cancels Customer 1's booking
        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("R14: Customer Booking History: GET /api/v1/me/bookings returns customer's bookings ordered by date desc")
    @WithMockUser(username = "cust1@test.com", roles = {"CUSTOMER"})
    void testGetCustomerBookingHistory() throws Exception {
        createTestBooking(customer1.getId());

        mockMvc.perform(get("/api/v1/me/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].routeSummary").value("Delhi -> Lucknow"))
                .andExpect(jsonPath("$[0].legCount").value(1))
                .andExpect(jsonPath("$[0].passengerCount").value(1))
                .andExpect(jsonPath("$[0].totalAmountInr").value(950.00))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("R14: Printable Ticket: GET /api/v1/bookings/{id} returns complete breakdown with passengers and stops")
    @WithMockUser(username = "cust1@test.com", roles = {"CUSTOMER"})
    void testGetPrintableTicket() throws Exception {
        Long bookingId = createTestBooking(customer1.getId());

        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.contactEmail").value("cust1@test.com"))
                .andExpect(jsonPath("$.contactPhone").value("+919000000001"))
                .andExpect(jsonPath("$.legs", hasSize(1)))
                .andExpect(jsonPath("$.legs[0].boardStationName").value("Delhi"))
                .andExpect(jsonPath("$.legs[0].alightStationName").value("Lucknow"))
                .andExpect(jsonPath("$.legs[0].departureTime").value("2026-11-01T06:00:00Z"))
                .andExpect(jsonPath("$.legs[0].arrivalTime").value("2026-11-01T14:00:00Z"))
                .andExpect(jsonPath("$.legs[0].allocatedSeats", hasSize(1)))
                .andExpect(jsonPath("$.legs[0].allocatedSeats[0].passengerName").value("Parijaat Gaur"))
                .andExpect(jsonPath("$.legs[0].allocatedSeats[0].passengerAge").value(28))
                .andExpect(jsonPath("$.legs[0].allocatedSeats[0].passengerGender").value("MALE"))
                .andExpect(jsonPath("$.legs[0].allocatedSeats[0].seatNumber").value("S1"))
                .andExpect(jsonPath("$.legs[0].allocatedSeats[0].fareInr").value(950.00));
    }
}
