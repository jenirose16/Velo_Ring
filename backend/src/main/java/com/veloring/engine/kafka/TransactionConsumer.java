package com.veloring.engine.kafka;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalWindowEngine;
import com.veloring.engine.temporal.TemporalAddResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final Driver neo4jDriver;
    private final Validator validator;
    private final TemporalWindowEngine temporalWindowEngine;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = "${veloring.kafka.topic:payment-events}", groupId = "${veloring.kafka.group-id:veloring-consumer-group}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TransactionEvent event, Acknowledgment acknowledgment) {
        long startTotal = System.nanoTime();
        try {
            event.setProcessedAt(Instant.now());
            log.info("Received transaction event: {}", event.getTransactionId());

        // 1. Validation (Schema/Data validation)
        Set<ConstraintViolation<TransactionEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String errorMsg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            log.error("Validation failed for event {}: {}", event.getTransactionId(), errorMsg);
            // Discard invalid event, ack so it doesn't block
            acknowledgment.acknowledge();
            return;
        }

        // 2. Programmatic integrity checks (not fraud detection)
        if (event.getSenderAccountId().equals(event.getReceiverAccountId())) {
            log.error("Data integrity failure: sender == receiver for event {}", event.getTransactionId());
            // Discard invalid event, ack so it doesn't block
            acknowledgment.acknowledge();
            return;
        }

        // 3. Neo4j Persistence
        try (Session session = neo4jDriver.session()) {
            String query = """
                MERGE (sender:Account {id: $senderId})
                MERGE (receiver:Account {id: $receiverId})
                MERGE (sender)-[t:TRANSFERRED_TO {transactionId: $transactionId}]->(receiver)
                ON CREATE SET
                    t.amount = $amount,
                    t.currency = $currency,
                    t.eventTime = $eventTime,
                    t.processedAt = $processedAt,
                    t.channel = $channel
            """;
            
            long startNeo4j = System.nanoTime();
            try {
                session.executeWrite(tx -> {
                    tx.run(query, 
                        org.neo4j.driver.Values.parameters(
                        "senderId", event.getSenderAccountId(),
                        "receiverId", event.getReceiverAccountId(),
                        "transactionId", event.getTransactionId(),
                        "amount", event.getAmount().doubleValue(),
                        "currency", event.getCurrency(),
                        "eventTime", event.getEventTime().toString(),
                        "processedAt", event.getProcessedAt().toString(),
                        "channel", event.getChannel()
                    )
                );
                return null;
                });
            } finally {
                meterRegistry.timer("veloring.ingest.neo4j.duration").record(System.nanoTime() - startNeo4j, TimeUnit.NANOSECONDS);
            }
            log.info("Persisted transaction {} to graph", event.getTransactionId());

            // 4. In-Memory Temporal Engine Insertion
            long startTemporal = System.nanoTime();
            TemporalAddResult engineResult;
            try {
                engineResult = temporalWindowEngine.add(event);
            } finally {
                meterRegistry.timer("veloring.ingest.temporal.duration").record(System.nanoTime() - startTemporal, TimeUnit.NANOSECONDS);
            }
            
            if (engineResult == TemporalAddResult.ACCEPTED) {
                log.info("Added transaction {} to temporal window", event.getTransactionId());
            } else if (engineResult == TemporalAddResult.DUPLICATE) {
                log.warn("Transaction {} ignored by temporal window: Duplicate", event.getTransactionId());
            } else if (engineResult == TemporalAddResult.TOO_LATE) {
                log.warn("Transaction {} ignored by temporal window: Too Late (outside allowed lateness)", event.getTransactionId());
            }

            // 5. Acknowledge message processing on successful write and engine add
            acknowledgment.acknowledge();
        }
        // Exceptions propagate to ErrorHandler (DefaultErrorHandler with BackOff)
        } finally {
            meterRegistry.timer("veloring.ingest.total.duration").record(System.nanoTime() - startTotal, TimeUnit.NANOSECONDS);
        }
    }
}
