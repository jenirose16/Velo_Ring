package com.veloring.engine.detection;

import com.veloring.engine.temporal.TemporalSnapshot;
import com.veloring.engine.temporal.TemporalWindowEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.veloring.engine.event.TransactionEvent;
import io.micrometer.core.instrument.MeterRegistry;

@Component
@Slf4j
public class CycleDetectionScheduler {

    private final TemporalWindowEngine temporalWindowEngine;
    private final ThreeAccountCycleDetector detector;
    private final Duration deduplicationRetention;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    private final Map<Cycle, Instant> reportedCycles = new ConcurrentHashMap<>();

    public CycleDetectionScheduler(
            TemporalWindowEngine temporalWindowEngine,
            ThreeAccountCycleDetector detector,
            long windowSizeSeconds,
            long allowedLatenessSeconds,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.temporalWindowEngine = temporalWindowEngine;
        this.detector = detector;
        // Keep in cache for window + lateness + 60s buffer to strictly bound memory
        this.deduplicationRetention = Duration.ofSeconds(windowSizeSeconds + allowedLatenessSeconds + 60);
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }
    
    // Default constructor for Spring when not testing with specific clock
    @org.springframework.beans.factory.annotation.Autowired
    public CycleDetectionScheduler(
            TemporalWindowEngine temporalWindowEngine,
            ThreeAccountCycleDetector detector,
            @org.springframework.beans.factory.annotation.Value("${veloring.temporal.window-size-seconds:60}") long windowSizeSeconds,
            @org.springframework.beans.factory.annotation.Value("${veloring.temporal.allowed-lateness-seconds:60}") long allowedLatenessSeconds,
            MeterRegistry meterRegistry) {
        this(temporalWindowEngine, detector, windowSizeSeconds, allowedLatenessSeconds, Clock.systemUTC(), meterRegistry);
    }

    @Scheduled(fixedDelayString = "${veloring.detection.fixed-delay-ms:5000}")
    public void detectCycles() {
        try {
            cleanupExpiredDeduplications();

            long startSnapshot = System.nanoTime();
            TemporalSnapshot snapshot;
            try {
                snapshot = temporalWindowEngine.getSnapshot();
            } finally {
                meterRegistry.timer("veloring.detect.snapshot.duration").record(System.nanoTime() - startSnapshot, TimeUnit.NANOSECONDS);
            }

            long startAlgo = System.nanoTime();
            List<Cycle> cycles;
            try {
                cycles = detector.detect(snapshot);
            } finally {
                meterRegistry.timer("veloring.detect.algorithm.duration").record(System.nanoTime() - startAlgo, TimeUnit.NANOSECONDS);
            }
            
            Instant detectionTime = clock.instant();

            long startLogging = System.nanoTime();
            try {
                Map<String, Instant> processedAtMap = null;

                for (Cycle cycle : cycles) {
                    if (reportedCycles.putIfAbsent(cycle, detectionTime) == null) {
                        
                        if (processedAtMap == null) {
                            processedAtMap = snapshot.getAllEvents().stream()
                                .collect(Collectors.toMap(TransactionEvent::getTransactionId, TransactionEvent::getProcessedAt, (e1, e2) -> e1));
                        }
                        
                        Instant t1 = processedAtMap.get(cycle.edge1().transactionId());
                        Instant t2 = processedAtMap.get(cycle.edge2().transactionId());
                        Instant t3 = processedAtMap.get(cycle.edge3().transactionId());
                        
                        if (t1 != null && t2 != null && t3 != null) {
                            Instant maxProcessed = t1;
                            if (t2.compareTo(maxProcessed) > 0) maxProcessed = t2;
                            if (t3.compareTo(maxProcessed) > 0) maxProcessed = t3;
                            
                            Duration delay = Duration.between(maxProcessed, detectionTime);
                            meterRegistry.timer("veloring.detect.e2e.delay").record(delay);
                        }

                        logNovelCycle(cycle, snapshot.getWatermark(), detectionTime);
                    }
                }
            } finally {
                meterRegistry.timer("veloring.detect.logging.duration").record(System.nanoTime() - startLogging, TimeUnit.NANOSECONDS);
            }
        } catch (Exception e) {
            log.error("Cycle detection scheduler encountered an unexpected error. It will recover on the next scheduled run.", e);
        }
    }

    private void cleanupExpiredDeduplications() {
        Instant expiryThreshold = clock.instant().minus(deduplicationRetention);
        Iterator<Map.Entry<Cycle, Instant>> it = reportedCycles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Cycle, Instant> entry = it.next();
            if (entry.getValue().compareTo(expiryThreshold) <= 0) {
                it.remove();
            }
        }
    }

    private void logNovelCycle(Cycle cycle, Instant watermark, Instant detectionTime) {
        String watermarkStr = watermark != null ? watermark.toString() : "N/A";
        log.info("NOVEL_CYCLE_DETECTED: Watermark={}, DetectionTime={}, " +
                        "Edge1=[tx={}, {}->{}], Edge2=[tx={}, {}->{}], Edge3=[tx={}, {}->{}]",
                watermarkStr,
                detectionTime,
                cycle.edge1().transactionId(), cycle.edge1().senderAccountId(), cycle.edge1().receiverAccountId(),
                cycle.edge2().transactionId(), cycle.edge2().senderAccountId(), cycle.edge2().receiverAccountId(),
                cycle.edge3().transactionId(), cycle.edge3().senderAccountId(), cycle.edge3().receiverAccountId()
        );
    }
    
    // For testing
    public int getReportedCyclesCount() {
        return reportedCycles.size();
    }

    // For testing
    public java.util.Set<Cycle> getReportedCycles() {
        return java.util.Collections.unmodifiableSet(reportedCycles.keySet());
    }
}
