package com.veloring.engine.evaluation;

import com.veloring.engine.detection.Cycle;
import com.veloring.engine.detection.ThreeAccountCycleDetector;
import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalAddResult;
import com.veloring.engine.temporal.TemporalSnapshot;
import com.veloring.engine.temporal.TemporalWindowEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalWindowCycleTest {

    private TemporalWindowEngine engine;
    private ThreeAccountCycleDetector detector;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        // windowSize = 60s, allowedLateness = 30s
        engine = new TemporalWindowEngine(60, 30);
        detector = new ThreeAccountCycleDetector();
        baseTime = Instant.now();
    }

    private TransactionEvent createEvent(String txId, String sender, String receiver, Instant time) {
        return TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId(sender)
                .receiverAccountId(receiver)
                .amount(new BigDecimal("100"))
                .currency("USD")
                .eventTime(time)
                .channel("WEB")
                .build();
    }

    @Test
    void testInOrderCycleDetection() {
        engine.add(createEvent("t1", "A", "B", baseTime.plusSeconds(10)));
        engine.add(createEvent("t2", "B", "C", baseTime.plusSeconds(20)));
        engine.add(createEvent("t3", "C", "A", baseTime.plusSeconds(30)));

        TemporalSnapshot snapshot = engine.getSnapshot();
        List<Cycle> cycles = detector.detect(snapshot);
        assertEquals(1, cycles.size());
    }

    @Test
    void testOutOfOrderWithinLateness() {
        // Watermark moves to 30
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t3", "C", "A", baseTime.plusSeconds(30))));

        // Allowed lateness is 30s, meaning events > T=0 are accepted. T=10 is accepted.
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t1", "A", "B", baseTime.plusSeconds(10))));
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t2", "B", "C", baseTime.plusSeconds(20))));

        TemporalSnapshot snapshot = engine.getSnapshot();
        List<Cycle> cycles = detector.detect(snapshot);
        assertEquals(1, cycles.size());
    }

    @Test
    void testRejectedTooLate() {
        // Watermark moves to 40
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t3", "C", "A", baseTime.plusSeconds(40))));

        // Allowed lateness is 30s. Threshold = 40 - 30 = 10.
        // Event at T=10 is exactly on threshold, should be TOO_LATE (<= threshold)
        assertEquals(TemporalAddResult.TOO_LATE, engine.add(createEvent("t1", "A", "B", baseTime.plusSeconds(10))));

        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t2", "B", "C", baseTime.plusSeconds(20))));

        TemporalSnapshot snapshot = engine.getSnapshot();
        List<Cycle> cycles = detector.detect(snapshot);
        assertEquals(0, cycles.size(), "Cycle should not be formed because t1 was rejected");
    }

    @Test
    void testEvictionCrossed() {
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t1", "A", "B", baseTime.plusSeconds(10))));
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t2", "B", "C", baseTime.plusSeconds(20))));

        // Watermark moves to 70. Window is 60s. Eviction threshold = 70 - 60 = 10.
        // Events <= 10 are evicted. Thus t1 (at 10) is evicted.
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t4_unrelated", "X", "Y", baseTime.plusSeconds(70))));

        // Allowed lateness is 30s. Lateness threshold = 70 - 30 = 40.
        // Event at 50 is ACCEPTED.
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("t3", "C", "A", baseTime.plusSeconds(50))));

        TemporalSnapshot snapshot = engine.getSnapshot();
        List<Cycle> cycles = detector.detect(snapshot);
        assertEquals(0, cycles.size(), "Cycle broken because t1 was evicted");
    }

    @Test
    void testExactWindowBoundary() {
        // Eviction threshold test:
        // Watermark = 70, window = 60, threshold = 10.
        // t1 at 10 should be evicted. t_11 at 11 should be kept.
        engine.add(createEvent("t_10", "A", "B", baseTime.plusSeconds(10)));
        engine.add(createEvent("t_11", "B", "C", baseTime.plusSeconds(11)));

        engine.add(createEvent("wm", "X", "Y", baseTime.plusSeconds(70)));

        TemporalSnapshot snapshot = engine.getSnapshot();
        boolean foundT10 = snapshot.getAllEvents().stream().anyMatch(e -> e.getTransactionId().equals("t_10"));
        boolean foundT11 = snapshot.getAllEvents().stream().anyMatch(e -> e.getTransactionId().equals("t_11"));

        assertTrue(!foundT10, "t_10 exactly on boundary must be evicted");
        assertTrue(foundT11, "t_11 just after boundary must be kept");
    }
}
