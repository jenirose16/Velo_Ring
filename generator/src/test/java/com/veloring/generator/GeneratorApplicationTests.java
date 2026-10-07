package com.veloring.generator;

import com.veloring.generator.engine.AccountRegistry;
import com.veloring.generator.config.GeneratorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class GeneratorApplicationTests {

    @Test
    void contextLoads() {
        // Just verify context starts properly with embedded kafka
    }

    @Test
    void testDeterminism() {
        GeneratorProperties props1 = new GeneratorProperties();
        props1.setSeed(100L);
        props1.setAccountsPoolSize(50);
        AccountRegistry registry1 = new AccountRegistry(props1);

        GeneratorProperties props2 = new GeneratorProperties();
        props2.setSeed(100L);
        props2.setAccountsPoolSize(50);
        AccountRegistry registry2 = new AccountRegistry(props2);

        Random random1 = new Random(200L);
        Random random2 = new Random(200L);

        // Verify the exact sequence of randomly generated account picks is identical
        for (int i = 0; i < 100; i++) {
            assertThat(registry1.getRandomAccount(random1))
                    .isEqualTo(registry2.getRandomAccount(random2));
        }
    }
}
