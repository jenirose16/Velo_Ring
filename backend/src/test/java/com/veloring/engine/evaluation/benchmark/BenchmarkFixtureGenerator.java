package com.veloring.engine.evaluation.benchmark;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.evaluation.GroundTruthEvaluator.GroundTruthRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class BenchmarkFixtureGenerator {

    public static class Config {
        public long seed = 42;
        public int totalTransactions = 10000;
        public int cycleCount = 50;
        public long eventSpacingMs = 10;
        public int outOfOrderCount = 0;
        public long maxLatenessMs = 0;
        public boolean finalWindowOnly = false;
        public long windowSizeSec = 60;
        public long allowedLatenessSec = 60;
    }

    public static BenchmarkWorkload generate(Config config) {
        Random random = new Random(config.seed);
        List<TransactionEvent> events = new ArrayList<>();
        List<GroundTruthRecord> truth = new ArrayList<>();

        Instant startTime = Instant.parse("2026-01-01T00:00:00Z");

        int accountCount = Math.max(100, config.totalTransactions / 10);

        int startIndex = 0;
        if (config.finalWindowOnly) {
            long totalTimeMs = config.totalTransactions * config.eventSpacingMs;
            long windowMs = config.windowSizeSec * 1000;
            // Inject only in the final window (actually slightly less to ensure it doesn't fall out right at the edge)
            if (totalTimeMs > windowMs) {
                startIndex = (int) (config.totalTransactions - (windowMs / config.eventSpacingMs));
                // Add a small buffer so events aren't right on the edge of the window
                startIndex += (config.totalTransactions - startIndex) / 10;
            }
        }

        int range = config.totalTransactions - 3 - startIndex;
        if (range <= 0) {
            startIndex = 0;
            range = config.totalTransactions - 3;
        }

        // Validate that it's possible to generate requested non-overlapping cycles in the range
        // Each cycle requires 3 slots. Max possible cycles = range / 3 (roughly).
        if (config.cycleCount * 3 > range) {
            throw new IllegalArgumentException(String.format(
                "Impossible configuration: requested %d cycles (requiring %d slots) but only %d slots available in the range [%d, %d]",
                config.cycleCount, config.cycleCount * 3, range, startIndex, config.totalTransactions - 3
            ));
        }

        Set<Integer> cycleStartIndices = new HashSet<>();
        int attempts = 0;
        int maxAttempts = config.cycleCount * 100; // prevent unbounded loops

        while (cycleStartIndices.size() < config.cycleCount) {
            int candidate = startIndex + random.nextInt(range);
            boolean overlap = false;
            for (int existing : cycleStartIndices) {
                if (Math.abs(candidate - existing) < 3) {
                    overlap = true;
                    break;
                }
            }
            if (!overlap) {
                cycleStartIndices.add(candidate);
            }

            attempts++;
            if (attempts > maxAttempts) {
                throw new IllegalArgumentException("Exceeded maximum placement attempts. Configuration is too dense for random placement.");
            }
        }

        int txIndex = 0;
        int cycleId = 0;

        while (txIndex < config.totalTransactions) {
            if (cycleStartIndices.contains(txIndex)) {
                String expId = "EXP";
                String scenarioId = "cycle-" + cycleId++;
                String a = "C_ACC_" + random.nextInt(accountCount);
                String b = "C_ACC_" + random.nextInt(accountCount);
                while(b.equals(a)) b = "C_ACC_" + random.nextInt(accountCount);
                String c = "C_ACC_" + random.nextInt(accountCount);
                while(c.equals(a) || c.equals(b)) c = "C_ACC_" + random.nextInt(accountCount);

                String tx1 = UUID.nameUUIDFromBytes((scenarioId + "_1").getBytes()).toString();
                String tx2 = UUID.nameUUIDFromBytes((scenarioId + "_2").getBytes()).toString();
                String tx3 = UUID.nameUUIDFromBytes((scenarioId + "_3").getBytes()).toString();

                events.add(buildEvent(tx1, a, b, startTime.plusMillis((long)txIndex * config.eventSpacingMs)));
                events.add(buildEvent(tx2, b, c, startTime.plusMillis((long)(txIndex+1) * config.eventSpacingMs)));
                events.add(buildEvent(tx3, c, a, startTime.plusMillis((long)(txIndex+2) * config.eventSpacingMs)));

                truth.add(new GroundTruthRecord(expId, scenarioId, "CYCLE_3", tx1, "HOP_1", true, true));
                truth.add(new GroundTruthRecord(expId, scenarioId, "CYCLE_3", tx2, "HOP_2", true, true));
                truth.add(new GroundTruthRecord(expId, scenarioId, "CYCLE_3", tx3, "HOP_3", true, true));

                txIndex += 3;
            } else {
                String a = "B_ACC_" + random.nextInt(accountCount);
                String b = "B_ACC_" + random.nextInt(accountCount);
                while(b.equals(a)) b = "B_ACC_" + random.nextInt(accountCount);
                String tx = UUID.nameUUIDFromBytes(("bg_" + txIndex).getBytes()).toString();
                events.add(buildEvent(tx, a, b, startTime.plusMillis((long)txIndex * config.eventSpacingMs)));
                txIndex++;
            }
        }

        if (config.outOfOrderCount > 0) {
            for (int i = 0; i < config.outOfOrderCount; i++) {
                int idx = random.nextInt(events.size());
                TransactionEvent ev = events.get(idx);
                // Jitter logic: delay is from 0 to maxLatenessMs * 1.5 uniformly
                long lateBy = config.maxLatenessMs > 0 ? (long)(random.nextDouble() * config.maxLatenessMs * 1.5) : 5000;
                ev.setEventTime(ev.getEventTime().minusMillis(lateBy));
            }
        }

        // Simulate temporal engine watermark to determine static eligibility
        long watermarkMs = -1;
        Set<String> ineligibleEventIds = new HashSet<>();

        // Pass 1: Arrival lateness
        for (TransactionEvent ev : events) {
            long evTime = ev.getEventTime().toEpochMilli();
            if (watermarkMs < 0 || evTime > watermarkMs) {
                watermarkMs = evTime;
            }
            long allowedLateMs = config.allowedLatenessSec * 1000L;
            if (watermarkMs - evTime >= allowedLateMs) {
                ineligibleEventIds.add(ev.getTransactionId());
            }
        }

        // Pass 2: Window eviction (at the end of the stream)
        long windowMs = config.windowSizeSec * 1000L;
        for (TransactionEvent ev : events) {
            long evTime = ev.getEventTime().toEpochMilli();
            if (watermarkMs - evTime >= windowMs) {
                ineligibleEventIds.add(ev.getTransactionId());
            }
        }

        // Update GroundTruthRecords with eligibility
        List<GroundTruthRecord> updatedTruth = new ArrayList<>(truth.size());
        for (GroundTruthRecord r : truth) {
            boolean eligible = !ineligibleEventIds.contains(r.transactionId());
            updatedTruth.add(new GroundTruthRecord(r.experimentId(), r.scenarioId(), r.scenarioType(), r.transactionId(), r.role(), r.injectedPattern(), eligible));
        }

        return new BenchmarkWorkload(events, updatedTruth);
    }

    private static TransactionEvent buildEvent(String id, String sender, String receiver, Instant time) {
        return TransactionEvent.builder()
            .transactionId(id)
            .senderAccountId(sender)
            .receiverAccountId(receiver)
            .amount(BigDecimal.valueOf(100.0))
            .currency("USD")
            .eventTime(time)
            .channel("BENCHMARK")
            .build();
    }
}
