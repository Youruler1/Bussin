package com.busbooking.journey;

import com.busbooking.auth.Role;
import com.busbooking.auth.User;
import com.busbooking.auth.UserRepository;
import com.busbooking.booking.Booking;
import com.busbooking.booking.BookingLeg;
import com.busbooking.booking.BookingRepository;
import com.busbooking.booking.SeatAllocation;
import com.busbooking.booking.SeatAllocationRepository;
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
import com.busbooking.trip.Trip;
import com.busbooking.trip.TripRepository;
import com.busbooking.trip.TripSeat;
import com.busbooking.trip.TripSeatRepository;
import com.busbooking.trip.TripStop;
import com.busbooking.trip.TripStopFare;
import com.busbooking.trip.TripStopFareRepository;
import com.busbooking.trip.TripStopRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class JourneySearchIntegrationTest {

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
    private Station jaipur;
    private Trip morningTrip;
    private Trip eveningTrip;
    private User testCustomer;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        testCustomer = userRepository.save(new User("customer@test.com", "passHash123", "Test Customer", "+919999988888", Role.ROLE_CUSTOMER));

        delhi = stationRepository.save(new Station("NDLS", "New Delhi", "New Delhi", "Delhi", "Asia/Kolkata"));
        agra = stationRepository.save(new Station("AGR", "Agra Cantt", "Agra", "UP", "Asia/Kolkata"));
        lucknow = stationRepository.save(new Station("LKO", "Lucknow Alambagh", "Lucknow", "UP", "Asia/Kolkata"));
        jaipur = stationRepository.save(new Station("JPR", "Jaipur", "Jaipur", "Rajasthan", "Asia/Kolkata"));

        Route r = new Route("RT-DEL-LKO", "Delhi to Lucknow via Agra", delhi, lucknow, BigDecimal.valueOf(480.0));
        RouteStop s0 = new RouteStop(r, delhi, 0, BigDecimal.ZERO, 0);
        RouteStop s1 = new RouteStop(r, agra, 1, BigDecimal.valueOf(200.0), 210);
        RouteStop s2 = new RouteStop(r, lucknow, 2, BigDecimal.valueOf(280.0), 270);
        r.addStop(s0);
        r.addStop(s1);
        r.addStop(s2);
        Route savedRoute = routeRepository.save(r);

        BusCategory category = categoryRepository.save(new BusCategory("VOLVO_AC", "Volvo Multi-Axle", "AC Coach"));
        Bus bus1 = busRepository.save(new Bus("DL-01-V-100", "Volvo", "9600", category, 20));
        Bus bus2 = busRepository.save(new Bus("DL-01-V-200", "Volvo", "9600", category, 20));

        List<Seat> seats1 = new ArrayList<>();
        List<Seat> seats2 = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int row = (i / 4) + 1;
            char col = (char) ('A' + (i % 4));
            seats1.add(new Seat(bus1, "" + row + col, row, (i % 4) + 1, "SEATER", "LOWER"));
            seats2.add(new Seat(bus2, "" + row + col, row, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seatRepository.saveAll(seats1);
        seatRepository.saveAll(seats2);

        // Morning Trip: departs Delhi 06:00 UTC (11:30 IST), Agra 09:30 UTC, Lucknow 14:00 UTC
        morningTrip = createTripWithStopsAndFares("TRIP-MORNING-01", savedRoute, bus1,
                LocalDate.of(2026, 11, 1),
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T09:30:00Z"),
                Instant.parse("2026-11-01T09:45:00Z"),
                Instant.parse("2026-11-01T14:15:00Z"),
                seats1);

        // Evening Trip: departs Delhi 16:00 UTC (21:30 IST), Agra 19:30 UTC, Lucknow 00:15 UTC next day
        eveningTrip = createTripWithStopsAndFares("TRIP-EVENING-02", savedRoute, bus2,
                LocalDate.of(2026, 11, 1),
                Instant.parse("2026-11-01T16:00:00Z"),
                Instant.parse("2026-11-01T19:30:00Z"),
                Instant.parse("2026-11-01T19:45:00Z"),
                Instant.parse("2026-11-02T00:15:00Z"),
                seats2);
    }

    private Trip createTripWithStopsAndFares(String tripNum, Route route, Bus bus, LocalDate date,
                                             Instant dep0, Instant arr1, Instant dep1, Instant arr2,
                                             List<Seat> seats) {
        Trip trip = new Trip(tripNum, route, bus, date, "SCHEDULED", dep0, arr2);
        TripStop ts0 = new TripStop(trip, delhi, 0, null, dep0, BigDecimal.ZERO);
        TripStop ts1 = new TripStop(trip, agra, 1, arr1, dep1, BigDecimal.valueOf(200.0));
        TripStop ts2 = new TripStop(trip, lucknow, 2, arr2, null, BigDecimal.valueOf(480.0));
        trip.addStop(ts0);
        trip.addStop(ts1);
        trip.addStop(ts2);

        // Fares: (0,1)=400, (1,2)=560, (0,2)=950
        trip.addFare(new TripStopFare(trip, 0, 1, BigDecimal.valueOf(400.00)));
        trip.addFare(new TripStopFare(trip, 1, 2, BigDecimal.valueOf(560.00)));
        trip.addFare(new TripStopFare(trip, 0, 2, BigDecimal.valueOf(950.00)));

        for (Seat seat : seats) {
            trip.addSeat(new TripSeat(trip, seat));
        }
        return tripRepository.save(trip);
    }

    @Test
    @DisplayName("R05: Direct search finds full-route trips with correct stop details, dates, and prices")
    void testDirectSearchFullRoute() throws Exception {
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", lucknow.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("DIRECT"))
                .andExpect(jsonPath("$.totalResults").value(2))
                .andExpect(jsonPath("$.options[0].totalFareInr").value(950.00))
                .andExpect(jsonPath("$.options[0].legs[0].tripNumber").value("TRIP-MORNING-01"))
                .andExpect(jsonPath("$.options[0].legs[0].originStationName").value("New Delhi"))
                .andExpect(jsonPath("$.options[0].legs[0].destinationStationName").value("Lucknow Alambagh"))
                .andExpect(jsonPath("$.options[0].legs[0].availableSeats").value(20))
                .andExpect(jsonPath("$.options[0].legs[0].segmentFareInr").value(950.00))
                .andExpect(jsonPath("$.options[1].legs[0].tripNumber").value("TRIP-EVENING-02"));
    }

    @Test
    @DisplayName("R05: Direct search finds intermediate stop pairs (Delhi -> Agra and Agra -> Lucknow)")
    void testDirectSearchIntermediateStopPairs() throws Exception {
        // Intermediate Pair 1: Delhi -> Agra (Stop 0 to Stop 1)
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", agra.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("DIRECT"))
                .andExpect(jsonPath("$.options[0].totalFareInr").value(400.00))
                .andExpect(jsonPath("$.options[0].legs[0].boardStopOrder").value(0))
                .andExpect(jsonPath("$.options[0].legs[0].alightStopOrder").value(1));

        // Intermediate Pair 2: Agra -> Lucknow (Stop 1 to Stop 2)
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", agra.getId().toString())
                        .param("destinationStationId", lucknow.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("DIRECT"))
                .andExpect(jsonPath("$.options[0].totalFareInr").value(560.00))
                .andExpect(jsonPath("$.options[0].legs[0].boardStopOrder").value(1))
                .andExpect(jsonPath("$.options[0].legs[0].alightStopOrder").value(2));
    }

    @Test
    @DisplayName("R05: Departure time window filters out-of-range trips")
    void testDepartureTimeWindowFiltering() throws Exception {
        // Delhi 06:00 UTC = 11:30 IST; Delhi 16:00 UTC = 21:30 IST
        // Filter for morning window: 08:00 to 14:00 IST -> includes only Morning Trip (11:30 IST)
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", lucknow.getId().toString())
                        .param("travelDate", "2026-11-01")
                        .param("departureFrom", "08:00:00")
                        .param("departureTo", "14:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.options[0].legs[0].tripNumber").value("TRIP-MORNING-01"));

        // Filter for late evening window: 20:00 to 23:59 IST -> includes only Evening Trip (21:30 IST)
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", lucknow.getId().toString())
                        .param("travelDate", "2026-11-01")
                        .param("departureFrom", "20:00:00")
                        .param("departureTo", "23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.options[0].legs[0].tripNumber").value("TRIP-EVENING-02"));
    }

    @Test
    @DisplayName("R05 & R06: Available seat count reflects overlapping allocations and enables disjoint resale")
    void testAvailableSeatCalculationWithAllocations() throws Exception {
        // Find a TripSeat for morning trip
        List<TripSeat> tripSeats = tripSeatRepository.findByTripId(morningTrip.getId());
        assertFalse(tripSeats.isEmpty());
        TripSeat bookedSeat = tripSeats.get(0); // seat 1A

        // Book seat 1A on segment [0, 1) (Delhi -> Agra)
        Booking booking = new Booking("BKG-TEST-100", testCustomer, "CONFIRMED", BigDecimal.valueOf(400.00), "cust@test.com", "99999", "idem-100");
        booking = bookingRepository.save(booking);

        BookingLeg leg = new BookingLeg(booking, morningTrip, 1, delhi, agra, 0, 1, BigDecimal.valueOf(400.00));
        booking.addLeg(leg);

        SeatAllocation allocation = new SeatAllocation(leg, bookedSeat, 0, 1, "John Doe", 30, "MALE", "CONFIRMED");
        leg.addAllocation(allocation);
        bookingRepository.save(booking);

        // 1. Search Delhi -> Agra [0, 1): Overlaps with allocation!
        // Capacity was 20; available must now be 19!
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", agra.getId().toString())
                        .param("travelDate", "2026-11-01")
                        .param("departureFrom", "08:00:00")
                        .param("departureTo", "14:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].legs[0].availableSeats").value(19));

        // 2. Search Agra -> Lucknow [1, 2): DISJOINT interval!
        // Seat 1A is NOT booked on [1, 2); available must be full capacity 20!
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", agra.getId().toString())
                        .param("destinationStationId", lucknow.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].legs[0].availableSeats").value(20));

        // 3. Search Delhi -> Lucknow [0, 2): Overlaps with [0, 1)!
        // Available must be 19!
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", lucknow.getId().toString())
                        .param("travelDate", "2026-11-01")
                        .param("departureFrom", "08:00:00")
                        .param("departureTo", "14:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].legs[0].availableSeats").value(19));
    }

    @Test
    @DisplayName("R05: Visual seat map endpoint /api/v1/trips/{id}/seats shows individual seat availability")
    void testGetTripSeatsLayoutAndAvailability() throws Exception {
        List<TripSeat> tripSeats = tripSeatRepository.findByTripId(morningTrip.getId());
        TripSeat bookedSeat = tripSeats.get(0); // seat 1A

        // Book seat 1A on [0, 1)
        Booking booking = new Booking("BKG-SEAT-MAP", testCustomer, "CONFIRMED", BigDecimal.valueOf(400.00), "c@t.com", "999", "idem-map");
        BookingLeg leg = new BookingLeg(booking, morningTrip, 1, delhi, agra, 0, 1, BigDecimal.valueOf(400.00));
        booking.addLeg(leg);
        leg.addAllocation(new SeatAllocation(leg, bookedSeat, 0, 1, "Alice", 28, "FEMALE", "CONFIRMED"));
        bookingRepository.save(booking);

        // Query seat map for [0, 1): seat 1A must have available = false; other 19 must have available = true
        mockMvc.perform(get("/api/v1/trips/" + morningTrip.getId() + "/seats")
                        .param("boardIndex", "0")
                        .param("alightIndex", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCapacity").value(20))
                .andExpect(jsonPath("$.availableSeatsCount").value(19))
                .andExpect(jsonPath("$.seats[0].seatNumber").value("1A"))
                .andExpect(jsonPath("$.seats[0].available").value(false))
                .andExpect(jsonPath("$.seats[1].available").value(true));

        // Query seat map for disjoint interval [1, 2): seat 1A MUST be available = true!
        mockMvc.perform(get("/api/v1/trips/" + morningTrip.getId() + "/seats")
                        .param("boardIndex", "1")
                        .param("alightIndex", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeatsCount").value(20))
                .andExpect(jsonPath("$.seats[0].available").value(true));
    }

    @Test
    @DisplayName("R05: Validation: Origin == Destination yields 400 Bad Request ProblemDetail")
    void testIdenticalOriginDestinationRejected() throws Exception {
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", delhi.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("R05: No direct routes returns journeyType NONE with empty options")
    void testNoDirectRoutesFound() throws Exception {
        // Delhi to Jaipur has no scheduled trip in setup
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", jaipur.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("NONE"))
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.options").isEmpty());
    }
}
