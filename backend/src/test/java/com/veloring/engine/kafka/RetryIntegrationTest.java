package com.veloring.engine.kafka;

import com.veloring.engine.event.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

public class RetryIntegrationTest extends AbstractIntegrationTest {

    @MockBean
    private Driver neo4jDriver;

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Test
    void testTransientFailureTriggersBoundedRetry() throws Exception {
        String txId = "tx-retry-" + UUID.randomUUID().toString();
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId("account-1")
                .receiverAccountId("account-2")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("mobile")
                .build();

        Session session = mock(Session.class);
        when(neo4jDriver.session()).thenReturn(session);

        // Fail first two times, succeed on the third
        when(session.executeWrite(org.mockito.ArgumentMatchers.<TransactionCallback<Void>>any()))
            .thenThrow(new TransientException("Neo.TransientError.Network.CommunicationError", "Connection dropped"))
            .thenThrow(new TransientException("Neo.TransientError.Network.CommunicationError", "Connection dropped"))
            .thenReturn(null);

        System.out.println("==================================================");
        System.out.println("RETRY TEST STARTING:");
        System.out.println("Sending event intended to fail transiently twice...");
        
        kafkaTemplate.send(topic, event);

        // Wait for retries (backoff is 1s, so 3 attempts take ~2-3 seconds)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Verify executeWrite was called exactly 3 times
            verify(session, times(3)).executeWrite(org.mockito.ArgumentMatchers.<TransactionCallback<Void>>any());
        });
        
        System.out.println("RETRY TEST RESULT:");
        System.out.println("Neo4j executeWrite was called exactly 3 times due to transient failures.");
        System.out.println("==================================================");
    }
}
