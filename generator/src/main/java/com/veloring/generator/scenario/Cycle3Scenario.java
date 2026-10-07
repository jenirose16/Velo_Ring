package com.veloring.generator.scenario;

import com.veloring.generator.engine.AccountRegistry;
import com.veloring.generator.model.GroundTruthRecord;
import com.veloring.generator.model.TransactionEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component("cycle3")
public class Cycle3Scenario implements Scenario {
    @Override
    public List<GeneratedEvent> generate(Instant currentTime, AccountRegistry registry, Random random, String experimentId) {
        String a = registry.getRandomAccount(random);
        String b = registry.getRandomAccount(random);
        String c = registry.getRandomAccount(random);

        BigDecimal baseAmount = BigDecimal.valueOf(5000 + random.nextDouble() * 5000).setScale(2, RoundingMode.HALF_UP);
        String scenarioId = "cycle3-" + Long.toHexString(random.nextLong());

        List<GeneratedEvent> events = new ArrayList<>();
        
        // A -> B
        events.add(createEdge(a, b, baseAmount, currentTime, experimentId, scenarioId, random, "CYCLE_3", "HOP_1"));
        
        // B -> C (happens 10-60 mins later)
        Instant time2 = currentTime.plus(10 + random.nextInt(50), ChronoUnit.MINUTES);
        BigDecimal amt2 = baseAmount.subtract(BigDecimal.valueOf(50)); // small fee drop
        events.add(createEdge(b, c, amt2, time2, experimentId, scenarioId, random, "CYCLE_3", "HOP_2"));

        // C -> A (happens 10-60 mins later)
        Instant time3 = time2.plus(10 + random.nextInt(50), ChronoUnit.MINUTES);
        BigDecimal amt3 = amt2.subtract(BigDecimal.valueOf(50));
        events.add(createEdge(c, a, amt3, time3, experimentId, scenarioId, random, "CYCLE_3", "HOP_3"));

        return events;
    }

    private GeneratedEvent createEdge(String sender, String receiver, BigDecimal amount, Instant time, 
                                      String experimentId, String scenarioId, Random random, String type, String role) {
        String txId = "tx-" + type.toLowerCase() + "-" + Long.toHexString(random.nextLong());
        TransactionEvent tx = TransactionEvent.builder()
                .transactionId(txId)
                .senderAccountId(sender)
                .receiverAccountId(receiver)
                .amount(amount)
                .currency("USD")
                .eventTime(time)
                .channel("WEB")
                .build();

        GroundTruthRecord truth = GroundTruthRecord.builder()
                .experimentId(experimentId)
                .scenarioId(scenarioId)
                .scenarioType(type)
                .transactionId(txId)
                .role(role)
                .injectedPattern(true)
                .build();

        return new GeneratedEvent(tx, truth);
    }
}
