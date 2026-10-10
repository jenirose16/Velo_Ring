package com.veloring.engine.detection;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalSnapshot;
import com.veloring.engine.temporal.TemporalWindowEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CycleDetectionSchedulerTest {

    @Mock
    private TemporalWindowEngine engine;

    @Mock
    private ThreeAccountCycleDetector detector;

    private CycleDetectionScheduler scheduler;
    private SimpleMeterRegistry meterRegistry;

    private MutableClock clock;

    static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void advance(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        meterRegistry = new SimpleMeterRegistry();
        // 60s window, 60s lateness -> retention is 60+60+60 = 180s
        scheduler = new CycleDetectionScheduler(engine, detector, 60, 60, clock, meterRegistry);
    }

    private TransactionEvent createEvent(String id, String sender, String receiver, Instant processedAt) {
        return TransactionEvent.builder()
                .transactionId(id)
                .senderAccountId(sender)
                .receiverAccountId(receiver)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .processedAt(processedAt != null ? processedAt : Instant.now())
                .channel("WEB")
                .build();
    }

    private Cycle createCycle(String prefix) {
        return new Cycle(
                createEvent(prefix + "1", "A", "B", Instant.parse("2026-01-01T09:59:00Z")),
                createEvent(prefix + "2", "B", "C", Instant.parse("2026-01-01T09:59:30Z")),
                createEvent(prefix + "3", "C", "A", Instant.parse("2026-01-01T09:59:50Z")) // max processedAt
        );
    }

    @Test
    void testCycleEmittedOnlyOnceWhileInRetention() {
        TemporalSnapshot snapshot = mock(TemporalSnapshot.class);
        when(engine.getSnapshot()).thenReturn(snapshot);

        Cycle cycle = createCycle("tx");
        when(snapshot.getAllEvents()).thenReturn(List.of(
            cycle.edge1(), cycle.edge2(), cycle.edge3()
        ).stream().map(e -> createEvent(e.transactionId(), e.senderAccountId(), e.receiverAccountId(), Instant.parse("2026-01-01T09:59:50Z"))).toList());
        when(detector.detect(snapshot)).thenReturn(List.of(cycle));

        // Scan 1
        scheduler.detectCycles();
        assertEquals(1, scheduler.getReportedCyclesCount());
        assertEquals(1, meterRegistry.find("veloring.detect.snapshot.duration").timer().count());
        assertEquals(1, meterRegistry.find("veloring.detect.algorithm.duration").timer().count());
        assertEquals(1, meterRegistry.find("veloring.detect.logging.duration").timer().count());
        assertEquals(1, meterRegistry.find("veloring.detect.e2e.delay").timer().count());
        // max processedAt = 10:00:00 - 09:59:50 = 10s delay
        assertEquals(10.0, meterRegistry.find("veloring.detect.e2e.delay").timer().max(java.util.concurrent.TimeUnit.SECONDS));

        // Scan 2 (10 seconds later)
        clock.advance(10);
        scheduler.detectCycles();
        // Still 1, didn't emit twice
        assertEquals(1, scheduler.getReportedCyclesCount());
        assertEquals(2, meterRegistry.find("veloring.detect.snapshot.duration").timer().count());
        // e2e delay timer count should STILL be 1 because cycle was deduplicated!
        assertEquals(1, meterRegistry.find("veloring.detect.e2e.delay").timer().count());
    }

    @Test
    void testExpiredDeduplicationEntriesRemoved() {
        TemporalSnapshot snapshot = mock(TemporalSnapshot.class);
        when(engine.getSnapshot()).thenReturn(snapshot);

        Cycle cycle = createCycle("tx");
        when(snapshot.getAllEvents()).thenReturn(List.of(
            createEvent("tx1", "A", "B", Instant.parse("2026-01-01T09:59:50Z")),
            createEvent("tx2", "B", "C", Instant.parse("2026-01-01T09:59:50Z")),
            createEvent("tx3", "C", "A", Instant.parse("2026-01-01T09:59:50Z"))
        ));
        when(detector.detect(snapshot)).thenReturn(List.of(cycle));

        scheduler.detectCycles();
        assertEquals(1, scheduler.getReportedCyclesCount());

        // Advance clock beyond retention (180 seconds)
        clock.advance(200);

        // Next scan should clean up expired cycles and, since detector still returns it, it's considered novel again
        scheduler.detectCycles();
        assertEquals(1, scheduler.getReportedCyclesCount());
    }

    @Test
    void testDifferentCyclesNotCollapsed() {
        TemporalSnapshot snapshot = mock(TemporalSnapshot.class);
        when(engine.getSnapshot()).thenReturn(snapshot);

        Cycle cycle1 = createCycle("c1_");
        Cycle cycle2 = createCycle("c2_"); // Completely distinct tx IDs

        when(snapshot.getAllEvents()).thenReturn(List.of(
            createEvent("c1_1", "A", "B", Instant.parse("2026-01-01T09:59:50Z")),
            createEvent("c1_2", "B", "C", Instant.parse("2026-01-01T09:59:50Z")),
            createEvent("c1_3", "C", "A", Instant.parse("2026-01-01T09:59:50Z")),
            createEvent("c2_1", "X", "Y", Instant.parse("2026-01-01T09:59:50Z")),
            createEvent("c2_2", "Y", "Z", Instant.parse("2026-01-01T09:59:50Z")),
            createEvent("c2_3", "Z", "X", Instant.parse("2026-01-01T09:59:50Z"))
        ));

        when(detector.detect(snapshot)).thenReturn(List.of(cycle1, cycle2));

        scheduler.detectCycles();
        assertEquals(2, scheduler.getReportedCyclesCount());
        assertEquals(2, meterRegistry.find("veloring.detect.e2e.delay").timer().count());
    }

    @Test
    void testDetectorExceptionDoesNotStopLaterScans() {
        TemporalSnapshot snapshot = mock(TemporalSnapshot.class);
        when(engine.getSnapshot()).thenReturn(snapshot);

        // Scan 1 throws, Scan 2 works
        Cycle cycle = createCycle("tx");
        when(detector.detect(snapshot))
            .thenThrow(new RuntimeException("Simulated detector crash"))
            .thenReturn(List.of(cycle));
            
        scheduler.detectCycles(); // Should catch and log, not throw

        scheduler.detectCycles(); // This is Scan 2
        assertEquals(1, scheduler.getReportedCyclesCount());
    }
}
