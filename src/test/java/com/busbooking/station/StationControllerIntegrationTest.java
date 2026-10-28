package com.busbooking.station;

import com.busbooking.route.Route;
import com.busbooking.route.RouteRepository;
import com.busbooking.station.dto.CreateStationRequest;
import com.busbooking.station.dto.UpdateStationRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class StationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();
    }

    @Test
    @DisplayName("Station CRUD: Create, read, update, deactivate station")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testStationCrudLifecycle() throws Exception {
        CreateStationRequest request = new CreateStationRequest(
                "NDLS",
                "New Delhi Central",
                "New Delhi",
                "Delhi",
                "Asia/Kolkata"
        );

        mockMvc.perform(post("/api/v1/admin/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NDLS"))
                .andExpect(jsonPath("$.name").value("New Delhi Central"))
                .andExpect(jsonPath("$.isActive").value(true));

        Station saved = stationRepository.findByCode("NDLS").orElseThrow();

        // Update
        UpdateStationRequest updateReq = new UpdateStationRequest(
                "New Delhi Inter State Bus Terminal",
                "New Delhi",
                "Delhi",
                "Asia/Kolkata",
                true
        );

        mockMvc.perform(put("/api/v1/admin/stations/" + saved.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Delhi Inter State Bus Terminal"));
    }

    @Test
    @DisplayName("Station duplicate code yields 409 Conflict ProblemDetail")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testDuplicateStationCodeReturnsConflict() throws Exception {
        stationRepository.save(new Station("AGR", "Agra Fort", "Agra", "UP", "Asia/Kolkata"));

        CreateStationRequest duplicateReq = new CreateStationRequest("AGR", "Agra Cantt", "Agra", "UP", "Asia/Kolkata");

        mockMvc.perform(post("/api/v1/admin/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Reference-safe delete: Station referenced by route yields 409 and soft-deactivates")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testReferencedStationCannotBeDestructivelyRemoved() throws Exception {
        Station st1 = stationRepository.save(new Station("JPR", "Jaipur", "Jaipur", "Rajasthan", "Asia/Kolkata"));
        Station st2 = stationRepository.save(new Station("DEL", "Delhi", "Delhi", "Delhi", "Asia/Kolkata"));
        routeRepository.save(new Route("RT-JPR-DEL", "Jaipur to Delhi", st1, st2, BigDecimal.valueOf(270)));

        mockMvc.perform(delete("/api/v1/admin/stations/" + st1.getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        Station afterAttempt = stationRepository.findById(st1.getId()).orElseThrow();
        assertFalse(afterAttempt.isActive(), "Referenced station must be marked inactive");
    }

    @Test
    @DisplayName("Unreferenced station can be deleted cleanly (204 No Content)")
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testDeleteUnreferencedStationSucceeds() throws Exception {
        Station station = stationRepository.save(new Station("CDG", "Chandigarh", "Chandigarh", "Punjab", "Asia/Kolkata"));

        mockMvc.perform(delete("/api/v1/admin/stations/" + station.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(stationRepository.existsById(station.getId()));
    }

    @Test
    @DisplayName("Public can list active stations without auth; customer denied admin modifications")
    @WithMockUser(username = "customer@demo.local", authorities = {"ROLE_CUSTOMER"})
    void testStationRbac() throws Exception {
        stationRepository.save(new Station("BOM", "Mumbai Central", "Mumbai", "Maharashtra", "Asia/Kolkata"));

        // Public list
        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Customer modification denied (403 Forbidden)
        CreateStationRequest request = new CreateStationRequest("BLR", "Bangalore", "Bangalore", "Karnataka", "Asia/Kolkata");
        mockMvc.perform(post("/api/v1/admin/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
