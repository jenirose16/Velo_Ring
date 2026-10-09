package com.veloring.engine.detection;

import com.veloring.engine.event.TransactionEvent;

public record Cycle(Edge edge1, Edge edge2, Edge edge3) {

    public record Edge(String transactionId, String senderAccountId, String receiverAccountId) {
        public Edge {
            if (transactionId == null || transactionId.trim().isEmpty()) throw new IllegalArgumentException("transactionId cannot be blank");
            if (senderAccountId == null || senderAccountId.trim().isEmpty()) throw new IllegalArgumentException("senderAccountId cannot be blank");
            if (receiverAccountId == null || receiverAccountId.trim().isEmpty()) throw new IllegalArgumentException("receiverAccountId cannot be blank");
        }

        public static Edge from(TransactionEvent event) {
            if (event == null) throw new IllegalArgumentException("Event cannot be null");
            return new Edge(event.getTransactionId(), event.getSenderAccountId(), event.getReceiverAccountId());
        }
    }

    public Cycle(TransactionEvent e1, TransactionEvent e2, TransactionEvent e3) {
        this(Edge.from(e1), Edge.from(e2), Edge.from(e3));
    }

    public Cycle {
        if (edge1 == null || edge2 == null || edge3 == null) {
            throw new IllegalArgumentException("Edges cannot be null");
        }

        String id1 = edge1.transactionId();
        String id2 = edge2.transactionId();
        String id3 = edge3.transactionId();

        if (id1.compareTo(id2) <= 0 && id1.compareTo(id3) <= 0) {
            // edge1 is minimum, already canonical
        } else if (id2.compareTo(id1) <= 0 && id2.compareTo(id3) <= 0) {
            Edge originalEdge1 = edge1;
            edge1 = edge2;
            edge2 = edge3;
            edge3 = originalEdge1;
        } else {
            Edge originalEdge3 = edge3;
            edge3 = edge2;
            edge2 = edge1;
            edge1 = originalEdge3;
        }

        if (!edge1.receiverAccountId().equals(edge2.senderAccountId()) ||
            !edge2.receiverAccountId().equals(edge3.senderAccountId()) ||
            !edge3.receiverAccountId().equals(edge1.senderAccountId())) {
            throw new IllegalArgumentException("Edges do not form a continuous directed cycle");
        }

        if (edge1.senderAccountId().equals(edge2.senderAccountId()) ||
            edge2.senderAccountId().equals(edge3.senderAccountId()) ||
            edge3.senderAccountId().equals(edge1.senderAccountId())) {
            throw new IllegalArgumentException("Cycle must involve exactly three distinct accounts");
        }
    }
}
