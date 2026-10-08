package com.veloring.engine.kafka;

import com.veloring.engine.event.TransactionEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final Driver neo4jDriver;
    private final Validator validator;

    @KafkaListener(topics = "payment-events", groupId = "veloring-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TransactionEvent event, Acknowledgment acknowledgment) {
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
            log.info("Persisted transaction {} to graph", event.getTransactionId());

            // 4. Acknowledge message processing on successful write
            acknowledgment.acknowledge();
        }
        // Exceptions propagate to ErrorHandler (DefaultErrorHandler with BackOff)
    }
}
