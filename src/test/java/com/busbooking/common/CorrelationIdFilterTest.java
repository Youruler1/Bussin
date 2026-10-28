package com.busbooking.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorrelationIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("R13: Auto-generates UUID correlation ID in X-Request-Id when missing")
    void testGeneratesCorrelationIdWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[a-f0-9\\-]{36}")));
    }

    @Test
    @DisplayName("R13: Preserves existing X-Request-Id passed by client")
    void testPreservesExistingCorrelationId() throws Exception {
        String customId = "trace-client-abc-12345";
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", customId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", customId));
    }
}
