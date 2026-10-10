package com.veloring.engine.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0",
        "management.health.neo4j.enabled=false",
        "management.health.kafka.enabled=false",
        "management.health.ping.enabled=true"
})
public class ActuatorConfigTest {

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testMetricsEndpointExposedAndCustomMetricVisible() {
        // Record a dummy metric to ensure it exists in the registry
        meterRegistry.timer("veloring.detect.e2e.delay").record(Duration.ofMillis(150));

        String url = "http://localhost:" + managementPort + "/actuator/metrics";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Metrics endpoint should return 200 OK");
        assertTrue(response.getBody().contains("veloring.detect.e2e.delay"), "Custom metric should be discoverable in the metrics list");
        
        String healthUrl = "http://localhost:" + managementPort + "/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode(), "Health endpoint should return 200 OK");
    }
}
