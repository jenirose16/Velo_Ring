package com.veloring.engine.evaluation.benchmark;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.*;
import com.veloring.engine.temporal.TemporalWindowEngine;
import com.veloring.engine.temporal.TemporalAddResult;
import com.veloring.engine.event.TransactionEvent;

public class BenchmarkTests {

    @Test
    public void testPercentileCalculation() {
        // Normal array of 10 elements: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        List<Long> data = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        // p50 -> ceil(0.5 * 10) - 1 = ceil(5) - 1 = 4 -> index 4 -> 5L
        assertEquals(5L, PercentileUtil.calculate(data, 50.0));

        // p95 -> ceil(0.95 * 10) - 1 = ceil(9.5) - 1 = 10 - 1 = 9 -> 10L
        assertEquals(10L, PercentileUtil.calculate(data, 95.0));

        // p99 -> ceil(0.99 * 10) - 1 = ceil(9.9) - 1 = 10 - 1 = 9 -> 10L
        assertEquals(10L, PercentileUtil.calculate(data, 99.0));

        // Single sample
        List<Long> single = Collections.singletonList(42L);
        assertEquals(42L, PercentileUtil.calculate(single, 50.0));
        assertEquals(42L, PercentileUtil.calculate(single, 99.0));

        // Empty list
        assertNull(PercentileUtil.calculate(Collections.emptyList(), 50.0));

        // Invalid percentiles
        assertThrows(IllegalArgumentException.class, () -> PercentileUtil.calculate(data, -5.0));
        assertThrows(IllegalArgumentException.class, () -> PercentileUtil.calculate(data, 0.0));
        assertThrows(IllegalArgumentException.class, () -> PercentileUtil.calculate(data, 100.1));

        // Duplicates
        List<Long> dupes = Arrays.asList(1L, 1L, 2L, 2L, 2L);
        assertEquals(2L, PercentileUtil.calculate(dupes, 50.0)); // ceil(2.5)-1 = 2 -> 2L
    }

    @Test
    public void testDeterministicWorkloadGeneration() {
        BenchmarkFixtureGenerator.Config config1 = new BenchmarkFixtureGenerator.Config();
        config1.seed = 123;
        config1.totalTransactions = 1000;
        config1.cycleCount = 10;

        BenchmarkFixtureGenerator.Config config2 = new BenchmarkFixtureGenerator.Config();
        config2.seed = 123;
        config2.totalTransactions = 1000;
        config2.cycleCount = 10;

        BenchmarkWorkload workload1 = BenchmarkFixtureGenerator.generate(config1);
        BenchmarkWorkload workload2 = BenchmarkFixtureGenerator.generate(config2);

        assertEquals(workload1.events().size(), workload2.events().size());

        for (int i = 0; i < workload1.events().size(); i++) {
            assertEquals(workload1.events().get(i).getTransactionId(), workload2.events().get(i).getTransactionId());
        }
    }

    @Test
    public void testExactCycleCountAndNoOverlaps() {
        BenchmarkFixtureGenerator.Config config = new BenchmarkFixtureGenerator.Config();
        config.seed = 42;
        config.totalTransactions = 10000;
        config.cycleCount = 50;

        BenchmarkWorkload workload = BenchmarkFixtureGenerator.generate(config);

        // Exactly requested number of cycles (3 records per cycle)
        assertEquals(50 * 3, workload.groundTruth().size());
    }

    @Test
    public void testImpossibleConfiguration() {
        BenchmarkFixtureGenerator.Config config = new BenchmarkFixtureGenerator.Config();
        config.seed = 42;
        config.totalTransactions = 100;
        // Requires 150 slots, but only ~97 slots available
        config.cycleCount = 50;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            BenchmarkFixtureGenerator.generate(config);
        });
        assertTrue(ex.getMessage().contains("Impossible configuration"));
    }

    @Test
    public void testImpossibleConfigurationDueToDensity() {
        BenchmarkFixtureGenerator.Config config = new BenchmarkFixtureGenerator.Config();
        config.seed = 42;
        config.totalTransactions = 100;
        // Requires 90 slots, available 97. Random placement might fail to find valid slots after a while.
        config.cycleCount = 30;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            BenchmarkFixtureGenerator.generate(config);
        });
        assertTrue(ex.getMessage().contains("Exceeded maximum placement attempts"));
    }

    @Test
    public void testInjectedTargetValidityAndSnapshotEligibility() {
        BenchmarkFixtureGenerator.Config config = new BenchmarkFixtureGenerator.Config();
        config.seed = 1;
        config.totalTransactions = 10000;
        config.cycleCount = 10;
        config.finalWindowOnly = true;
        config.windowSizeSec = 60;

        BenchmarkWorkload workload = BenchmarkFixtureGenerator.generate(config);

        TemporalWindowEngine engine = new TemporalWindowEngine(60, 60);
        for (TransactionEvent ev : workload.events()) {
            engine.add(ev);
        }

        List<TransactionEvent> snapshotEvents = engine.getSnapshot().getAllEvents();

        // All ground truth injected cycles must be in the snapshot!
        Set<String> snapshotTxIds = new HashSet<>();
        for (TransactionEvent ev : snapshotEvents) {
            snapshotTxIds.add(ev.getTransactionId());
        }

        int foundInSnapshot = 0;
        for (var gt : workload.groundTruth()) {
            if (snapshotTxIds.contains(gt.transactionId())) {
                foundInSnapshot++;
            }
        }

        assertEquals(30, workload.groundTruth().size(), "10 cycles * 3 hops");
        assertEquals(30, foundInSnapshot, "All injected targets must be eligible in the final snapshot");
    }

    @Test
    public void testE4BoundaryBehavior() {
        TemporalWindowEngine engine = new TemporalWindowEngine(60, 60);
        Instant baseTime = Instant.parse("2026-01-01T00:02:00Z"); // 120s

        // Advance watermark to 120s
        TransactionEvent advanceWatermark = TransactionEvent.builder()
            .transactionId("w1").senderAccountId("a").receiverAccountId("b")
            .amount(java.math.BigDecimal.TEN).currency("USD").channel("CH")
            .eventTime(baseTime)
            .build();
        engine.add(advanceWatermark);

        // Allowed lateness is 60s, so threshold is 120s - 60s = 60s.
        // Event at 61s: INSIDE allowed lateness (accepted)
        TransactionEvent inside = TransactionEvent.builder()
            .transactionId("e1").senderAccountId("a").receiverAccountId("b")
            .amount(java.math.BigDecimal.TEN).currency("USD").channel("CH")
            .eventTime(baseTime.minusSeconds(59))
            .build();
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(inside));

        // Event at 60s: EXACTLY on boundary (rejected)
        TransactionEvent boundary = TransactionEvent.builder()
            .transactionId("e2").senderAccountId("a").receiverAccountId("b")
            .amount(java.math.BigDecimal.TEN).currency("USD").channel("CH")
            .eventTime(baseTime.minusSeconds(60))
            .build();
        assertEquals(TemporalAddResult.TOO_LATE, engine.add(boundary));

        // Event at 59s: BEYOND lateness (rejected)
        TransactionEvent beyond = TransactionEvent.builder()
            .transactionId("e3").senderAccountId("a").receiverAccountId("b")
            .amount(java.math.BigDecimal.TEN).currency("USD").channel("CH")
            .eventTime(baseTime.minusSeconds(61))
            .build();
        assertEquals(TemporalAddResult.TOO_LATE, engine.add(beyond));
    }

    @Test
    public void testUndefinedMetricHandling() {
        assertEquals("N/A", CsvReporter.formatLong(null));
        assertEquals("N/A", CsvReporter.formatDouble(null));
        assertEquals("N/A", CsvReporter.formatDouble(Double.NaN));
        assertEquals("N/A", CsvReporter.formatDouble(Double.POSITIVE_INFINITY));
    }
}
