package com.veloring.engine.evaluation;

import com.veloring.engine.detection.Cycle;
import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalSnapshot;

import java.util.*;

public class BruteForceCycleOracle {

    public List<Cycle> detect(TemporalSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("Snapshot cannot be null");

        List<TransactionEvent> events = snapshot.getAllEvents();
        Set<Cycle> uniqueCycles = new HashSet<>();

        int n = events.size();
        for (int i = 0; i < n; i++) {
            TransactionEvent e1 = events.get(i);
            if (e1 == null) throw new IllegalArgumentException("Snapshot contains null event");

            // Check blank IDs early
            if (e1.getTransactionId() == null || e1.getTransactionId().trim().isEmpty()) {
                throw new IllegalArgumentException("Malformed event: transactionId is null or blank");
            }

            for (int j = 0; j < n; j++) {
                TransactionEvent e2 = events.get(j);
                if (!e1.getReceiverAccountId().equals(e2.getSenderAccountId())) continue;

                for (int k = 0; k < n; k++) {
                    TransactionEvent e3 = events.get(k);

                    if (!e2.getReceiverAccountId().equals(e3.getSenderAccountId())) continue;
                    if (!e3.getReceiverAccountId().equals(e1.getSenderAccountId())) continue;

                    if (e1.getSenderAccountId().equals(e2.getSenderAccountId()) ||
                        e2.getSenderAccountId().equals(e3.getSenderAccountId()) ||
                        e3.getSenderAccountId().equals(e1.getSenderAccountId())) {
                        continue;
                    }

                    uniqueCycles.add(new Cycle(e1, e2, e3));
                }
            }
        }

        List<Cycle> result = new ArrayList<>(uniqueCycles);
        result.sort(Comparator.comparing((Cycle c) -> c.edge1().transactionId())
                .thenComparing(c -> c.edge2().transactionId())
                .thenComparing(c -> c.edge3().transactionId()));
        return result;
    }
}
