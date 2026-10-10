package com.veloring.engine.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TaskSchedulingConfigTest {

    @Autowired
    private Environment environment;

    @Test
    void testGracefulShutdownConfiguredForScheduler() {
        String awaitTermination = environment.getProperty("spring.task.scheduling.shutdown.await-termination");
        String awaitTerminationPeriod = environment.getProperty("spring.task.scheduling.shutdown.await-termination-period");
        
        assertEquals("true", awaitTermination, "Scheduler graceful shutdown should be enabled");
        assertEquals("30s", awaitTerminationPeriod, "Scheduler graceful shutdown timeout should be configured");
    }
}
