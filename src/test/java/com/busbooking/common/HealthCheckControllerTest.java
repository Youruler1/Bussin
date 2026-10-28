package com.busbooking.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthCheckControllerTest {

    private final HealthCheckController controller = new HealthCheckController();

    @Test
    void healthEndpointReturnsStatusUp() {
        ResponseEntity<Map<String, Object>> response = controller.getHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("bus-ticket-booking-platform", response.getBody().get("service"));
        assertNotNull(response.getBody().get("timestamp"));
    }
}
