package com.veloring.engine.evaluation;

import com.veloring.engine.detection.Cycle;
import com.veloring.engine.event.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroundTruthEvaluatorTest {

    private GroundTruthEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new GroundTruthEvaluator();
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

    private GroundTruthEvaluator.GroundTruthRecord createRecord(String experimentId, String scenarioId, String txId, String role, boolean injected) {
        return new GroundTruthEvaluator.GroundTruthRecord(experimentId, scenarioId, "CYCLE_3", txId, role, injected, true);
    }

    @Test
    void testPerfectMatch() {
        List<GroundTruthEvaluator.GroundTruthRecord> records = List.of(
                createRecord("EXP1", "SC1", "tx1", "HOP_1", true),
                createRecord("EXP1", "SC1", "tx2", "HOP_2", true),
                createRecord("EXP1", "SC1", "tx3", "HOP_3", true)
        );

        Cycle cycle = new Cycle(
                createEvent("tx1", "A", "B"),
                createEvent("tx2", "B", "C"),
                createEvent("tx3", "C", "A")
        );

        CycleMatchResult result = evaluator.evaluateRecords(records, List.of(cycle));

        assertEquals(1, result.truePositives());
        assertEquals(0, result.ordinaryFalsePositives());
        assertEquals(0, result.falseNegatives());
        assertEquals(0, result.ambiguousTargets());
        assertEquals(1, result.totalInjectedTargets());
        assertEquals(1.0, result.precision());
        assertEquals(1.0, result.recall());
        assertEquals(1.0, result.f1Score());
    }

    @Test
    void testPartialOverlapIsFalsePositive() {
        List<GroundTruthEvaluator.GroundTruthRecord> records = List.of(
                createRecord("EXP1", "SC1", "tx1", "HOP_1", true),
                createRecord("EXP1", "SC1", "tx2", "HOP_2", true),
                createRecord("EXP1", "SC1", "tx3", "HOP_3", true)
        );

        Cycle cycle = new Cycle(
                createEvent("tx1", "A", "B"),
                createEvent("tx2", "B", "C"),
                createEvent("tx99", "C", "A") // Partial overlap
        );

        CycleMatchResult result = evaluator.evaluateRecords(records, List.of(cycle));

        assertEquals(0, result.truePositives());
        assertEquals(1, result.ordinaryFalsePositives()); // Unmatched structural cycle
        assertEquals(1, result.falseNegatives()); // Target missed
        assertEquals(0.0, result.precision());
        assertEquals(0.0, result.recall());
        assertTrue(Double.isNaN(result.f1Score()));
    }



    @Test
    void testDuplicateMatchingPrevented() {
        List<GroundTruthEvaluator.GroundTruthRecord> records = List.of(
                createRecord("EXP1", "SC1", "tx1", "HOP_1", true),
                createRecord("EXP1", "SC1", "tx2", "HOP_2", true),
                createRecord("EXP1", "SC1", "tx3", "HOP_3", true)
        );

        Cycle cycle1 = new Cycle(createEvent("tx1", "A", "B"), createEvent("tx2", "B", "C"), createEvent("tx3", "C", "A"));
        Cycle cycle2 = new Cycle(createEvent("tx1", "A", "B"), createEvent("tx2", "B", "C"), createEvent("tx3", "C", "A"));

        // Two identical cycles detected, but only one target
        CycleMatchResult result = evaluator.evaluateRecords(records, List.of(cycle1, cycle2));

        assertEquals(1, result.truePositives());
        assertEquals(1, result.ordinaryFalsePositives()); // Second cycle fails to match remaining targets
        assertEquals(0, result.falseNegatives());
        assertEquals(0.5, result.precision());
        assertEquals(1.0, result.recall());
    }

    @Test
    void testZeroDenominatorMetrics() {
        CycleMatchResult result = new CycleMatchResult(0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertTrue(Double.isNaN(result.precision()));
        assertTrue(Double.isNaN(result.recall()));
        assertTrue(Double.isNaN(result.f1Score()));
    }

    @Test
    void testExactMatchAmbiguousTargetIsIgnoredNotFP() {
        // Target has 3 txIds, but incorrect roles (two HOP_1s), making it ambiguous
        List<GroundTruthEvaluator.GroundTruthRecord> records = List.of(
                createRecord("EXP1", "SC_AMB", "tx1", "HOP_1", true),
                createRecord("EXP1", "SC_AMB", "tx2", "HOP_1", true),
                createRecord("EXP1", "SC_AMB", "tx3", "HOP_3", true)
        );

        Cycle cycle = new Cycle(
                createEvent("tx1", "A", "B"),
                createEvent("tx2", "B", "C"),
                createEvent("tx3", "C", "A")
        );

        CycleMatchResult result = evaluator.evaluateRecords(records, List.of(cycle));

        assertEquals(1, result.ambiguousTargets());
        assertEquals(0, result.totalInjectedTargets());
        assertEquals(0, result.ordinaryFalsePositives()); // Exactly matches ambiguous txIds -> ignored, NOT FP
        assertEquals(1, result.ignoredAmbiguousDetections());
        assertEquals(0, result.falseNegatives());
        assertTrue(Double.isNaN(result.precision()));
        assertTrue(Double.isNaN(result.recall()));
    }

    @Test
    void testPartialOverlapAmbiguousTargetIsFP() {
        // Target has 3 txIds, but incorrect roles (making it ambiguous)
        List<GroundTruthEvaluator.GroundTruthRecord> records = List.of(
                createRecord("EXP1", "SC_AMB", "tx1", "HOP_1", true),
                createRecord("EXP1", "SC_AMB", "tx2", "HOP_1", true),
                createRecord("EXP1", "SC_AMB", "tx3", "HOP_3", true)
        );

        Cycle cycle = new Cycle(
                createEvent("tx1", "A", "B"), // Overlaps 1 txId
                createEvent("tx88", "B", "C"),
                createEvent("tx99", "C", "A")
        );

        CycleMatchResult result = evaluator.evaluateRecords(records, List.of(cycle));

        assertEquals(1, result.ambiguousTargets());
        assertEquals(0, result.totalInjectedTargets());
        assertEquals(1, result.ordinaryFalsePositives()); // Partial overlap is insufficient -> FP
        assertEquals(0, result.ignoredAmbiguousDetections());
    }

    @Test
    void testUnrelatedCycleIsFP() {
        List<GroundTruthEvaluator.GroundTruthRecord> records = List.of(
                createRecord("EXP1", "SC_AMB", "tx1", "HOP_1", true),
                createRecord("EXP1", "SC_AMB", "tx2", "HOP_1", true),
                createRecord("EXP1", "SC_AMB", "tx3", "HOP_3", true)
        );

        Cycle cycle = new Cycle(
                createEvent("tx77", "A", "B"),
                createEvent("tx88", "B", "C"),
                createEvent("tx99", "C", "A")
        );

        CycleMatchResult result = evaluator.evaluateRecords(records, List.of(cycle));

        assertEquals(1, result.ambiguousTargets());
        assertEquals(0, result.totalInjectedTargets());
        assertEquals(1, result.ordinaryFalsePositives()); // Cycle has no overlap -> FP
        assertEquals(0, result.ignoredAmbiguousDetections());
    }
}
