package com.veloring.engine.api;

import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final Driver neo4jDriver;

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        boolean isNeo4jUp = false;
        try {
            neo4jDriver.verifyConnectivity();
            isNeo4jUp = true;
        } catch (Exception e) {
            // Log and ignore
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "neo4j", isNeo4jUp ? "UP" : "DOWN",
            "service", "veloring-engine"
        ));
    }
}
