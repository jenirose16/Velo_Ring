package com.veloring.engine.kafka;

import com.veloring.engine.event.TransactionEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.veloring.engine.detection.Cycle;
import com.veloring.engine.detection.CycleDetectionScheduler;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.awaitility.Awaitility;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
    "management.server.port=0",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "veloring.detection.fixed-delay-ms=500",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@Testcontainers
public class EndToEndIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Container
    static final Neo4jContainer<?> neo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.9.0"))
            .withoutAuthentication();

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("veloring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("veloring.neo4j.username", () -> "");
        registry.add("veloring.neo4j.password", () -> "");
        registry.add("management.health.neo4j.enabled", () -> "false");
        registry.add("management.health.kafka.enabled", () -> "false");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private Driver neo4jDriver;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private CycleDetectionScheduler scheduler;

    @Test
    void testEndToEndCycleDetection() throws Exception {
        String tx1 = "tx-" + UUID.randomUUID();
        String tx2 = "tx-" + UUID.randomUUID();
        String tx3 = "tx-" + UUID.randomUUID();
        
        var initialTimer = meterRegistry.find("veloring.detect.e2e.delay").timer();
        long initialCount = initialTimer != null ? initialTimer.count() : 0;

        Instant baseTime = Instant.now();

        TransactionEvent e1 = TransactionEvent.builder()
                .transactionId(tx1)
                .senderAccountId("A")
                .receiverAccountId("B")
                .amount(new BigDecimal("100"))
                .currency("USD")
                .eventTime(baseTime)
                .channel("WEB")
                .build();
                
        TransactionEvent e2 = TransactionEvent.builder()
                .transactionId(tx2)
                .senderAccountId("B")
                .receiverAccountId("C")
                .amount(new BigDecimal("100"))
                .currency("USD")
                .eventTime(baseTime.plusSeconds(1))
                .channel("WEB")
                .build();
                
        TransactionEvent e3 = TransactionEvent.builder()
                .transactionId(tx3)
                .senderAccountId("C")
                .receiverAccountId("A")
                .amount(new BigDecimal("100"))
                .currency("USD")
                .eventTime(baseTime.plusSeconds(2))
                .channel("WEB")
                .build();

        // 1. Produce events to Kafka
        String topic = "payment-events";
        kafkaTemplate.send(topic, e1.getTransactionId(), e1).get();
        kafkaTemplate.send(topic, e2.getTransactionId(), e2).get();
        kafkaTemplate.send(topic, e3.getTransactionId(), e3).get();

        // 2. Wait for Neo4j persistence
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            try (Session session = neo4jDriver.session()) {
                long c1 = session.run("MATCH (a:Account {id: 'A'})-[r:TRANSFERRED_TO {transactionId: $id}]->(b:Account {id: 'B'}) RETURN count(r)", org.neo4j.driver.Values.parameters("id", tx1)).single().get(0).asLong();
                long c2 = session.run("MATCH (a:Account {id: 'B'})-[r:TRANSFERRED_TO {transactionId: $id}]->(b:Account {id: 'C'}) RETURN count(r)", org.neo4j.driver.Values.parameters("id", tx2)).single().get(0).asLong();
                long c3 = session.run("MATCH (a:Account {id: 'C'})-[r:TRANSFERRED_TO {transactionId: $id}]->(b:Account {id: 'A'}) RETURN count(r)", org.neo4j.driver.Values.parameters("id", tx3)).single().get(0).asLong();
                assertEquals(1L, c1, "Transaction 1 A->B missing");
                assertEquals(1L, c2, "Transaction 2 B->C missing");
                assertEquals(1L, c3, "Transaction 3 C->A missing");
            }
        });

        // 3. Wait for Cycle Detection Metric and specific cycle
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var timer = meterRegistry.find("veloring.detect.e2e.delay").timer();
            assertTrue(timer != null && timer.count() >= initialCount + 1, "Expected cycle detection E2E metric to be recorded");

            Cycle expectedCycle = new Cycle(e1, e2, e3);
            assertTrue(scheduler.getReportedCycles().contains(expectedCycle), "Expected exactly the injected cycle to be detected");
        });
    }
}
