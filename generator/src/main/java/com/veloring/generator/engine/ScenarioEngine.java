package com.veloring.generator.engine;

import com.veloring.generator.config.GeneratorProperties;
import com.veloring.generator.publisher.GroundTruthPublisher;
import com.veloring.generator.publisher.KafkaPublisher;
import com.veloring.generator.scenario.Scenario;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioEngine {
    private final GeneratorProperties properties;
    private final SimulationClock clock;
    private final AccountRegistry registry;
    private final KafkaPublisher kafkaPublisher;
    private final GroundTruthPublisher truthPublisher;
    private final ApplicationContext context;

    private final PriorityQueue<Scenario.GeneratedEvent> eventQueue = new PriorityQueue<>(
            Comparator.comparing(e -> e.getTransaction().getEventTime())
    );

    private final List<ScenarioSelector> selectors = new ArrayList<>();
    private Random engineRandom;
    private int totalWeight = 0;
    
    // For Rate Limiting
    private long lastPublishTime = System.currentTimeMillis();
    private double tokens = 0;

    @PostConstruct
    public void init() {
        engineRandom = new Random(properties.getSeed());
        
        properties.getScenarios().forEach((name, props) -> {
            Scenario scenario = context.getBean(name, Scenario.class);
            selectors.add(new ScenarioSelector(scenario, props.getWeight()));
            totalWeight += props.getWeight();
            log.info("Registered scenario: {} with weight {}", name, props.getWeight());
        });
        
        // Pre-fill queue
        replenishQueue();
    }

    private void replenishQueue() {
        while (eventQueue.size() < 1000) {
            int roll = engineRandom.nextInt(totalWeight);
            int current = 0;
            Scenario selected = null;
            for (ScenarioSelector sel : selectors) {
                current += sel.weight;
                if (roll < current) {
                    selected = sel.scenario;
                    break;
                }
            }
            if (selected != null) {
                List<Scenario.GeneratedEvent> newEvents = selected.generate(
                        clock.nextDeterministicTime(engineRandom), 
                        registry, 
                        engineRandom, 
                        properties.getExperimentId());
                eventQueue.addAll(newEvents);
            }
        }
    }

    // Runs every 10ms to push events to Kafka if simulated time has reached them
    @Scheduled(fixedRate = 10)
    public void tick() {
        long now = System.currentTimeMillis();
        double elapsedSeconds = (now - lastPublishTime) / 1000.0;
        lastPublishTime = now;
        
        // Add tokens based on TPS limit
        tokens += elapsedSeconds * properties.getTps();
        if (tokens > properties.getTps()) {
            tokens = properties.getTps(); // Cap at max burst
        }

        while (!eventQueue.isEmpty() && eventQueue.peek().getTransaction().getEventTime().isBefore(clock.playbackNow())) {
            if (tokens >= 1.0) {
                Scenario.GeneratedEvent event = eventQueue.poll();
                tokens -= 1.0;
                
                kafkaPublisher.publish(event.getTransaction());
                truthPublisher.publish(event.getTruth());
            } else {
                // Rate limited, wait for next tick
                break;
            }
        }

        if (eventQueue.size() < 500) {
            replenishQueue();
        }
    }

    private record ScenarioSelector(Scenario scenario, int weight) {}
}
