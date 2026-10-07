package com.veloring.generator.scenario;

import com.veloring.generator.config.GeneratorProperties;
import com.veloring.generator.engine.AccountRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioTests {

    private AccountRegistry registry;

    @BeforeEach
    void setUp() {
        GeneratorProperties props = new GeneratorProperties();
        props.setSeed(42L);
        props.setAccountsPoolSize(100);
        registry = new AccountRegistry(props);
    }

    @Test
    void testCycle3Scenario() {
        Cycle3Scenario cycle3 = new Cycle3Scenario();
        Random random = new Random(42L);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        String expId = "EXP-TEST";

        List<Scenario.GeneratedEvent> events = cycle3.generate(now, registry, random, expId);

        assertThat(events).hasSize(3);

        Scenario.GeneratedEvent e1 = events.get(0);
        Scenario.GeneratedEvent e2 = events.get(1);
        Scenario.GeneratedEvent e3 = events.get(2);

        // Verify graph cycle A -> B, B -> C, C -> A
        assertThat(e1.getTransaction().getReceiverAccountId()).isEqualTo(e2.getTransaction().getSenderAccountId());
        assertThat(e2.getTransaction().getReceiverAccountId()).isEqualTo(e3.getTransaction().getSenderAccountId());
        assertThat(e3.getTransaction().getReceiverAccountId()).isEqualTo(e1.getTransaction().getSenderAccountId());

        // Verify Ground Truth Correlation
        assertThat(e1.getTruth().getScenarioId()).isEqualTo(e2.getTruth().getScenarioId());
        assertThat(e2.getTruth().getScenarioId()).isEqualTo(e3.getTruth().getScenarioId());
        
        // Verify Unique Tx IDs
        assertThat(e1.getTransaction().getTransactionId()).isNotEqualTo(e2.getTransaction().getTransactionId());
        assertThat(e1.getTransaction().getTransactionId()).isEqualTo(e1.getTruth().getTransactionId());
        
        // Verify eventTime advances properly
        assertThat(e2.getTransaction().getEventTime()).isAfter(e1.getTransaction().getEventTime());
        assertThat(e3.getTransaction().getEventTime()).isAfter(e2.getTransaction().getEventTime());
        
        // Verify scenarioType and injectedPattern
        assertThat(e1.getTruth().getScenarioType()).isEqualTo("CYCLE_3");
        assertThat(e1.getTruth().isInjectedPattern()).isTrue();
    }

    @Test
    void testFanOutScenario() {
        FanOutScenario fanOut = new FanOutScenario();
        Random random = new Random(42L);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        String expId = "EXP-TEST";

        List<Scenario.GeneratedEvent> events = fanOut.generate(now, registry, random, expId);

        assertThat(events).hasSizeGreaterThanOrEqualTo(3);

        String source = events.get(0).getTransaction().getSenderAccountId();
        String scenarioId = events.get(0).getTruth().getScenarioId();

        for (Scenario.GeneratedEvent event : events) {
            // Verify all have same source A -> B, A -> C, etc.
            assertThat(event.getTransaction().getSenderAccountId()).isEqualTo(source);
            // Verify target is not source
            assertThat(event.getTransaction().getReceiverAccountId()).isNotEqualTo(source);
            // Verify all share same scenarioId
            assertThat(event.getTruth().getScenarioId()).isEqualTo(scenarioId);
            // Verify Ground Truth
            assertThat(event.getTruth().getScenarioType()).isEqualTo("FAN_OUT");
            assertThat(event.getTruth().isInjectedPattern()).isTrue();
            assertThat(event.getTransaction().getTransactionId()).isEqualTo(event.getTruth().getTransactionId());
        }
    }
}
