package com.busbooking.route;

import com.busbooking.bus.Bus;
import com.busbooking.bus.BusRepository;
import com.busbooking.category.BusCategory;
import com.busbooking.category.CategoryRepository;
import com.busbooking.route.dto.CreateRouteRequest;
import com.busbooking.route.dto.CreateRouteStopRequest;
import com.busbooking.route.dto.UpdateRouteRequest;
import com.busbooking.station.Station;
import com.busbooking.station.StationRepository;
import com.busbooking.trip.Trip;
import com.busbooking.trip.TripRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private ObjectMapper objectMapper;

    private Station delhi;
    private Station agra;
    private Station lucknow;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        delhi = stationRepository.save(new Station("NDLS", "New Delhi", "New Delhi", "Delhi", "Asia/Kolkata"));
        agra = stationRepository.save(new Station("AGR", "Agra Cantt", "Agra", "UP", "Asia/Kolkata"));
        lucknow = stationRepository.save(new Station("LKO", "Lucknow Alambagh", "Lucknow", "UP", "Asia/Kolkata"));
    }

    @Test
    @DisplayName("R03: Route creation with ordered stops (Delhi -> Agra -> Lucknow) succeeds")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testCreateRouteWithOrderedStops() throws Exception {
        List<CreateRouteStopRequest> stops = List.of(
                new CreateRouteStopRequest(delhi.getId(), 0, BigDecimal.ZERO, 0),
                new CreateRouteStopRequest(agra.getId(), 1, BigDecimal.valueOf(200.0), 210),
                new CreateRouteStopRequest(lucknow.getId(), 2, BigDecimal.valueOf(280.0), 270)
        );

        CreateRouteRequest request = new CreateRouteRequest(
                "RT-DEL-LKO",
                "New Delhi to Lucknow via Agra",
                delhi.getId(),
                lucknow.getId(),
                BigDecimal.valueOf(480.0),
                stops
        );

        mockMvc.perform(post("/api/v1/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeCode").value("RT-DEL-LKO"))
                .andExpect(jsonPath("$.name").value("New Delhi to Lucknow via Agra"))
                .andExpect(jsonPath("$.stops.length()").value(3))
                .andExpect(jsonPath("$.stops[0].stationCode").value("NDLS"))
                .andExpect(jsonPath("$.stops[0].stopOrder").value(0))
                .andExpect(jsonPath("$.stops[1].stationCode").value("AGR"))
                .andExpect(jsonPath("$.stops[1].stopOrder").value(1))
                .andExpect(jsonPath("$.stops[2].stationCode").value("LKO"))
                .andExpect(jsonPath("$.stops[2].stopOrder").value(2));

        Route saved = routeRepository.findByRouteCode("RT-DEL-LKO").orElseThrow();
        List<RouteStop> savedStops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(saved.getId());
        assertEquals(3, savedStops.size());
        assertEquals(0, savedStops.get(0).getStopOrder());
        assertEquals(delhi.getId(), savedStops.get(0).getStation().getId());
        assertEquals(1, savedStops.get(1).getStopOrder());
        assertEquals(agra.getId(), savedStops.get(1).getStation().getId());
        assertEquals(2, savedStops.get(2).getStopOrder());
        assertEquals(lucknow.getId(), savedStops.get(2).getStation().getId());
    }

    @Test
    @DisplayName("R03: Identical origin and destination terminals yields 400 Bad Request ProblemDetail")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testIdenticalTerminalsRejected() throws Exception {
        List<CreateRouteStopRequest> stops = List.of(
                new CreateRouteStopRequest(delhi.getId(), 0, BigDecimal.ZERO, 0),
                new CreateRouteStopRequest(delhi.getId(), 1, BigDecimal.valueOf(100.0), 60)
        );

        CreateRouteRequest request = new CreateRouteRequest(
                "RT-LOOP",
                "Loop Route",
                delhi.getId(),
                delhi.getId(),
                BigDecimal.valueOf(100.0),
                stops
        );

        mockMvc.perform(post("/api/v1/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("R03: Repeated station in route stops yields 400 Bad Request ProblemDetail")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testRepeatedStationInStopsRejected() throws Exception {
        List<CreateRouteStopRequest> stops = List.of(
                new CreateRouteStopRequest(delhi.getId(), 0, BigDecimal.ZERO, 0),
                new CreateRouteStopRequest(agra.getId(), 1, BigDecimal.valueOf(200.0), 200),
                new CreateRouteStopRequest(delhi.getId(), 2, BigDecimal.valueOf(200.0), 200),
                new CreateRouteStopRequest(lucknow.getId(), 3, BigDecimal.valueOf(280.0), 280)
        );

        CreateRouteRequest request = new CreateRouteRequest(
                "RT-REPEAT",
                "Repeat Station Route",
                delhi.getId(),
                lucknow.getId(),
                BigDecimal.valueOf(680.0),
                stops
        );

        mockMvc.perform(post("/api/v1/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("R03: Non-sequential stop orders (gap or wrong order) yields 400 Bad Request")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testNonSequentialStopOrdersRejected() throws Exception {
        // Stop orders 0 and 2 (gap: missing 1)
        List<CreateRouteStopRequest> stops = List.of(
                new CreateRouteStopRequest(delhi.getId(), 0, BigDecimal.ZERO, 0),
                new CreateRouteStopRequest(lucknow.getId(), 2, BigDecimal.valueOf(480.0), 500)
        );

        CreateRouteRequest request = new CreateRouteRequest(
                "RT-GAP",
                "Gap Route",
                delhi.getId(),
                lucknow.getId(),
                BigDecimal.valueOf(480.0),
                stops
        );

        mockMvc.perform(post("/api/v1/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("R03: First stop not matching origin yields 400 Bad Request")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testFirstStopNotMatchingOriginRejected() throws Exception {
        List<CreateRouteStopRequest> stops = List.of(
                new CreateRouteStopRequest(agra.getId(), 0, BigDecimal.ZERO, 0),
                new CreateRouteStopRequest(lucknow.getId(), 1, BigDecimal.valueOf(280.0), 270)
        );

        CreateRouteRequest request = new CreateRouteRequest(
                "RT-MISMATCH",
                "Mismatch Origin Route",
                delhi.getId(),
                lucknow.getId(),
                BigDecimal.valueOf(280.0),
                stops
        );

        mockMvc.perform(post("/api/v1/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("R03: Reference-safe delete: Route with scheduled trips yields 409 and is soft-deactivated")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testReferencedRouteCannotBeDestructivelyRemoved() throws Exception {
        Route route = routeRepository.save(new Route("RT-TRIP-REF", "Delhi-Lucknow", delhi, lucknow, BigDecimal.valueOf(480)));
        BusCategory cat = categoryRepository.save(new BusCategory("CAT-1", "Standard", "Desc"));
        Bus bus = busRepository.save(new Bus("DL-01-REF-2", "Tata", "Bus", cat, 30));

        Trip trip = new Trip(
                "TRIP-ROUTE-REF",
                route,
                bus,
                LocalDate.now().plusDays(2),
                "SCHEDULED",
                Instant.now().plusSeconds(172800),
                Instant.now().plusSeconds(183600)
        );
        tripRepository.save(trip);

        mockMvc.perform(delete("/api/v1/admin/routes/" + route.getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        Route afterAttempt = routeRepository.findById(route.getId()).orElseThrow();
        assertFalse(afterAttempt.isActive(), "Referenced route must be marked inactive");
    }

    @Test
    @DisplayName("R03: Unreferenced route can be deleted cleanly along with stops")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testDeleteUnreferencedRouteSucceeds() throws Exception {
        List<CreateRouteStopRequest> stops = List.of(
                new CreateRouteStopRequest(delhi.getId(), 0, BigDecimal.ZERO, 0),
                new CreateRouteStopRequest(agra.getId(), 1, BigDecimal.valueOf(200.0), 200)
        );

        CreateRouteRequest request = new CreateRouteRequest(
                "RT-DEL-AGR-DEL",
                "Delhi to Agra",
                delhi.getId(),
                agra.getId(),
                BigDecimal.valueOf(200.0),
                stops
        );

        String responseStr = mockMvc.perform(post("/api/v1/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long routeId = objectMapper.readTree(responseStr).get("id").asLong();
        assertTrue(routeRepository.existsById(routeId));
        assertEquals(2, routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId).size());

        mockMvc.perform(delete("/api/v1/admin/routes/" + routeId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(routeRepository.existsById(routeId));
        assertEquals(0, routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId).size());
    }

    @Test
    @DisplayName("R03: Customer cannot access admin route endpoints (403 Forbidden)")
    @WithMockUser(username = "customer@demo.local", authorities = {"ROLE_CUSTOMER"})
    void testCustomerCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/routes/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
