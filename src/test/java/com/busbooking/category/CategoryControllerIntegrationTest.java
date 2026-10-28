package com.busbooking.category;

import com.busbooking.auth.Role;
import com.busbooking.auth.User;
import com.busbooking.auth.UserRepository;
import com.busbooking.bus.Bus;
import com.busbooking.bus.BusRepository;
import com.busbooking.category.dto.CreateCategoryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();
    }

    @Test
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testCreateReadUpdateDeactivateCategory() throws Exception {
        // 1. Create Category (R01 - Create)
        CreateCategoryRequest createReq = new CreateCategoryRequest(
                "LUXURY_AC",
                "Luxury AC Coach",
                "High-end luxury bus with air conditioning"
        );

        mockMvc.perform(post("/api/v1/admin/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LUXURY_AC"))
                .andExpect(jsonPath("$.name").value("Luxury AC Coach"))
                .andExpect(jsonPath("$.isActive").value(true));

        BusCategory saved = categoryRepository.findByCode("LUXURY_AC").orElseThrow();

        // 2. Read Category by ID (R01 - Read)
        mockMvc.perform(get("/api/v1/admin/categories/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.code").value("LUXURY_AC"));

        // 3. Update Category (R01 - Update)
        CreateCategoryRequest updateReq = new CreateCategoryRequest(
                "LUXURY_AC_V2",
                "Luxury AC Coach Platinum",
                "Updated description"
        );

        mockMvc.perform(put("/api/v1/admin/categories/" + saved.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LUXURY_AC_V2"))
                .andExpect(jsonPath("$.name").value("Luxury AC Coach Platinum"));

        // 4. Deactivate Category (R01 - Deactivate)
        mockMvc.perform(patch("/api/v1/admin/categories/" + saved.getId() + "/deactivate")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // Public /api/v1/categories should not include inactive categories
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testDeleteUnreferencedCategorySucceeds() throws Exception {
        BusCategory category = new BusCategory("NON_AC_TEST", "Standard Non-AC", "Test");
        BusCategory saved = categoryRepository.save(category);

        mockMvc.perform(delete("/api/v1/admin/categories/" + saved.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(categoryRepository.existsById(saved.getId()), "Category must be removed from DB");
    }

    @Test
    @WithMockUser(username = "admin@busbooking.local", authorities = {"ROLE_ADMIN"})
    void testReferencedCategoryCannotBeDestructivelyRemoved() throws Exception {
        BusCategory category = new BusCategory("REFERENCED_CAT", "Referenced Category", "Has buses");
        BusCategory savedCat = categoryRepository.save(category);

        Bus bus = new Bus("TEST-BUS-01", "Volvo", "9600", savedCat, 40);
        busRepository.save(bus);

        // Attempt DELETE on referenced category
        mockMvc.perform(delete("/api/v1/admin/categories/" + savedCat.getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        // Verify category was not hard-deleted but deactivated (ADR-06 reference-safe)
        BusCategory afterAttempt = categoryRepository.findById(savedCat.getId()).orElseThrow();
        assertFalse(afterAttempt.isActive(), "Referenced category must be marked inactive");
    }

    @Test
    void testUnauthenticatedUserCannotModifyCategories() throws Exception {
        CreateCategoryRequest createReq = new CreateCategoryRequest("TEST_CAT", "Test Category", "Desc");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@demo.local", authorities = {"ROLE_CUSTOMER"})
    void testCustomerCannotModifyCategories() throws Exception {
        CreateCategoryRequest createReq = new CreateCategoryRequest("TEST_CAT", "Test Category", "Desc");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());
    }
}
