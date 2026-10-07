package com.veloring.generator.publisher;

import com.veloring.generator.model.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPublisher {
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public CompletableFuture<Void> publish(TransactionEvent event) {
        // Publish asynchronously, using sender account ID as routing key to guarantee partition ordering per account
        return kafkaTemplate.send("payment-events", event.getSenderAccountId(), event)
                .thenAccept(result -> {
                    // Success, nothing to do. The simulation time is independent of this async callback.
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish transaction {}", event.getTransactionId(), ex);
                    return null;
                });
    }
}
