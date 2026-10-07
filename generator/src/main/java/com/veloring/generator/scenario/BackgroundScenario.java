package com.veloring.generator.scenario;

import com.veloring.generator.engine.AccountRegistry;
import com.veloring.generator.model.GroundTruthRecord;
import com.veloring.generator.model.TransactionEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component("background")
public class BackgroundScenario implements Scenario {
    @Override
    public List<GeneratedEvent> generate(Instant currentTime, AccountRegistry registry, Random random, String experimentId) {
        String sender = registry.getRandomAccount(random);
        String receiver = registry.getRandomAccount(random);
        while (receiver.equals(sender)) {
            receiver = registry.getRandomAccount(random);
        }

        BigDecimal amount = BigDecimal.valueOf(10 + random.nextDouble() * 1000).setScale(2, RoundingMode.HALF_UP);
        String txId = "tx-" + UUID.randomUUID().toString();
        // For determinism, we should use a seeded UUID or sequence. 
        // Using a combination of time and random allows uniqueness but retains deterministic seed properties if the random is seeded.
        // Actually, replacing UUID with a deterministic string based on random:
        txId = "tx-bg-" + Long.toHexString(random.nextLong());
        
        String scenarioId = "bg-" + Long.toHexString(random.nextLong());

        TransactionEvent tx = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId(sender)
                .receiverAccountId(receiver)
                .amount(amount)
                .currency("USD")
                .eventTime(currentTime)
                .channel("MOBILE")
                .build();

        GroundTruthRecord truth = GroundTruthRecord.builder()
                .experimentId(experimentId)
                .scenarioId(scenarioId)
                .scenarioType("BACKGROUND")
                .transactionId(txId)
                .role("BACKGROUND_TRANSFER")
                .injectedPattern(false)
                .build();

        return Collections.singletonList(new GeneratedEvent(tx, truth));
    }
}
