package com.veloring.engine.kafka;

import com.veloring.engine.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final Driver neo4jDriver;

    @KafkaListener(topics = "payment-events", groupId = "veloring-consumer-group")
    public void consume(TransactionEvent event) {
        event.setProcessedAt(Instant.now());
        log.info("Received transaction event: {}", event.getTransactionId());
        
        // Minimal temporal graph persistence stub for Phase 1
        try (Session session = neo4jDriver.session()) {
            String query = """
                MERGE (sender:Account {id: $senderId})
                MERGE (receiver:Account {id: $receiverId})
                CREATE (sender)-[t:TRANSFERRED_TO {
                    transactionId: $transactionId,
                    amount: $amount,
                    currency: $currency,
                    eventTime: $eventTime,
                    processedAt: $processedAt,
                    channel: $channel
                }]->(receiver)
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
        } catch (Exception e) {
            log.error("Error persisting transaction to graph", e);
        }
    }
}
