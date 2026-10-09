package com.veloring.engine.temporal;

import com.veloring.engine.event.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TemporalWindowEngineTest {

    private TemporalWindowEngine engine;

    @BeforeEach
    void setUp() {
        // windowSize = 60s, allowedLateness = 60s
        engine = new TemporalWindowEngine(60, 60);
    }

    private TransactionEvent createEvent(String id, Instant time, String sender, String receiver) {
        return TransactionEvent.builder()
                .transactionId(id)
                .eventTime(time)
                .senderAccountId(sender)
                .receiverAccountId(receiver)
                .amount(new BigDecimal("100.0"))
                .currency("USD")
                .channel("WEB")
                .build();
    }

    @Test
    void testEmptyWindow() {
        TemporalSnapshot snapshot = engine.getSnapshot();
        assertNull(snapshot.getWatermark());
        assertTrue(snapshot.getAllEvents().isEmpty());
    }

    @Test
    void testOneAcceptedEvent() {
        Instant time = Instant.parse("2023-01-01T10:00:00Z");
        TransactionEvent event = createEvent("txn1", time, "acc1", "acc2");

        assertEquals(TemporalAddResult.ACCEPTED, engine.add(event));

        TemporalSnapshot snapshot = engine.getSnapshot();
        assertEquals(time, snapshot.getWatermark());
        assertEquals(1, snapshot.getAllEvents().size());
        assertEquals("txn1", snapshot.getAllEvents().get(0).getTransactionId());
    }

    @Test
    void testMultipleEvents() {
        Instant time1 = Instant.parse("2023-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2023-01-01T10:00:10Z");

        engine.add(createEvent("txn1", time1, "acc1", "acc2"));
        engine.add(createEvent("txn2", time2, "acc3", "acc4"));

        TemporalSnapshot snapshot = engine.getSnapshot();
        assertEquals(time2, snapshot.getWatermark());
        assertEquals(2, snapshot.getAllEvents().size());
    }

    @Test
    void testExact60SecondBoundaryIsExcluded() {
        Instant time1 = Instant.parse("2023-01-01T10:00:00Z");
        engine.add(createEvent("txn1", time1, "acc1", "acc2"));

        Instant time2 = Instant.parse("2023-01-01T10:01:00Z"); // Exactly 60s later
        engine.add(createEvent("txn2", time2, "acc3", "acc4"));

        TemporalSnapshot snapshot = engine.getSnapshot();
        assertEquals(time2, snapshot.getWatermark());

        // txn1 should be evicted because eventTime <= watermark - 60s
        // time1 (10:00:00) <= time2 (10:01:00) - 60s = 10:00:00
        assertEquals(1, snapshot.getAllEvents().size());
        assertEquals("txn2", snapshot.getAllEvents().get(0).getTransactionId());
        assertTrue(snapshot.getEventsForAccount("acc1").isEmpty());
    }

    @Test
    void testEventOutsideActiveWindow() {
        Instant time1 = Instant.parse("2023-01-01T10:00:00Z");
        engine.add(createEvent("txn1", time1, "acc1", "acc2"));

        Instant time2 = Instant.parse("2023-01-01T10:01:05Z"); // 65s later
        engine.add(createEvent("txn2", time2, "acc3", "acc4"));

        TemporalSnapshot snapshot = engine.getSnapshot();
        assertEquals(time2, snapshot.getWatermark());
        assertEquals(1, snapshot.getAllEvents().size());
        assertEquals("txn2", snapshot.getAllEvents().get(0).getTransactionId());
    }

    @Test
    void testWatermarkAdvancement() {
        Instant time1 = Instant.parse("2023-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2023-01-01T09:59:50Z"); // Older event

        engine.add(createEvent("txn1", time1, "acc1", "acc2"));
        assertEquals(time1, engine.getSnapshot().getWatermark());

        // Add older event, watermark should not change
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("txn2", time2, "acc3", "acc4")));
        assertEquals(time1, engine.getSnapshot().getWatermark());
    }

    @Test
    void testSameEventTimeDeterministicOrdering() {
        Instant time = Instant.parse("2023-01-01T10:00:00Z");

        engine.add(createEvent("txnB", time, "acc1", "acc2"));
        engine.add(createEvent("txnA", time, "acc3", "acc4"));

        List<TransactionEvent> events = engine.getSnapshot().getAllEvents();
        assertEquals(2, events.size());
        // Ordered by eventTime, then transactionId ASC
        assertEquals("txnA", events.get(0).getTransactionId());
        assertEquals("txnB", events.get(1).getTransactionId());
    }

    @Test
    void testAcceptedOutOfOrderEvent() {
        Instant time1 = Instant.parse("2023-01-01T10:00:50Z");
        engine.add(createEvent("txn1", time1, "acc1", "acc2"));

        Instant time2 = Instant.parse("2023-01-01T10:00:10Z"); // 40s late, within 60s lateness
        assertEquals(TemporalAddResult.ACCEPTED, engine.add(createEvent("txn2", time2, "acc3", "acc4")));

        List<TransactionEvent> events = engine.getSnapshot().getAllEvents();
        assertEquals(2, events.size());
        assertEquals("txn2", events.get(0).getTransactionId()); // Ordered earlier
        assertEquals("txn1", events.get(1).getTransactionId());
    }

    @Test
    void testTooLateEvent() {
        Instant time1 = Instant.parse("2023-01-01T10:01:10Z");
        engine.add(createEvent("txn1", time1, "acc1", "acc2"));

        Instant time2 = Instant.parse("2023-01-01T10:00:05Z"); // 65s late
        assertEquals(TemporalAddResult.TOO_LATE, engine.add(createEvent("txn2", time2, "acc3", "acc4")));

        TemporalSnapshot snapshot = engine.getSnapshot();
        assertEquals(1, snapshot.getAllEvents().size());
        assertEquals(time1, snapshot.getWatermark());
    }

    @Test
    void testDuplicateTransactionId() {
        Instant time = Instant.parse("2023-01-01T10:00:00Z");
        engine.add(createEvent("txn1", time, "acc1", "acc2"));

        assertEquals(TemporalAddResult.DUPLICATE, engine.add(createEvent("txn1", time, "acc1", "acc2")));

        assertEquals(1, engine.getSnapshot().getAllEvents().size());
    }

    @Test
    void testDuplicateAfterEvictionCanBeAcceptedAgain() {
        Instant time1 = Instant.parse("2023-01-01T10:00:00Z");
        engine.add(createEvent("txn1", time1, "acc1", "acc2"));

        Instant time2 = Instant.parse("2023-01-01T10:01:05Z"); // Advance watermark, evicting txn1
        engine.add(createEvent("txn2", time2, "acc3", "acc4"));

        assertTrue(engine.getSnapshot().getEventsForAccount("acc1").isEmpty());

        // In this implementation, if a duplicate comes after it was evicted,
        // it would be evaluated as out-of-order. Since it's outside the lateness window,
        // it will be TOO_LATE, but it won't return DUPLICATE.
        // Wait, if it's within the lateness window somehow?
        // Let's test the duplicate index logic specifically: it was evicted, so duplicate check passes.
        Instant time3 = Instant.parse("2023-01-01T10:00:10Z"); // Within lateness window
        TransactionEvent lateEvent = createEvent("txnLate", time3, "acc1", "acc2");
        engine.add(lateEvent);

        // Evict lateEvent
        Instant time4 = Instant.parse("2023-01-01T10:02:00Z");
        engine.add(createEvent("txn4", time4, "acc5", "acc6"));

        // lateEvent is evicted. Now add it again.
        // It will be too late.
        assertEquals(TemporalAddResult.TOO_LATE, engine.add(lateEvent));

        // But what if it's not too late? E.g., event time in the future but we evicted manually? We don't have manual eviction.
        // The test asks to verify: duplicate after eviction can be accepted again.
        // But any event that is evicted MUST be > 60s older than the watermark.
        // Since allowed_lateness is also 60s, it will ALWAYS be TOO_LATE.
        // To test this properly, we could use different lateness vs window size.
        // Let's create a custom engine for this test: windowSize = 10s, allowedLateness = 100s
        TemporalWindowEngine customEngine = new TemporalWindowEngine(10, 100);

        customEngine.add(createEvent("txnA", Instant.parse("2023-01-01T10:00:00Z"), "acc1", "acc2"));

        // Advance watermark by 20s. txnA is evicted (time < watermark - 10s).
        customEngine.add(createEvent("txnB", Instant.parse("2023-01-01T10:00:20Z"), "acc3", "acc4"));

        // Re-add txnA. It is 20s late, lateness allowed is 100s. So it's NOT too late.
        // And since it was evicted, the duplicate index was cleared.
        // So it should be ACCEPTED. But it will be instantly evicted again since it's <= watermark - 10s.
        assertEquals(TemporalAddResult.ACCEPTED, customEngine.add(createEvent("txnA", Instant.parse("2023-01-01T10:00:00Z"), "acc1", "acc2")));
    }

    @Test
    void testDeterministicReplay() {
        // Run sequence 1
        TemporalWindowEngine engine1 = new TemporalWindowEngine(60, 60);
        engine1.add(createEvent("txn1", Instant.parse("2023-01-01T10:00:00Z"), "acc1", "acc2"));
        engine1.add(createEvent("txn3", Instant.parse("2023-01-01T10:00:15Z"), "acc5", "acc6"));
        engine1.add(createEvent("txn2", Instant.parse("2023-01-01T10:00:10Z"), "acc3", "acc4"));

        // Run sequence 2 (same inputs, same order)
        TemporalWindowEngine engine2 = new TemporalWindowEngine(60, 60);
        engine2.add(createEvent("txn1", Instant.parse("2023-01-01T10:00:00Z"), "acc1", "acc2"));
        engine2.add(createEvent("txn3", Instant.parse("2023-01-01T10:00:15Z"), "acc5", "acc6"));
        engine2.add(createEvent("txn2", Instant.parse("2023-01-01T10:00:10Z"), "acc3", "acc4"));

        TemporalSnapshot snap1 = engine1.getSnapshot();
        TemporalSnapshot snap2 = engine2.getSnapshot();

        assertEquals(snap1.getWatermark(), snap2.getWatermark());
        assertEquals(snap1.getAllEvents().size(), snap2.getAllEvents().size());

        for (int i = 0; i < snap1.getAllEvents().size(); i++) {
            assertEquals(snap1.getAllEvents().get(i).getTransactionId(), snap2.getAllEvents().get(i).getTransactionId());
        }
    }

    @Test
    void testSnapshotConsistency() {
        Instant time = Instant.parse("2023-01-01T10:00:00Z");
        engine.add(createEvent("txn1", time, "acc1", "acc2"));

        TemporalSnapshot snapshot = engine.getSnapshot();

        // Engine advances
        engine.add(createEvent("txn2", Instant.parse("2023-01-01T10:00:10Z"), "acc3", "acc4"));

        // Snapshot should remain unchanged
        assertEquals(time, snapshot.getWatermark());
        assertEquals(1, snapshot.getAllEvents().size());

        // Cannot mutate snapshot
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getAllEvents().add(createEvent("txn", time, "a", "b")));
    }

    @Test
    void testAccountLookup() {
        Instant time = Instant.parse("2023-01-01T10:00:00Z");
        engine.add(createEvent("txn1", time, "acc1", "acc2")); // acc1 sender, acc2 receiver
        engine.add(createEvent("txn2", time.plusSeconds(1), "acc2", "acc3")); // acc2 sender, acc3 receiver

        TemporalSnapshot snapshot = engine.getSnapshot();

        assertEquals(1, snapshot.getEventsForAccount("acc1").size());
        assertEquals("txn1", snapshot.getEventsForAccount("acc1").get(0).getTransactionId());

        assertEquals(2, snapshot.getEventsForAccount("acc2").size());

        assertEquals(1, snapshot.getEventsForAccount("acc3").size());
        assertEquals("txn2", snapshot.getEventsForAccount("acc3").get(0).getTransactionId());
    }

    @Test
    void testHighVolumeInsertionCorrectness() {
        Instant baseTime = Instant.parse("2023-01-01T10:00:00Z");

        for (int i = 0; i < 1000; i++) {
            engine.add(createEvent("txn" + i, baseTime.plusMillis(i), "acc" + i, "acc" + (i + 1)));
        }

        TemporalSnapshot snapshot = engine.getSnapshot();
        assertEquals(1000, snapshot.getAllEvents().size());
        assertEquals(baseTime.plusMillis(999), snapshot.getWatermark());

        // Adding one event that advances watermark by 65 seconds should evict everything
        engine.add(createEvent("txnFinal", baseTime.plusSeconds(65), "accA", "accB"));

        snapshot = engine.getSnapshot();
        assertEquals(1, snapshot.getAllEvents().size());
        assertEquals("txnFinal", snapshot.getAllEvents().get(0).getTransactionId());
    }

    @Test
    void testConcurrentAccessAndSnapshotSafety() throws InterruptedException {
        int threadCount = 10;
        int eventsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Instant baseTime = Instant.parse("2023-01-01T10:00:00Z");

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        String id = "txn_" + threadIndex + "_" + j;
                        engine.add(createEvent(id, baseTime.plusMillis(j), "accA", "accB"));

                        if (j % 10 == 0) {
                            TemporalSnapshot snapshot = engine.getSnapshot();
                            assertNotNull(snapshot);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        TemporalSnapshot finalSnapshot = engine.getSnapshot();
        // Exact size depends on concurrent execution, but it should be thread-safe without exceptions
        assertTrue(finalSnapshot.getAllEvents().size() <= threadCount * eventsPerThread);
    }
}
