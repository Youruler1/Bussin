package com.busbooking.bus;

import com.busbooking.bus.dto.CreateBusRequest;
import com.busbooking.bus.dto.UpdateBusRequest;
import com.busbooking.category.BusCategory;
import com.busbooking.category.CategoryRepository;
import com.busbooking.route.Route;
import com.busbooking.route.RouteRepository;
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
class BusControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private ObjectMapper objectMapper;

    private BusCategory seaterCategory;
    private BusCategory sleeperCategory;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        seaterCategory = categoryRepository.save(new BusCategory("SEATER_STD", "Standard Seater", "Regular push-back seats"));
        sleeperCategory = categoryRepository.save(new BusCategory("SLEEPER_AC", "AC Sleeper Coach", "Luxury sleeper berths"));
    }

    @Test
    @DisplayName("R02: Bus creation auto-generates seats matching total capacity and category layout")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testCreateBusAutoGeneratesSeats() throws Exception {
        CreateBusRequest request = new CreateBusRequest(
                "DL-01-AB-1234",
                "Volvo",
                "9600",
                seaterCategory.getId(),
                30
        );

        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationNumber").value("DL-01-AB-1234"))
                .andExpect(jsonPath("$.brand").value("Volvo"))
                .andExpect(jsonPath("$.model").value("9600"))
                .andExpect(jsonPath("$.categoryId").value(seaterCategory.getId()))
                .andExpect(jsonPath("$.totalCapacity").value(30))
                .andExpect(jsonPath("$.seats.length()").value(30))
                .andExpect(jsonPath("$.seats[0].seatNumber").value("1A"))
                .andExpect(jsonPath("$.seats[0].seatType").value("SEATER"));

        Bus bus = busRepository.findByRegistrationNumber("DL-01-AB-1234").orElseThrow();
        assertEquals(30, seatRepository.countByBusId(bus.getId()));

        List<Seat> seats = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus.getId());
        assertEquals(30, seats.size());
        assertEquals("1A", seats.get(0).getSeatNumber());
        assertEquals(1, seats.get(0).getRowNumber());
        assertEquals(1, seats.get(0).getColumnNumber());
        assertEquals("SEATER", seats.get(0).getSeatType());
        assertEquals("8B", seats.get(29).getSeatNumber());
    }

    @Test
    @DisplayName("R02: Sleeper bus generates sleeper berths with S prefix and 2 columns")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testCreateSleeperBusGeneratesBerths() throws Exception {
        CreateBusRequest request = new CreateBusRequest(
                "KA-01-ZZ-9999",
                "Scania",
                "Metrolink Sleeper",
                sleeperCategory.getId(),
                20
        );

        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats.length()").value(20))
                .andExpect(jsonPath("$.seats[0].seatNumber").value("S1A"))
                .andExpect(jsonPath("$.seats[0].seatType").value("SLEEPER"))
                .andExpect(jsonPath("$.seats[19].seatNumber").value("S10B"));

        Bus bus = busRepository.findByRegistrationNumber("KA-01-ZZ-9999").orElseThrow();
        assertEquals(20, seatRepository.countByBusId(bus.getId()));
    }

    @Test
    @DisplayName("R02: Duplicate bus registration number yields 409 Conflict ProblemDetail")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testDuplicateRegistrationReturnsConflict() throws Exception {
        CreateBusRequest req1 = new CreateBusRequest("UP-32-AA-1111", "Tata", "Starbus", seaterCategory.getId(), 25);
        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        CreateBusRequest reqDuplicate = new CreateBusRequest("UP-32-AA-1111", "Ashok Leyland", "Viking", seaterCategory.getId(), 30);
        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDuplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("R02: Invalid capacity (< 1 or > 80) yields 400 Bad Request ProblemDetail")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testInvalidCapacityReturnsBadRequest() throws Exception {
        CreateBusRequest zeroCapacity = new CreateBusRequest("RJ-14-CC-0000", "Tata", "City", seaterCategory.getId(), 0);
        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroCapacity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        CreateBusRequest excessiveCapacity = new CreateBusRequest("RJ-14-CC-9999", "Tata", "City", seaterCategory.getId(), 85);
        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveCapacity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("R02: Nonexistent category ID yields 404 ProblemDetail")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testNonexistentCategoryReturnsNotFound() throws Exception {
        CreateBusRequest request = new CreateBusRequest("MH-02-EE-5555", "Eicher", "Intercity", 9999L, 30);
        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("R02: Reference-safe delete: Bus with scheduled trips yields 409 and is soft-deactivated")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testReferencedBusCannotBeDestructivelyRemoved() throws Exception {
        Station st1 = stationRepository.save(new Station("DEL", "Delhi", "Delhi", "Delhi", "Asia/Kolkata"));
        Station st2 = stationRepository.save(new Station("AGR", "Agra", "Agra", "UP", "Asia/Kolkata"));
        Route route = routeRepository.save(new Route("RT-100", "Delhi-Agra", st1, st2, BigDecimal.valueOf(200)));

        Bus bus = busRepository.save(new Bus("DL-01-REF-01", "Volvo", "9600", seaterCategory, 30));

        Trip trip = new Trip(
                "TRIP-TEST-001",
                route,
                bus,
                LocalDate.now().plusDays(1),
                "SCHEDULED",
                Instant.now().plusSeconds(86400),
                Instant.now().plusSeconds(97200)
        );
        tripRepository.save(trip);

        // Attempt deletion of bus referenced by scheduled trip
        mockMvc.perform(delete("/api/v1/admin/buses/" + bus.getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        // Verify bus is still in DB but marked inactive
        Bus afterAttempt = busRepository.findById(bus.getId()).orElseThrow();
        assertFalse(afterAttempt.isActive(), "Referenced bus must be marked inactive");
    }

    @Test
    @DisplayName("R02: Unreferenced bus can be deleted and associated seats are removed")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testDeleteUnreferencedBusSucceeds() throws Exception {
        CreateBusRequest request = new CreateBusRequest("HR-26-DEL-1", "Tata", "Starbus", seaterCategory.getId(), 20);
        String responseStr = mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long busId = objectMapper.readTree(responseStr).get("id").asLong();
        assertTrue(busRepository.existsById(busId));
        assertEquals(20, seatRepository.countByBusId(busId));

        // Delete unreferenced bus
        mockMvc.perform(delete("/api/v1/admin/buses/" + busId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(busRepository.existsById(busId));
        assertEquals(0, seatRepository.countByBusId(busId));
    }

    @Test
    @DisplayName("R02: Security: Customer cannot create or delete buses (403 Forbidden)")
    @WithMockUser(username = "customer@demo.local", authorities = {"ROLE_CUSTOMER"})
    void testCustomerCannotAccessAdminBusEndpoints() throws Exception {
        CreateBusRequest request = new CreateBusRequest("DL-01-CUST-1", "Volvo", "9600", seaterCategory.getId(), 30);
        mockMvc.perform(post("/api/v1/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/buses/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("R02: Public endpoint /api/v1/buses lists active buses without authentication")
    void testPublicCanListActiveBuses() throws Exception {
        Bus activeBus = busRepository.save(new Bus("PUB-ACTIVE", "Tata", "Bus", seaterCategory, 20));
        Bus inactiveBus = new Bus("PUB-INACTIVE", "Tata", "Bus", seaterCategory, 20);
        inactiveBus.setActive(false);
        busRepository.save(inactiveBus);

        mockMvc.perform(get("/api/v1/buses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].registrationNumber").value("PUB-ACTIVE"));
    }
}
