package com.veloring.engine.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veloring.engine.detection.Cycle;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class GroundTruthEvaluator {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroundTruthRecord(
            String experimentId,
            String scenarioId,
            String scenarioType,
            String transactionId,
            String role,
            boolean injectedPattern
    ) {}

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CycleMatchResult evaluate(Path groundTruthFile, List<Cycle> detectedCycles) throws IOException {
        List<GroundTruthRecord> records = new ArrayList<>();
        if (groundTruthFile != null && Files.exists(groundTruthFile)) {
            try (BufferedReader reader = Files.newBufferedReader(groundTruthFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        records.add(objectMapper.readValue(line, GroundTruthRecord.class));
                    }
                }
            }
        }
        return evaluateRecords(records, detectedCycles);
    }

    public CycleMatchResult evaluateRecords(List<GroundTruthRecord> records, List<Cycle> detectedCycles) {
        // Group by composite key: experimentId + "|" + scenarioId
        Map<String, List<GroundTruthRecord>> grouped = records.stream()
                .filter(r -> "CYCLE_3".equals(r.scenarioType()))
                .collect(Collectors.groupingBy(r -> r.experimentId() + "|" + r.scenarioId()));

        int ambiguousTargetsCount = 0;
        List<Set<String>> validTargets = new ArrayList<>();
        List<Set<String>> ambiguousTargetTxIdSets = new ArrayList<>();

        for (Map.Entry<String, List<GroundTruthRecord>> entry : grouped.entrySet()) {
            List<GroundTruthRecord> group = entry.getValue();
            Set<String> txIds = group.stream().map(GroundTruthRecord::transactionId).collect(Collectors.toSet());
            Set<String> roles = group.stream().map(GroundTruthRecord::role).collect(Collectors.toSet());

            if (txIds.size() == 3 && roles.containsAll(Set.of("HOP_1", "HOP_2", "HOP_3")) && group.size() == 3) {
                validTargets.add(txIds);
            } else {
                ambiguousTargetsCount++;
                ambiguousTargetTxIdSets.add(txIds);
            }
        }

        int totalInjectedTargets = validTargets.size();
        int truePositives = 0;
        int falsePositives = 0;
        int ignoredAmbiguousDetections = 0;

        for (Cycle cycle : detectedCycles) {
            Set<String> cycleTxIds = Set.of(
                    cycle.edge1().transactionId(),
                    cycle.edge2().transactionId(),
                    cycle.edge3().transactionId()
            );

            boolean matched = false;
            Iterator<Set<String>> targetIterator = validTargets.iterator();
            while (targetIterator.hasNext()) {
                Set<String> target = targetIterator.next();
                if (target.equals(cycleTxIds)) {
                    matched = true;
                    targetIterator.remove(); // Match each injected target at most once
                    break;
                }
            }

            if (matched) {
                truePositives++;
            } else {
                // Determine if this exact set of 3 transaction IDs matches an ambiguous target.
                // Partial overlaps (sharing 1 or 2 IDs) do not provide sufficient evidence
                // that the entire cycle is ambiguous, so they are not ignored.
                boolean exactAmbiguousMatch = false;
                for (Set<String> ambigTarget : ambiguousTargetTxIdSets) {
                    if (ambigTarget.equals(cycleTxIds)) {
                        exactAmbiguousMatch = true;
                        break;
                    }
                }

                if (exactAmbiguousMatch) {
                    ignoredAmbiguousDetections++;
                } else {
                    falsePositives++;
                }
            }
        }

        int falseNegatives = validTargets.size();

        return new CycleMatchResult(
                truePositives,
                falsePositives,
                falseNegatives,
                ambiguousTargetsCount,
                ignoredAmbiguousDetections,
                detectedCycles.size(),
                totalInjectedTargets
        );
    }
}
