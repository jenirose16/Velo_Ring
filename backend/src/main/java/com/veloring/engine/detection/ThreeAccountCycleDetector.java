package com.veloring.engine.detection;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalSnapshot;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ThreeAccountCycleDetector {

    public List<Cycle> detect(TemporalSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot cannot be null");
        }

        List<TransactionEvent> events = snapshot.getAllEvents();
        if (events == null) {
            throw new IllegalArgumentException("Snapshot events cannot be null");
        }

        Set<String> seenTxIds = new HashSet<>();
        Map<String, List<TransactionEvent>> outgoingEdges = new HashMap<>();

        // 1. Validation and Adjacency List Construction
        for (TransactionEvent event : events) {
            if (event == null) {
                throw new IllegalArgumentException("Snapshot contains null event");
            }

            String txId = event.getTransactionId();
            String senderId = event.getSenderAccountId();
            String receiverId = event.getReceiverAccountId();

            if (txId == null || txId.trim().isEmpty()) {
                throw new IllegalArgumentException("Malformed event: transactionId is null or blank");
            }
            if (senderId == null || senderId.trim().isEmpty()) {
                throw new IllegalArgumentException("Malformed event: senderAccountId is null or blank");
            }
            if (receiverId == null || receiverId.trim().isEmpty()) {
                throw new IllegalArgumentException("Malformed event: receiverAccountId is null or blank");
            }

            if (!seenTxIds.add(txId)) {
                throw new IllegalArgumentException("Duplicate transactionId found in snapshot: " + txId);
            }

            // Skip self-loops
            if (!senderId.equals(receiverId)) {
                outgoingEdges.computeIfAbsent(senderId, k -> new ArrayList<>()).add(event);
            }
        }

        Set<Cycle> cycles = new HashSet<>();

        // 2. Cycle Detection
        for (TransactionEvent e1 : events) {
            String a = e1.getSenderAccountId();
            String b = e1.getReceiverAccountId();

            if (a.equals(b)) {
                continue; // self-loop
            }

            List<TransactionEvent> bOut = outgoingEdges.getOrDefault(b, Collections.emptyList());
            for (TransactionEvent e2 : bOut) {
                String c = e2.getReceiverAccountId();

                // Must be distinct accounts (A != B != C)
                if (c.equals(a) || c.equals(b)) {
                    continue;
                }

                List<TransactionEvent> cOut = outgoingEdges.getOrDefault(c, Collections.emptyList());
                for (TransactionEvent e3 : cOut) {
                    if (e3.getReceiverAccountId().equals(a)) {
                        cycles.add(new Cycle(e1, e2, e3));
                    }
                }
            }
        }

        // 3. Sorting
        return cycles.stream()
                .sorted(Comparator.comparing((Cycle c) -> c.edge1().transactionId())
                        .thenComparing(c -> c.edge2().transactionId())
                        .thenComparing(c -> c.edge3().transactionId()))
                .collect(Collectors.toList());
    }
}
