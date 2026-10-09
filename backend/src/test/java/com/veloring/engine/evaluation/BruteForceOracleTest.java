package com.veloring.engine.evaluation;

import com.veloring.engine.detection.Cycle;
import com.veloring.engine.detection.ThreeAccountCycleDetector;
import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BruteForceOracleTest {

    private BruteForceCycleOracle oracle;
    private ThreeAccountCycleDetector detector;

    @BeforeEach
    void setUp() {
        oracle = new BruteForceCycleOracle();
        detector = new ThreeAccountCycleDetector();
    }

    private TransactionEvent createEvent(String txId, String sender, String receiver) {
        return TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId(sender)
                .receiverAccountId(receiver)
                .amount(new BigDecimal("100"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("WEB")
                .build();
    }

    @Test
    void testEquivalenceOnComplexGraph() {
        List<TransactionEvent> events = new ArrayList<>();
        // Seeded random for deterministic bounded tests
        Random random = new Random(42);
        String[] accounts = {"A", "B", "C", "D", "E"};

        // Generate 50 random edges to create a messy graph with parallel edges and cycles
        for (int i = 0; i < 50; i++) {
            String u = accounts[random.nextInt(accounts.length)];
            String v = accounts[random.nextInt(accounts.length)];
            events.add(createEvent("tx" + i, u, v));
        }

        TemporalSnapshot snapshot = new TemporalSnapshot(Instant.now(), events, Collections.emptyMap());

        List<Cycle> oracleCycles = oracle.detect(snapshot);
        List<Cycle> detectorCycles = detector.detect(snapshot);

        assertEquals(oracleCycles.size(), detectorCycles.size(), "Oracle and detector must find same number of cycles");
        for (int i = 0; i < oracleCycles.size(); i++) {
            assertEquals(oracleCycles.get(i), detectorCycles.get(i), "Cycles must match exactly in deterministic order");
        }
    }
}
