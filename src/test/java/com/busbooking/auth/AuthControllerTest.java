package com.busbooking.auth;

import com.busbooking.auth.dto.LoginRequest;
import com.busbooking.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.busbooking.TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterCustomerSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "john.doe@example.com",
                "Secret@123",
                "John Doe",
                "+919876543210"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmail("john.doe@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("Secret@123", saved.getPasswordHash()), "Password must be BCrypt hashed");
    }

    @Test
    void testRegisterDuplicateEmailReturnsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@example.com",
                "Secret@123",
                "User One",
                "+919876543210"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt duplicate registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void testLoginSuccessCreatesSessionAndAllowsMeAccess() throws Exception {
        User user = new User("alice@example.com", passwordEncoder.encode("AlicePass123"), "Alice Smith", "+919876500000", Role.ROLE_CUSTOMER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("alice@example.com", "AlicePass123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertTrue(session != null, "Session must be created on successful login");

        // Access /api/v1/me using this session
        mockMvc.perform(get("/api/v1/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Smith"));
    }

    @Test
    void testLoginWithBadCredentialsReturnsUnauthorized() throws Exception {
        User user = new User("bob@example.com", passwordEncoder.encode("BobPass123"), "Bob Builder", "+919876500001", Role.ROLE_CUSTOMER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("bob@example.com", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void testCustomerCannotAccessAdminEndpoint() throws Exception {
        User customer = new User("cust@example.com", passwordEncoder.encode("Pass12345"), "Normal Customer", "+919876500002", Role.ROLE_CUSTOMER);
        userRepository.save(customer);

        LoginRequest loginRequest = new LoginRequest("cust@example.com", "Pass12345");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Attempt to access admin endpoint
        mockMvc.perform(get("/api/v1/admin/categories")
                        .session(session))
                .andExpect(status().isForbidden());
    }
}
