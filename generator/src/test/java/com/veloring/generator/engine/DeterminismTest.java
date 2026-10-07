package com.veloring.generator.engine;

import com.veloring.generator.config.GeneratorProperties;
import com.veloring.generator.publisher.GroundTruthPublisher;
import com.veloring.generator.publisher.KafkaPublisher;
import com.veloring.generator.scenario.BackgroundScenario;
import com.veloring.generator.scenario.Cycle3Scenario;
import com.veloring.generator.scenario.FanOutScenario;
import com.veloring.generator.scenario.Scenario;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeterminismTest {

    @Test
    void testCompleteSequenceDeterminism() {
        // Run 1 with normal acceleration
        List<Scenario.GeneratedEvent> run1 = generateSequence(10.0);

        // Run 2 with huge acceleration (proving it doesn't affect event generation)
        List<Scenario.GeneratedEvent> run2 = generateSequence(1000.0);

        assertThat(run1).hasSizeGreaterThan(0);
        assertThat(run1).hasSize(run2.size());

        for (int i = 0; i < run1.size(); i++) {
            Scenario.GeneratedEvent e1 = run1.get(i);
            Scenario.GeneratedEvent e2 = run2.get(i);

            // Compare properties
            assertThat(e1.getTransaction().getTransactionId()).isEqualTo(e2.getTransaction().getTransactionId());
            assertThat(e1.getTransaction().getSenderAccountId()).isEqualTo(e2.getTransaction().getSenderAccountId());
            assertThat(e1.getTransaction().getReceiverAccountId()).isEqualTo(e2.getTransaction().getReceiverAccountId());
            assertThat(e1.getTransaction().getAmount()).isEqualTo(e2.getTransaction().getAmount());
            assertThat(e1.getTransaction().getCurrency()).isEqualTo(e2.getTransaction().getCurrency());
            assertThat(e1.getTransaction().getEventTime()).isEqualTo(e2.getTransaction().getEventTime());
            assertThat(e1.getTransaction().getChannel()).isEqualTo(e2.getTransaction().getChannel());

            assertThat(e1.getTruth().getScenarioId()).isEqualTo(e2.getTruth().getScenarioId());
            assertThat(e1.getTruth().getScenarioType()).isEqualTo(e2.getTruth().getScenarioType());
            assertThat(e1.getTruth().getRole()).isEqualTo(e2.getTruth().getRole());
            assertThat(e1.getTruth().isInjectedPattern()).isEqualTo(e2.getTruth().isInjectedPattern());
        }
    }

    private List<Scenario.GeneratedEvent> generateSequence(double accelerationFactor) {
        GeneratorProperties props = new GeneratorProperties();
        props.setSeed(42L);
        props.setStartTime("2026-01-01T00:00:00Z");
        props.setAccelerationFactor(accelerationFactor);
        props.setAccountsPoolSize(100);
        props.setTps(1000000); // effectively infinite TPS for test
        props.setExperimentId("EXP-TEST");

        GeneratorProperties.ScenarioProps bg = new GeneratorProperties.ScenarioProps();
        bg.setWeight(50);
        GeneratorProperties.ScenarioProps cycle3 = new GeneratorProperties.ScenarioProps();
        cycle3.setWeight(25);
        GeneratorProperties.ScenarioProps fanout = new GeneratorProperties.ScenarioProps();
        fanout.setWeight(25);

        props.setScenarios(Map.of("background", bg, "cycle3", cycle3, "fanout", fanout));

        SimulationClock clock = new SimulationClock(props);
        AccountRegistry registry = new AccountRegistry(props);

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(eq("background"), eq(Scenario.class))).thenReturn(new BackgroundScenario());
        when(context.getBean(eq("cycle3"), eq(Scenario.class))).thenReturn(new Cycle3Scenario());
        when(context.getBean(eq("fanout"), eq(Scenario.class))).thenReturn(new FanOutScenario());

        KafkaPublisher kafkaPublisher = mock(KafkaPublisher.class);
        when(kafkaPublisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
        
        GroundTruthPublisher truthPublisher = mock(GroundTruthPublisher.class);
        
        ScenarioEngine engine = new ScenarioEngine(props, clock, registry, kafkaPublisher, truthPublisher, context) {
            @Override
            public void tick() {
                // In test we just want to force generation, so we override to capture events
            }
        };

        engine.init(); // This will replenishQueue and generate 1000 events in the PriorityQueue

        // Since we want both Transaction and Truth, let's pull them directly from eventQueue via reflection or just a simple loop if we expose a method.
        // Actually, let's just make eventQueue accessible for tests?
        // Or better yet, just use the built-in tick() logic by setting playback time into the far future!
        
        // Wait, tick() removes from eventQueue and calls publishers.
        return extractEvents(engine);
    }
    
    private List<Scenario.GeneratedEvent> extractEvents(ScenarioEngine engine) {
        try {
            java.lang.reflect.Field field = ScenarioEngine.class.getDeclaredField("eventQueue");
            field.setAccessible(true);
            java.util.PriorityQueue<Scenario.GeneratedEvent> pq = (java.util.PriorityQueue<Scenario.GeneratedEvent>) field.get(engine);
            
            List<Scenario.GeneratedEvent> list = new ArrayList<>();
            while(!pq.isEmpty()){
                list.add(pq.poll());
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
