package com.veloring.generator.engine;

import com.veloring.generator.config.GeneratorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Component
@Slf4j
public class SimulationClock {
    private final Instant startTime;
    private final double accelerationFactor;
    private final long realStartMillis;
    private Instant deterministicScheduleTime;

    public SimulationClock(GeneratorProperties properties) {
        this.startTime = Instant.parse(properties.getStartTime());
        this.deterministicScheduleTime = this.startTime;
        this.accelerationFactor = properties.getAccelerationFactor();
        this.realStartMillis = System.currentTimeMillis();
        log.info("Simulation clock initialized: startTime={}, accelerationFactor={}", startTime, accelerationFactor);
    }

    /**
     * Used by ScenarioEngine to get a perfectly deterministic schedule time for the next scenario.
     * Advances the clock by a random deterministic interval (e.g. 0 to 2 seconds).
     */
    public Instant nextDeterministicTime(Random random) {
        // Advance by up to 2 seconds deterministically
        deterministicScheduleTime = deterministicScheduleTime.plusMillis(random.nextInt(2000));
        return deterministicScheduleTime;
    }

    /**
     * Calculates the playback time based on how much real time has passed,
     * scaled by the acceleration factor. Used ONLY to gate event release, NEVER for event generation.
     */
    public Instant playbackNow() {
        long elapsedRealMillis = System.currentTimeMillis() - realStartMillis;
        long elapsedSimulatedMillis = (long) (elapsedRealMillis * accelerationFactor);
        return startTime.plus(elapsedSimulatedMillis, ChronoUnit.MILLIS);
    }
}
