package com.veloring.engine.detection;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ThreeAccountCycleDetectorTest {

    private ThreeAccountCycleDetector detector;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        detector = new ThreeAccountCycleDetector();
        baseTime = Instant.now();
    }

    private TransactionEvent createEvent(String id, String sender, String receiver) {
        return TransactionEvent.builder()
                .transactionId(id)
                .senderAccountId(sender)
                .receiverAccountId(receiver)
                .amount(new BigDecimal("100"))
                .currency("USD")
                .channel("WEB")
                .eventTime(baseTime)
                .build();
    }

    private TemporalSnapshot createSnapshot(List<TransactionEvent> events) {
        return new TemporalSnapshot(baseTime, events, Collections.emptyMap());
    }

    @Test
    void testOneValidCycle() {
        List<TransactionEvent> events = Arrays.asList(
                createEvent("t1", "A", "B"),
                createEvent("t2", "B", "C"),
                createEvent("t3", "C", "A")
        );
        List<Cycle> cycles = detector.detect(createSnapshot(events));
        assertEquals(1, cycles.size());
        Cycle c = cycles.get(0);
        assertEquals("t1", c.edge1().transactionId());
        assertEquals("t2", c.edge2().transactionId());
        assertEquals("t3", c.edge3().transactionId());
    }

    @Test
    void testNoCycle() {
        List<TransactionEvent> events = Arrays.asList(
                createEvent("t1", "A", "B"),
                createEvent("t2", "B", "C"),
                createEvent("t3", "C", "D")
        );
        assertTrue(detector.detect(createSnapshot(events)).isEmpty());
    }

    @Test
    void testParallelEdges() {
        List<TransactionEvent> events = Arrays.asList(
                createEvent("t1", "A", "B"),
                createEvent("t1a", "A", "B"), // Parallel edge
                createEvent("t2", "B", "C"),
                createEvent("t3", "C", "A")
        );
        List<Cycle> cycles = detector.detect(createSnapshot(events));
        assertEquals(2, cycles.size());

        // Output must be sorted deterministically
        assertEquals("t1", cycles.get(0).edge1().transactionId());
        assertEquals("t1a", cycles.get(1).edge1().transactionId());
    }

    @Test
    void testReverseDirection() {
        List<TransactionEvent> events = Arrays.asList(
                createEvent("t1", "A", "B"),
                createEvent("t2", "B", "C"),
                createEvent("t3", "C", "A"),
                createEvent("t4", "A", "C"),
                createEvent("t5", "C", "B"),
                createEvent("t6", "B", "A")
        );
        List<Cycle> cycles = detector.detect(createSnapshot(events));
        assertEquals(2, cycles.size());

        Cycle c1 = cycles.get(0);
        assertEquals("t1", c1.edge1().transactionId());
        assertEquals("t2", c1.edge2().transactionId());
        assertEquals("t3", c1.edge3().transactionId());

        Cycle c2 = cycles.get(1);
        assertEquals("t4", c2.edge1().transactionId());
        assertEquals("t5", c2.edge2().transactionId());
        assertEquals("t6", c2.edge3().transactionId());
    }

    @Test
    void testDuplicateIdsThrowException() {
        List<TransactionEvent> events = Arrays.asList(
                createEvent("t1", "A", "B"),
                createEvent("t1", "B", "C")
        );
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(events)));
    }

    @Test
    void testMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> detector.detect(null));

        List<TransactionEvent> nullTxId = List.of(createEvent(null, "A", "B"));
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(nullTxId)));

        List<TransactionEvent> nullSender = List.of(createEvent("t1", null, "B"));
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(nullSender)));

        List<TransactionEvent> nullReceiver = List.of(createEvent("t1", "A", null));
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(nullReceiver)));
    }

    @Test
    void testSelfLoopsAreSkipped() {
        List<TransactionEvent> events = Arrays.asList(
                createEvent("t1", "A", "A"),
                createEvent("t2", "A", "B"),
                createEvent("t3", "B", "C"),
                createEvent("t4", "C", "A")
        );
        List<Cycle> cycles = detector.detect(createSnapshot(events));
        assertEquals(1, cycles.size());
    }

    @Test
    void testInputOrderIndependenceAndCanonicalEquality() {
        List<TransactionEvent> events1 = Arrays.asList(
                createEvent("t1", "A", "B"),
                createEvent("t2", "B", "C"),
                createEvent("t3", "C", "A")
        );
        List<TransactionEvent> events2 = Arrays.asList(
                createEvent("t3", "C", "A"),
                createEvent("t1", "A", "B"),
                createEvent("t2", "B", "C")
        );

        List<Cycle> cycles1 = detector.detect(createSnapshot(events1));
        List<Cycle> cycles2 = detector.detect(createSnapshot(events2));

        assertEquals(1, cycles1.size());
        assertEquals(1, cycles2.size());
        assertEquals(cycles1.get(0), cycles2.get(0));
        assertEquals(cycles1.get(0).hashCode(), cycles2.get(0).hashCode());

        // Ensure immutability constraint test: mutate original event
        events1.get(0).setTransactionId("mutated");
        // Due to Edge record mapping, the stored Cycle hash/equals won't change
        assertEquals("t1", cycles1.get(0).edge1().transactionId());
    }

    @Test
    void testSnapshotImmutability() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("t1", "A", "B"));
        TemporalSnapshot snapshot = createSnapshot(events);

        detector.detect(snapshot);

        assertEquals(1, snapshot.getAllEvents().size());
        assertEquals("t1", snapshot.getAllEvents().get(0).getTransactionId());
    }

    @Test
    void testEmptySnapshot() {
        TemporalSnapshot snapshot = createSnapshot(Collections.emptyList());
        assertTrue(detector.detect(snapshot).isEmpty());
    }

    @Test
    void testNullEventInSnapshotThrowsException() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("t1", "A", "B"));
        events.add(null);
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(events)));
    }

    @Test
    void testBlankIdsThrowsException() {
        List<TransactionEvent> blankTxId = List.of(createEvent("  ", "A", "B"));
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(blankTxId)));

        List<TransactionEvent> blankSender = List.of(createEvent("t1", "  ", "B"));
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(blankSender)));

        List<TransactionEvent> blankReceiver = List.of(createEvent("t1", "A", "  "));
        assertThrows(IllegalArgumentException.class, () -> detector.detect(createSnapshot(blankReceiver)));
    }

    @Test
    void testInvalidCycleConstruction() {
        TransactionEvent e1 = createEvent("t1", "A", "B");
        TransactionEvent e2 = createEvent("t2", "B", "C");

        // Not a cycle
        TransactionEvent e3_open = createEvent("t3", "C", "D");
        assertThrows(IllegalArgumentException.class, () -> new Cycle(e1, e2, e3_open));

        // Two distinct accounts only
        TransactionEvent e3_short = createEvent("t3", "C", "B");
        assertThrows(IllegalArgumentException.class, () -> new Cycle(e1, e2, e3_short));

        // Null edges
        assertThrows(IllegalArgumentException.class, () -> new Cycle(null, e2, e3_open));
    }
}
