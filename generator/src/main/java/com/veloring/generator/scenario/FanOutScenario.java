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

@Component("fanout")
public class FanOutScenario implements Scenario {
    @Override
    public List<GeneratedEvent> generate(Instant currentTime, AccountRegistry registry, Random random, String experimentId) {
        String source = registry.getRandomAccount(random);
        int numTargets = 3 + random.nextInt(5); // 3 to 7 targets

        String scenarioId = "fanout-" + Long.toHexString(random.nextLong());
        List<GeneratedEvent> events = new ArrayList<>();

        for (int i = 0; i < numTargets; i++) {
            String target = registry.getRandomAccount(random);
            while (target.equals(source)) {
                target = registry.getRandomAccount(random);
            }

            BigDecimal amount = BigDecimal.valueOf(100 + random.nextDouble() * 500).setScale(2, RoundingMode.HALF_UP);
            Instant time = currentTime.plus(random.nextInt(60), ChronoUnit.SECONDS); // all within a minute
            
            String txId = "tx-fanout-" + Long.toHexString(random.nextLong());
            
            TransactionEvent tx = TransactionEvent.builder()
                    .transactionId(txId)
                    .senderAccountId(source)
                    .receiverAccountId(target)
                    .amount(amount)
                    .currency("USD")
                    .eventTime(time)
                    .channel("MOBILE")
                    .build();

            GroundTruthRecord truth = GroundTruthRecord.builder()
                    .experimentId(experimentId)
                    .scenarioId(scenarioId)
                    .scenarioType("FAN_OUT")
                    .transactionId(txId)
                    .role(i == 0 ? "SOURCE" : "TARGET") // simplified role
                    .injectedPattern(true)
                    .build();

            events.add(new GeneratedEvent(tx, truth));
        }

        return events;
    }
}
