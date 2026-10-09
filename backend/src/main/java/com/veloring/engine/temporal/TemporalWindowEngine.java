package com.veloring.engine.temporal;

import com.veloring.engine.event.TransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class TemporalWindowEngine {

    private final Duration windowSize;
    private final Duration allowedLateness;

    private Instant watermark = null;

    private final TreeSet<TransactionEvent> activeEvents;
    private final Map<String, TransactionEvent> duplicateIndex = new HashMap<>();
    private final Map<String, List<TransactionEvent>> accountIndex = new HashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public TemporalWindowEngine(
            @Value("${veloring.temporal.window-size-seconds:60}") long windowSizeSeconds,
            @Value("${veloring.temporal.allowed-lateness-seconds:60}") long allowedLatenessSeconds) {
        this.windowSize = Duration.ofSeconds(windowSizeSeconds);
        this.allowedLateness = Duration.ofSeconds(allowedLatenessSeconds);

        this.activeEvents = new TreeSet<>((e1, e2) -> {
            int timeCompare = e1.getEventTime().compareTo(e2.getEventTime());
            if (timeCompare != 0) {
                return timeCompare;
            }
            return e1.getTransactionId().compareTo(e2.getTransactionId());
        });
    }

    public TemporalAddResult add(TransactionEvent event) {
        lock.writeLock().lock();
        try {
            // 2. check transactionId duplicate
            if (duplicateIndex.containsKey(event.getTransactionId())) {
                return TemporalAddResult.DUPLICATE;
            }

            Instant eventTime = event.getEventTime();

            // 3. determine relationship to current watermark
            if (watermark != null && eventTime.compareTo(watermark) < 0) {
                // 4. evaluate lateness if event is out-of-order
                Instant latenessThreshold = watermark.minus(allowedLateness);
                if (eventTime.compareTo(latenessThreshold) <= 0) {
                    return TemporalAddResult.TOO_LATE;
                }
            }

            // 5. update watermark only when eventTime > current watermark
            if (watermark == null || eventTime.compareTo(watermark) > 0) {
                watermark = eventTime;
            }

            // 6. insert accepted event
            activeEvents.add(event);

            // 7. update all indexes
            duplicateIndex.put(event.getTransactionId(), event);

            accountIndex.computeIfAbsent(event.getSenderAccountId(), k -> new ArrayList<>()).add(event);
            if (!event.getSenderAccountId().equals(event.getReceiverAccountId())) {
                accountIndex.computeIfAbsent(event.getReceiverAccountId(), k -> new ArrayList<>()).add(event);
            }

            // 8. perform synchronous eviction
            evict();

            // 9. return TemporalAddResult
            return TemporalAddResult.ACCEPTED;

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void evict() {
        if (watermark == null) {
            return;
        }

        Instant evictionThreshold = watermark.minus(windowSize);

        // Evict every event satisfying: eventTime <= watermark - WINDOW_SIZE
        Iterator<TransactionEvent> iterator = activeEvents.iterator();
        while (iterator.hasNext()) {
            TransactionEvent event = iterator.next();
            if (event.getEventTime().compareTo(evictionThreshold) <= 0) {
                iterator.remove(); // remove from TreeSet

                // remove transactionId from duplicate index
                duplicateIndex.remove(event.getTransactionId());

                // remove from account index
                removeFromAccountIndex(event.getSenderAccountId(), event);
                if (!event.getSenderAccountId().equals(event.getReceiverAccountId())) {
                    removeFromAccountIndex(event.getReceiverAccountId(), event);
                }
            } else {
                // Since TreeSet is ordered by eventTime, once we find an event > evictionThreshold,
                // we can stop checking further events.
                break;
            }
        }
    }

    private void removeFromAccountIndex(String accountId, TransactionEvent event) {
        List<TransactionEvent> events = accountIndex.get(accountId);
        if (events != null) {
            events.remove(event);
            if (events.isEmpty()) {
                accountIndex.remove(accountId);
            }
        }
    }

    public TemporalSnapshot getSnapshot() {
        lock.readLock().lock();
        try {
            List<TransactionEvent> snapshotEvents = new ArrayList<>(activeEvents);
            Map<String, List<TransactionEvent>> snapshotAccountIndex = new HashMap<>();

            for (Map.Entry<String, List<TransactionEvent>> entry : accountIndex.entrySet()) {
                snapshotAccountIndex.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            return new TemporalSnapshot(watermark, snapshotEvents, snapshotAccountIndex);
        } finally {
            lock.readLock().unlock();
        }
    }
}
