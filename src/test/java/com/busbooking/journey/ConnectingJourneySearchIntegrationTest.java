package com.busbooking.journey;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class ConnectingJourneySearchIntegrationTest {

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
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    private Station delhi;
    private Station agra;
    private Station lucknow;
    private Station kanpur;
    private Station chandigarh;

    private Route routeDelhiToLucknow;
    private Route routeLucknowToKanpur;
    private Bus bus1;
    private Bus bus2;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        delhi = stationRepository.save(new Station("NDLS", "New Delhi", "New Delhi", "Delhi", "Asia/Kolkata"));
        agra = stationRepository.save(new Station("AGR", "Agra Cantt", "Agra", "UP", "Asia/Kolkata"));
        lucknow = stationRepository.save(new Station("LKO", "Lucknow Alambagh", "Lucknow", "UP", "Asia/Kolkata"));
        kanpur = stationRepository.save(new Station("KNP", "Kanpur Central", "Kanpur", "UP", "Asia/Kolkata"));
        chandigarh = stationRepository.save(new Station("CDG", "Chandigarh", "Chandigarh", "Punjab", "Asia/Kolkata"));

        // Route 1: Delhi -> Agra -> Lucknow (480 km)
        Route r1 = new Route("RT-DEL-LKO", "Delhi to Lucknow via Agra", delhi, lucknow, BigDecimal.valueOf(480.0));
        r1.addStop(new RouteStop(r1, delhi, 0, BigDecimal.ZERO, 0));
        r1.addStop(new RouteStop(r1, agra, 1, BigDecimal.valueOf(200.0), 210));
        r1.addStop(new RouteStop(r1, lucknow, 2, BigDecimal.valueOf(280.0), 270));
        routeDelhiToLucknow = routeRepository.save(r1);

        // Route 2: Lucknow -> Kanpur (85 km) - No direct route between Delhi and Kanpur!
        Route r2 = new Route("RT-LKO-KNP", "Lucknow to Kanpur Express", lucknow, kanpur, BigDecimal.valueOf(85.0));
        r2.addStop(new RouteStop(r2, lucknow, 0, BigDecimal.ZERO, 0));
        r2.addStop(new RouteStop(r2, kanpur, 1, BigDecimal.valueOf(85.0), 90));
        routeLucknowToKanpur = routeRepository.save(r2);

        BusCategory category = categoryRepository.save(new BusCategory("AC_VOLVO", "Volvo Multi-Axle", "AC Coach"));
        bus1 = busRepository.save(new Bus("DL-01-V-100", "Volvo", "9600", category, 20));
        bus2 = busRepository.save(new Bus("UP-32-T-200", "Tata", "Starbus", category, 30));

        List<Seat> seats1 = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            seats1.add(new Seat(bus1, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seatRepository.saveAll(seats1);

        List<Seat> seats2 = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            seats2.add(new Seat(bus2, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seatRepository.saveAll(seats2);
    }

    private Trip createTrip(String tripNum, Route route, Bus bus, LocalDate date,
                            Instant dep0, Instant arrEnd, BigDecimal fullFare) {
        Trip trip = new Trip(tripNum, route, bus, date, "SCHEDULED", dep0, arrEnd);

        List<RouteStop> rStops = route.getStops();
        for (int i = 0; i < rStops.size(); i++) {
            RouteStop rs = rStops.get(i);
            Instant arr = (i == 0) ? null : dep0.plusSeconds(rs.getTypicalDurationMinutes() * 60L);
            Instant dep = (i == rStops.size() - 1) ? null : dep0;
            trip.addStop(new TripStop(trip, rs.getStation(), rs.getStopOrder(), arr, dep, rs.getDistanceFromPrevKm()));
        }

        // Add segment fares
        for (int i = 0; i < rStops.size(); i++) {
            for (int j = i + 1; j < rStops.size(); j++) {
                trip.addFare(new TripStopFare(trip, i, j, fullFare));
            }
        }

        List<Seat> seats = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus.getId());
        for (Seat seat : seats) {
            trip.addSeat(new TripSeat(trip, seat));
        }
        return tripRepository.save(trip);
    }

    @Test
    @DisplayName("R09: When no direct trip exists, returns time-feasible 1-transfer connection (Delhi -> Lucknow -> Kanpur)")
    void testFeasibleConnectingJourneyFound() throws Exception {
        // Leg 1: Delhi -> Lucknow
        // Departs Delhi: 2026-11-01T06:00:00Z (11:30 IST)
        // Arrives Lucknow: 2026-11-01T14:00:00Z (19:30 IST)
        Trip leg1Trip = new Trip("TRIP-DEL-LKO-LEG1", routeDelhiToLucknow, bus1,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T14:00:00Z"));
        leg1Trip.addStop(new TripStop(leg1Trip, delhi, 0, null, Instant.parse("2026-11-01T06:00:00Z"), BigDecimal.ZERO));
        leg1Trip.addStop(new TripStop(leg1Trip, agra, 1, Instant.parse("2026-11-01T09:30:00Z"), Instant.parse("2026-11-01T09:45:00Z"), BigDecimal.valueOf(200.0)));
        leg1Trip.addStop(new TripStop(leg1Trip, lucknow, 2, Instant.parse("2026-11-01T14:00:00Z"), null, BigDecimal.valueOf(480.0)));
        leg1Trip.addFare(new TripStopFare(leg1Trip, 0, 2, BigDecimal.valueOf(900.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId())) {
            leg1Trip.addSeat(new TripSeat(leg1Trip, s));
        }
        tripRepository.save(leg1Trip);

        // Leg 2: Lucknow -> Kanpur
        // Departs Lucknow: 2026-11-01T15:30:00Z (Layover = 90 mins, valid: 30m <= 90m <= 480m)
        // Arrives Kanpur: 2026-11-01T17:00:00Z
        Trip leg2Trip = new Trip("TRIP-LKO-KNP-LEG2", routeLucknowToKanpur, bus2,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T15:30:00Z"),
                Instant.parse("2026-11-01T17:00:00Z"));
        leg2Trip.addStop(new TripStop(leg2Trip, lucknow, 0, null, Instant.parse("2026-11-01T15:30:00Z"), BigDecimal.ZERO));
        leg2Trip.addStop(new TripStop(leg2Trip, kanpur, 1, Instant.parse("2026-11-01T17:00:00Z"), null, BigDecimal.valueOf(85.0)));
        leg2Trip.addFare(new TripStopFare(leg2Trip, 0, 1, BigDecimal.valueOf(200.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus2.getId())) {
            leg2Trip.addSeat(new TripSeat(leg2Trip, s));
        }
        tripRepository.save(leg2Trip);

        // Search Delhi -> Kanpur (No direct route exists)
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", kanpur.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("CONNECTING"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.options[0].itineraryType").value("CONNECTING"))
                // Total duration: 06:00 to 17:00 = 11 hours = 660 mins
                .andExpect(jsonPath("$.options[0].totalDurationMinutes").value(660))
                // Total fare: 900 + 200 = 1100.00
                .andExpect(jsonPath("$.options[0].totalFareInr").value(1100.00))
                // 2 legs
                .andExpect(jsonPath("$.options[0].legs.length()").value(2))
                .andExpect(jsonPath("$.options[0].legs[0].tripNumber").value("TRIP-DEL-LKO-LEG1"))
                .andExpect(jsonPath("$.options[0].legs[0].originStationName").value("New Delhi"))
                .andExpect(jsonPath("$.options[0].legs[0].destinationStationName").value("Lucknow Alambagh"))
                .andExpect(jsonPath("$.options[0].legs[0].segmentFareInr").value(900.00))
                .andExpect(jsonPath("$.options[0].legs[1].tripNumber").value("TRIP-LKO-KNP-LEG2"))
                .andExpect(jsonPath("$.options[0].legs[1].originStationName").value("Lucknow Alambagh"))
                .andExpect(jsonPath("$.options[0].legs[1].destinationStationName").value("Kanpur Central"))
                .andExpect(jsonPath("$.options[0].legs[1].segmentFareInr").value(200.00));
    }

    @Test
    @DisplayName("R09: Rejects impossible connections where layover is less than 30 minutes (tight connection)")
    void testRejectsLayoverLessThan30Minutes() throws Exception {
        // Leg 1 arrives Lucknow at 14:00 UTC
        Trip leg1Trip = new Trip("TRIP-LEG1-TIGHT", routeDelhiToLucknow, bus1,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T14:00:00Z"));
        leg1Trip.addStop(new TripStop(leg1Trip, delhi, 0, null, Instant.parse("2026-11-01T06:00:00Z"), BigDecimal.ZERO));
        leg1Trip.addStop(new TripStop(leg1Trip, lucknow, 2, Instant.parse("2026-11-01T14:00:00Z"), null, BigDecimal.valueOf(480.0)));
        leg1Trip.addFare(new TripStopFare(leg1Trip, 0, 2, BigDecimal.valueOf(900.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId())) {
            leg1Trip.addSeat(new TripSeat(leg1Trip, s));
        }
        tripRepository.save(leg1Trip);

        // Leg 2 departs Lucknow at 14:15 UTC (ONLY 15 minutes layover! Must be rejected)
        Trip tightLeg2 = new Trip("TRIP-LEG2-TIGHT", routeLucknowToKanpur, bus2,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T14:15:00Z"),
                Instant.parse("2026-11-01T15:45:00Z"));
        tightLeg2.addStop(new TripStop(tightLeg2, lucknow, 0, null, Instant.parse("2026-11-01T14:15:00Z"), BigDecimal.ZERO));
        tightLeg2.addStop(new TripStop(tightLeg2, kanpur, 1, Instant.parse("2026-11-01T15:45:00Z"), null, BigDecimal.valueOf(85.0)));
        tightLeg2.addFare(new TripStopFare(tightLeg2, 0, 1, BigDecimal.valueOf(200.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus2.getId())) {
            tightLeg2.addSeat(new TripSeat(tightLeg2, s));
        }
        tripRepository.save(tightLeg2);

        // Search Delhi -> Kanpur: tight connection must not be returned
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", kanpur.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("NONE"))
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.options").isEmpty());
    }

    @Test
    @DisplayName("R09: Rejects impossible connections where layover exceeds 8 hours (480 minutes)")
    void testRejectsLayoverExceeding8Hours() throws Exception {
        // Leg 1 arrives Lucknow at 14:00 UTC
        Trip leg1Trip = new Trip("TRIP-LEG1-LONG", routeDelhiToLucknow, bus1,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T14:00:00Z"));
        leg1Trip.addStop(new TripStop(leg1Trip, delhi, 0, null, Instant.parse("2026-11-01T06:00:00Z"), BigDecimal.ZERO));
        leg1Trip.addStop(new TripStop(leg1Trip, lucknow, 2, Instant.parse("2026-11-01T14:00:00Z"), null, BigDecimal.valueOf(480.0)));
        leg1Trip.addFare(new TripStopFare(leg1Trip, 0, 2, BigDecimal.valueOf(900.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId())) {
            leg1Trip.addSeat(new TripSeat(leg1Trip, s));
        }
        tripRepository.save(leg1Trip);

        // Leg 2 departs Lucknow at 22:30 UTC (8 hours 30 mins = 510 mins layover! Must be rejected)
        Trip longWaitLeg2 = new Trip("TRIP-LEG2-LONG", routeLucknowToKanpur, bus2,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T22:30:00Z"),
                Instant.parse("2026-11-02T00:00:00Z"));
        longWaitLeg2.addStop(new TripStop(longWaitLeg2, lucknow, 0, null, Instant.parse("2026-11-01T22:30:00Z"), BigDecimal.ZERO));
        longWaitLeg2.addStop(new TripStop(longWaitLeg2, kanpur, 1, Instant.parse("2026-11-02T00:00:00Z"), null, BigDecimal.valueOf(85.0)));
        longWaitLeg2.addFare(new TripStopFare(longWaitLeg2, 0, 1, BigDecimal.valueOf(200.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus2.getId())) {
            longWaitLeg2.addSeat(new TripSeat(longWaitLeg2, s));
        }
        tripRepository.save(longWaitLeg2);

        // Search Delhi -> Kanpur: excessive layover must not be returned
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", kanpur.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("NONE"))
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.options").isEmpty());
    }

    @Test
    @DisplayName("R09: Cross-midnight connection supported seamlessly (Nov 1 23:30 arrival -> Nov 2 01:00 departure)")
    void testCrossMidnightConnectionSupported() throws Exception {
        // Leg 1 arrives Lucknow at 23:30 UTC on Nov 1
        Trip leg1Trip = new Trip("TRIP-MIDNIGHT-1", routeDelhiToLucknow, bus1,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T15:30:00Z"),
                Instant.parse("2026-11-01T23:30:00Z"));
        leg1Trip.addStop(new TripStop(leg1Trip, delhi, 0, null, Instant.parse("2026-11-01T15:30:00Z"), BigDecimal.ZERO));
        leg1Trip.addStop(new TripStop(leg1Trip, lucknow, 2, Instant.parse("2026-11-01T23:30:00Z"), null, BigDecimal.valueOf(480.0)));
        leg1Trip.addFare(new TripStopFare(leg1Trip, 0, 2, BigDecimal.valueOf(950.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId())) {
            leg1Trip.addSeat(new TripSeat(leg1Trip, s));
        }
        tripRepository.save(leg1Trip);

        // Leg 2 departs Lucknow at 01:00 UTC on Nov 2 (90 minutes layover across midnight boundary!)
        Trip leg2Trip = new Trip("TRIP-MIDNIGHT-2", routeLucknowToKanpur, bus2,
                LocalDate.of(2026, 11, 2), "SCHEDULED",
                Instant.parse("2026-11-02T01:00:00Z"),
                Instant.parse("2026-11-02T02:30:00Z"));
        leg2Trip.addStop(new TripStop(leg2Trip, lucknow, 0, null, Instant.parse("2026-11-02T01:00:00Z"), BigDecimal.ZERO));
        leg2Trip.addStop(new TripStop(leg2Trip, kanpur, 1, Instant.parse("2026-11-02T02:30:00Z"), null, BigDecimal.valueOf(85.0)));
        leg2Trip.addFare(new TripStopFare(leg2Trip, 0, 1, BigDecimal.valueOf(220.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus2.getId())) {
            leg2Trip.addSeat(new TripSeat(leg2Trip, s));
        }
        tripRepository.save(leg2Trip);

        // Search Delhi -> Kanpur on Nov 1
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", kanpur.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("CONNECTING"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.options[0].totalDurationMinutes").value(660)) // 15:30 to 02:30 = 11h = 660 mins
                .andExpect(jsonPath("$.options[0].totalFareInr").value(1170.00))
                .andExpect(jsonPath("$.options[0].legs[0].tripNumber").value("TRIP-MIDNIGHT-1"))
                .andExpect(jsonPath("$.options[0].legs[1].tripNumber").value("TRIP-MIDNIGHT-2"));
    }

    @Test
    @DisplayName("R09: When direct service exists, search prioritizes DIRECT and does not return connecting")
    void testDirectJourneyTakesPrecedence() throws Exception {
        // Direct trip: Delhi -> Lucknow
        Trip directTrip = new Trip("TRIP-DIRECT-PRIORITY", routeDelhiToLucknow, bus1,
                LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T14:00:00Z"));
        directTrip.addStop(new TripStop(directTrip, delhi, 0, null, Instant.parse("2026-11-01T06:00:00Z"), BigDecimal.ZERO));
        directTrip.addStop(new TripStop(directTrip, agra, 1, Instant.parse("2026-11-01T09:30:00Z"), Instant.parse("2026-11-01T09:45:00Z"), BigDecimal.valueOf(200.0)));
        directTrip.addStop(new TripStop(directTrip, lucknow, 2, Instant.parse("2026-11-01T14:00:00Z"), null, BigDecimal.valueOf(480.0)));
        directTrip.addFare(new TripStopFare(directTrip, 0, 2, BigDecimal.valueOf(900.00)));
        for (Seat s : seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus1.getId())) {
            directTrip.addSeat(new TripSeat(directTrip, s));
        }
        tripRepository.save(directTrip);

        // Search Delhi -> Lucknow (Direct service exists)
        mockMvc.perform(get("/api/v1/journeys/search")
                        .param("originStationId", delhi.getId().toString())
                        .param("destinationStationId", lucknow.getId().toString())
                        .param("travelDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journeyType").value("DIRECT"))
                .andExpect(jsonPath("$.options[0].itineraryType").value("DIRECT"))
                .andExpect(jsonPath("$.options[0].legs.length()").value(1));
    }
}
