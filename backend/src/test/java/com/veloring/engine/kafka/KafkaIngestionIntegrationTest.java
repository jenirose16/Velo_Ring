package com.veloring.engine.kafka;

import com.veloring.engine.event.TransactionEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "veloring.neo4j.uri=bolt://localhost:7687",
    "veloring.neo4j.username=neo4j",
    "veloring.neo4j.password=verysecurepassword"
})
public class KafkaIngestionIntegrationTest {

    @Autowired
    private Driver neo4jDriver;

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Test
    void testEndToEndValidEvent() throws InterruptedException {
        String txId = "tx-valid-" + UUID.randomUUID().toString();
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId("account-1")
                .receiverAccountId("account-2")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();

        kafkaTemplate.send("payment-events", event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            try (Session session = neo4jDriver.session()) {
                Result result = session.run("MATCH (a)-[t:TRANSFERRED_TO {transactionId: $txId}]->(b) RETURN t.processedAt AS processedAt, t.eventTime AS eventTime", 
                    org.neo4j.driver.Values.parameters("txId", txId));
                assertTrue(result.hasNext(), "Transaction should be in neo4j");
                Record record = result.next();
                
                assertNotNull(record.get("processedAt").asString());
                assertEquals(event.getEventTime().toString(), record.get("eventTime").asString());
                
                System.out.println("==================================================");
                System.out.println("E2E TEST RESULT:");
                System.out.println("Transaction ID: " + txId);
                System.out.println("Event Time (from generator): " + record.get("eventTime").asString());
                System.out.println("Processed At (assigned by backend): " + record.get("processedAt").asString());
                System.out.println("==================================================");
            }
        });
    }
    
    @Test
    void testPoisonPillDoesNotBlock() throws InterruptedException {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("payment-events", "bad-key", "{ bad json: "));
        }
        
        String txId = "tx-after-poison-" + UUID.randomUUID().toString();
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId("account-3")
                .receiverAccountId("account-4")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();
                
        kafkaTemplate.send("payment-events", event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            try (Session session = neo4jDriver.session()) {
                Result result = session.run("MATCH (a)-[t:TRANSFERRED_TO {transactionId: $txId}]->(b) RETURN t", 
                    org.neo4j.driver.Values.parameters("txId", txId));
                assertTrue(result.hasNext(), "Valid transaction should be processed after poison pill");
                
                System.out.println("==================================================");
                System.out.println("POISON PILL TEST RESULT:");
                System.out.println("Successfully bypassed malformed JSON.");
                System.out.println("Successfully processed subsequent valid transaction: " + txId);
                System.out.println("==================================================");
            }
        });
    }
    
    @Test
    void testInvalidEventValidation() throws InterruptedException {
        String txId = "tx-invalid-" + UUID.randomUUID().toString();
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId("account-5")
                .receiverAccountId("account-6")
                // missing amount (violates @NotNull)
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();
                
        kafkaTemplate.send("payment-events", event);

        String sentinelId = "tx-sentinel-" + UUID.randomUUID().toString();
        TransactionEvent sentinelEvent = TransactionEvent.builder()
                .transactionId(sentinelId)
                .senderAccountId("account-sentinel-1")
                .receiverAccountId("account-sentinel-2")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();
        
        kafkaTemplate.send("payment-events", sentinelEvent);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            try (Session session = neo4jDriver.session()) {
                Result sentinelResult = session.run("MATCH (a)-[t:TRANSFERRED_TO {transactionId: $txId}]->(b) RETURN t", 
                    org.neo4j.driver.Values.parameters("txId", sentinelId));
                assertTrue(sentinelResult.hasNext(), "Sentinel transaction should be in graph");
                
                Result invalidResult = session.run("MATCH (a)-[t:TRANSFERRED_TO {transactionId: $txId}]->(b) RETURN t", 
                    org.neo4j.driver.Values.parameters("txId", txId));
                assertFalse(invalidResult.hasNext(), "Invalid transaction should NOT be in graph");
                
                System.out.println("==================================================");
                System.out.println("VALIDATION TEST RESULT:");
                System.out.println("Invalid event " + txId + " was correctly rejected (log + acknowledge + discard).");
                System.out.println("==================================================");
            }
        });
    }
    
    @Test
    void testDataIntegritySenderEqualsReceiver() throws InterruptedException {
        String txId = "tx-integrity-" + UUID.randomUUID().toString();
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId("account-self")
                .receiverAccountId("account-self")
                .amount(new BigDecimal("20.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();
                
        kafkaTemplate.send("payment-events", event);

        String sentinelId = "tx-sentinel-" + UUID.randomUUID().toString();
        TransactionEvent sentinelEvent = TransactionEvent.builder()
                .transactionId(sentinelId)
                .senderAccountId("account-sentinel-3")
                .receiverAccountId("account-sentinel-4")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();
                
        kafkaTemplate.send("payment-events", sentinelEvent);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            try (Session session = neo4jDriver.session()) {
                Result sentinelResult = session.run("MATCH (a)-[t:TRANSFERRED_TO {transactionId: $txId}]->(b) RETURN t", 
                    org.neo4j.driver.Values.parameters("txId", sentinelId));
                assertTrue(sentinelResult.hasNext(), "Sentinel transaction should be in graph");

                Result invalidResult = session.run("MATCH (a)-[t:TRANSFERRED_TO {transactionId: $txId}]->(b) RETURN t", 
                    org.neo4j.driver.Values.parameters("txId", txId));
                assertFalse(invalidResult.hasNext(), "Self-transfer transaction should NOT be in graph");
                
                System.out.println("==================================================");
                System.out.println("DATA INTEGRITY TEST RESULT:");
                System.out.println("Self-transfer event " + txId + " was correctly rejected (log + acknowledge + discard).");
                System.out.println("==================================================");
            }
        });
    }
    
    @Test
    void testDuplicateDeliveryCreatesOnlyOneRelationship() throws InterruptedException {
        String txId = "tx-duplicate-" + UUID.randomUUID().toString();
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId("account-7")
                .receiverAccountId("account-8")
                .amount(new BigDecimal("75.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();

        System.out.println("==================================================");
        System.out.println("IDEMPOTENCY TEST STARTING:");
        System.out.println("Sending delivery 1: " + txId);
        kafkaTemplate.send("payment-events", event);
        
        System.out.println("Sending delivery 2: " + txId);
        kafkaTemplate.send("payment-events", event); 

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            try (Session session = neo4jDriver.session()) {
                Result result = session.run("MATCH (a)-[t:TRANSFERRED_TO {transactionId: $txId}]->(b) RETURN count(t) AS c", 
                    org.neo4j.driver.Values.parameters("txId", txId));
                assertTrue(result.hasNext());
                int count = result.next().get("c").asInt();
                assertEquals(1, count, "Should only have 1 relationship despite duplicate delivery");
                
                System.out.println("IDEMPOTENCY TEST RESULT:");
                System.out.println("Transaction ID: " + txId);
                System.out.println("Total graph relationships created: " + count);
                System.out.println("==================================================");
            }
        });
    }
}
