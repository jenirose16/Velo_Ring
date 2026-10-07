package com.veloring.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroundTruthRecord {
    private String experimentId;
    private String scenarioId;
    private String scenarioType;
    private String transactionId;
    private String role;
    private boolean injectedPattern;
}
