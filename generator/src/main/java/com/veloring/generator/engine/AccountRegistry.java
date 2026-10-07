package com.veloring.generator.engine;

import com.veloring.generator.config.GeneratorProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class AccountRegistry {
    private final List<String> accountIds = new ArrayList<>();
    private final Random random;

    public AccountRegistry(GeneratorProperties properties) {
        // Use a deterministic seed to generate the exact same pool of accounts every time
        this.random = new Random(properties.getSeed());
        for (int i = 0; i < properties.getAccountsPoolSize(); i++) {
            // Generate deterministic UUIDs using seeded random bytes
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            accountIds.add(UUID.nameUUIDFromBytes(bytes).toString());
        }
    }

    public String getRandomAccount(Random scenarioRandom) {
        return accountIds.get(scenarioRandom.nextInt(accountIds.size()));
    }
}
