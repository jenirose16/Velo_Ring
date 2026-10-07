package com.veloring.generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "veloring.generator")
public class GeneratorProperties {
    private long seed = 42L;
    private int tps = 50;
    private int durationSeconds = 60;
    private String startTime = "2026-01-01T00:00:00Z";
    private double accelerationFactor = 1.0;
    private int accountsPoolSize = 1000;
    private String groundTruthFile = "ground-truth.jsonl";
    private String experimentId = "EXP-001";
    private Map<String, ScenarioProps> scenarios;

    @Data
    public static class ScenarioProps {
        private int weight;
    }
}
