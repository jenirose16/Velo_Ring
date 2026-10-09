package com.veloring.engine.temporal;

import com.veloring.engine.event.TransactionEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TemporalSnapshot {
    private final Instant watermark;
    private final List<TransactionEvent> allEvents;
    private final Map<String, List<TransactionEvent>> accountIndex;

    public TemporalSnapshot(Instant watermark, List<TransactionEvent> allEvents, Map<String, List<TransactionEvent>> accountIndex) {
        this.watermark = watermark;
        this.allEvents = Collections.unmodifiableList(allEvents);
        this.accountIndex = Collections.unmodifiableMap(accountIndex);
    }

    public Instant getWatermark() {
        return watermark;
    }

    public List<TransactionEvent> getAllEvents() {
        return allEvents;
    }

    public List<TransactionEvent> getEventsForAccount(String accountId) {
        return accountIndex.getOrDefault(accountId, Collections.emptyList());
    }
}
