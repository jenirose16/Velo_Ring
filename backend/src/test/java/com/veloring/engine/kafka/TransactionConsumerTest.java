package com.veloring.engine.kafka;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.temporal.TemporalWindowEngine;
import com.veloring.engine.temporal.TemporalAddResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.springframework.kafka.support.Acknowledgment;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionConsumerTest {

    @Mock
    private Driver neo4jDriver;
    @Mock
    private Validator validator;
    @Mock
    private TemporalWindowEngine temporalWindowEngine;
    @Mock
    private Acknowledgment acknowledgment;
    @Mock
    private Session session;

    private TransactionConsumer consumer;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        consumer = new TransactionConsumer(neo4jDriver, validator, temporalWindowEngine, meterRegistry);
    }

    private TransactionEvent createValidEvent() {
        return TransactionEvent.builder()
                .transactionId("tx-123")
                .senderAccountId("A")
                .receiverAccountId("B")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .eventTime(Instant.now())
                .channel("WEB")
                .build();
    }

    @Test
    void testValidEventPersistedAndAddedToEngineAndAcknowledged() {
        TransactionEvent event = createValidEvent();

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(neo4jDriver.session()).thenReturn(session);
        when(session.executeWrite(any(TransactionCallback.class))).thenReturn(null);
        when(temporalWindowEngine.add(event)).thenReturn(TemporalAddResult.ACCEPTED);

        consumer.consume(event, acknowledgment);

        verify(session).executeWrite(any());
        verify(temporalWindowEngine).add(event);
        verify(acknowledgment).acknowledge();
        
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.total.duration").timer().count());
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.neo4j.duration").timer().count());
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.temporal.duration").timer().count());
    }

    @Test
    void testInvalidEventAcknowledgedWithoutEngineInsertion() {
        TransactionEvent event = createValidEvent();

        ConstraintViolation<TransactionEvent> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("amount");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be positive");
        
        when(validator.validate(event)).thenReturn(Set.of(violation));

        consumer.consume(event, acknowledgment);

        verify(neo4jDriver, never()).session();
        verify(temporalWindowEngine, never()).add(any());
        verify(acknowledgment).acknowledge();
        
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.total.duration").timer().count());
        org.junit.jupiter.api.Assertions.assertNull(meterRegistry.find("veloring.ingest.neo4j.duration").timer());
        org.junit.jupiter.api.Assertions.assertNull(meterRegistry.find("veloring.ingest.temporal.duration").timer());
    }

    @Test
    void testNeo4jFailurePreventsEngineInsertionAndAcknowledgment() {
        TransactionEvent event = createValidEvent();

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(neo4jDriver.session()).thenReturn(session);
        
        RuntimeException neo4jException = new RuntimeException("Neo4j down");
        when(session.executeWrite(any())).thenThrow(neo4jException);

        try {
            consumer.consume(event, acknowledgment);
        } catch (RuntimeException e) {
            // expected
        }

        verify(temporalWindowEngine, never()).add(any());
        verify(acknowledgment, never()).acknowledge();
        
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.total.duration").timer().count());
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.neo4j.duration").timer().count());
        org.junit.jupiter.api.Assertions.assertNull(meterRegistry.find("veloring.ingest.temporal.duration").timer());
    }

    @Test
    void testEngineInsertionExceptionPreventsAcknowledgment() {
        TransactionEvent event = createValidEvent();

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(neo4jDriver.session()).thenReturn(session);
        when(session.executeWrite(any(TransactionCallback.class))).thenReturn(null);
        
        RuntimeException engineException = new RuntimeException("OOM");
        when(temporalWindowEngine.add(event)).thenThrow(engineException);

        try {
            consumer.consume(event, acknowledgment);
        } catch (RuntimeException e) {
            // expected
        }

        verify(session).executeWrite(any());
        verify(acknowledgment, never()).acknowledge();
        
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.total.duration").timer().count());
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.neo4j.duration").timer().count());
        org.junit.jupiter.api.Assertions.assertEquals(1, meterRegistry.find("veloring.ingest.temporal.duration").timer().count());
    }

    @Test
    void testDuplicateEventHandledCorrectly() {
        TransactionEvent event = createValidEvent();

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(neo4jDriver.session()).thenReturn(session);
        when(session.executeWrite(any(TransactionCallback.class))).thenReturn(null);
        
        when(temporalWindowEngine.add(event)).thenReturn(TemporalAddResult.DUPLICATE);

        consumer.consume(event, acknowledgment);

        verify(session).executeWrite(any());
        verify(temporalWindowEngine).add(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void testTooLateEventHandledCorrectly() {
        TransactionEvent event = createValidEvent();

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(neo4jDriver.session()).thenReturn(session);
        when(session.executeWrite(any(TransactionCallback.class))).thenReturn(null);
        
        when(temporalWindowEngine.add(event)).thenReturn(TemporalAddResult.TOO_LATE);

        consumer.consume(event, acknowledgment);

        verify(session).executeWrite(any());
        verify(temporalWindowEngine).add(event);
        verify(acknowledgment).acknowledge();
    }
}
