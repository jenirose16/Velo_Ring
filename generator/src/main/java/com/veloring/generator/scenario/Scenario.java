package com.veloring.generator.scenario;

import com.veloring.generator.engine.AccountRegistry;
import com.veloring.generator.model.GroundTruthRecord;
import com.veloring.generator.model.TransactionEvent;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Random;

public interface Scenario {
    List<GeneratedEvent> generate(Instant currentTime, AccountRegistry registry, Random random, String experimentId);

    @Data
    class GeneratedEvent {
        private final TransactionEvent transaction;
        private final GroundTruthRecord truth;
    }
}
