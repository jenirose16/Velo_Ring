package com.veloring.engine.evaluation.benchmark;

import com.veloring.engine.temporal.TemporalWindowEngine;
import com.veloring.engine.temporal.TemporalSnapshot;
import com.veloring.engine.temporal.TemporalAddResult;
import com.veloring.engine.detection.ThreeAccountCycleDetector;
import com.veloring.engine.detection.Cycle;
import com.veloring.engine.evaluation.GroundTruthEvaluator;
import com.veloring.engine.evaluation.CycleMatchResult;
import com.veloring.engine.event.TransactionEvent;

import org.junit.jupiter.api.Test;
import java.nio.file.Paths;
import java.util.*;

public class DetectionBenchmark {

    @Test
    public void runBenchmarks() throws Exception {
        System.out.println("Starting Benchmark...");
        List<BenchmarkMetrics> results = new ArrayList<>();

        // E1A - Final Window
        System.out.println("Running E1A-FinalWindow");
        results.add(runExperiment("E1A-FinalWindow", 42, 10000, 50, 60, 60, 0, 0, true));

        CsvReporter.write(Paths.get("target/veloring-benchmarks/results.csv"), results);
        System.out.println("Benchmark completed. Results written to target/veloring-benchmarks/results.csv");
    }

    @Test
    public void runWindowSensitivityExperiment() throws Exception {
        System.out.println("Starting Window Sensitivity Experiment...");
        List<BenchmarkMetrics> results = new ArrayList<>();

        int[] windowSizes = {10, 30, 60, 300};
        for (int win : windowSizes) {
            System.out.println("Running E2-Win" + win);
            results.add(runExperiment("E2-Win" + win, 42, 10000, 50, win, 60, 0, 0, false));
        }

        CsvReporter.write(Paths.get("target/veloring-benchmarks/window_sensitivity.csv"), results);
        System.out.println("Experiment completed. Results written to target/veloring-benchmarks/window_sensitivity.csv");
    }

    private BenchmarkMetrics runExperiment(String expId, long seed, int totalTx, int cycles,
                                           long winSec, long lateSec, int oooCount, long maxLateMs, boolean finalWindowOnly) {
        BenchmarkFixtureGenerator.Config config = new BenchmarkFixtureGenerator.Config();
        config.seed = seed;
        config.totalTransactions = totalTx;
        config.cycleCount = cycles;
        config.eventSpacingMs = 10;
        config.outOfOrderCount = oooCount;
        config.maxLatenessMs = maxLateMs;
        config.finalWindowOnly = finalWindowOnly;
        config.windowSizeSec = winSec;
        config.allowedLatenessSec = lateSec;


        int warmup = 10;
        int measured = 100;

        List<Long> engineTimes = new ArrayList<>();
        List<Long> snapshotTimes = new ArrayList<>();
        List<Long> detectTimes = new ArrayList<>();
        List<Long> combinedTimes = new ArrayList<>();

        int attemptedCount = 0;
        int acceptedCount = 0;
        int rejectedCount = 0;
        int generatedCyclesCount = 0;
        long evaluatedSeed = seed;
        CycleMatchResult finalQuality = null;
        int snapshotTxCount = 0;
        TemporalSnapshot finalSnap = null;
        String fingerprint = "";

        for (int i = 0; i < warmup + measured; i++) {
            // Generate a fresh workload per iteration to avoid artificial JIT branch-prediction bias.
            // This tests workload variability robustness rather than hot-path identical fixture optimization.
            config.seed = seed + i;
            BenchmarkWorkload workload = BenchmarkFixtureGenerator.generate(config);
            attemptedCount = workload.events().size();
            generatedCyclesCount = workload.groundTruth().size() / 3;
            TemporalWindowEngine engine = new TemporalWindowEngine(winSec, lateSec);
            ThreeAccountCycleDetector detector = new ThreeAccountCycleDetector();

            long runEngineNanos = 0;
            int accepted = 0;
            int rejected = 0;

            long t0 = System.nanoTime();
            for (TransactionEvent ev : workload.events()) {
                TemporalAddResult res = engine.add(ev);
                if (res == TemporalAddResult.ACCEPTED) accepted++;
                else if (res == TemporalAddResult.TOO_LATE) rejected++;
            }
            long t1 = System.nanoTime();
            runEngineNanos = t1 - t0;

            long s0 = System.nanoTime();
            TemporalSnapshot snap = engine.getSnapshot();
            long s1 = System.nanoTime();
            long runSnapNanos = s1 - s0;

            long d0 = System.nanoTime();
            List<Cycle> detected = detector.detect(snap);
            long d1 = System.nanoTime();
            long runDetectNanos = d1 - d0;

            if (i >= warmup) {
                engineTimes.add(runEngineNanos);
                snapshotTimes.add(runSnapNanos);
                detectTimes.add(runDetectNanos);
                combinedTimes.add(runEngineNanos + runSnapNanos + runDetectNanos);
            }

            if (i == warmup + measured - 1) {
                // Record accuracy on the final repetition's generated workload
                evaluatedSeed = config.seed;
                acceptedCount = accepted;
                rejectedCount = rejected;
                snapshotTxCount = snap.getAllEvents().size();
                finalSnap = snap;

                fingerprint = workload.baseWorkloadFingerprint();

                GroundTruthEvaluator evaluator = new GroundTruthEvaluator();
                finalQuality = evaluator.evaluateRecords(workload.groundTruth(), detected);
            }
        }

        engineTimes.sort(Long::compareTo);
        snapshotTimes.sort(Long::compareTo);
        detectTimes.sort(Long::compareTo);
        combinedTimes.sort(Long::compareTo);

        long engineP50 = PercentileUtil.calculate(engineTimes, 50.0);
        long detectP50 = PercentileUtil.calculate(detectTimes, 50.0);

        double attemptedTps = attemptedCount / (engineP50 / 1e9);
        double acceptedTps = acceptedCount / (engineP50 / 1e9);
        double detectionTps = snapshotTxCount == 0 || detectP50 == 0 ? 0.0 : snapshotTxCount / (detectP50 / 1e9);

        double precision = finalQuality.precision();
        double recall = finalQuality.recall();
        double f1 = finalQuality.f1Score();

        int requestedCycles = config.cycleCount;
        int generatedCycles = generatedCyclesCount;

        long finalWatermarkMs = finalSnap == null || finalSnap.getWatermark() == null ? -1 : finalSnap.getWatermark().toEpochMilli();

        return new BenchmarkMetrics(
            expId, seed, evaluatedSeed, fingerprint, totalTx, requestedCycles, generatedCycles, winSec, lateSec,
            config.eventSpacingMs, oooCount, maxLateMs, finalWindowOnly,
            warmup, measured,
            PercentileUtil.calculate(engineTimes, 50), PercentileUtil.calculate(engineTimes, 95), PercentileUtil.calculate(engineTimes, 99),
            PercentileUtil.calculate(snapshotTimes, 50), PercentileUtil.calculate(snapshotTimes, 95), PercentileUtil.calculate(snapshotTimes, 99),
            PercentileUtil.calculate(detectTimes, 50), PercentileUtil.calculate(detectTimes, 95), PercentileUtil.calculate(detectTimes, 99),
            PercentileUtil.calculate(combinedTimes, 50), PercentileUtil.calculate(combinedTimes, 95), PercentileUtil.calculate(combinedTimes, 99),
            attemptedTps, acceptedTps, detectionTps,
            finalQuality.truePositives(), finalQuality.ordinaryFalsePositives(), finalQuality.ineligibleTargetDetections(), finalQuality.falseNegatives(),
            precision, recall, f1,
            attemptedCount, acceptedCount, rejectedCount, finalQuality.ambiguousTargets(), finalQuality.ignoredAmbiguousDetections(), finalQuality.totalInjectedTargets(),
            finalQuality.ineligibleTargets(), snapshotTxCount, finalWatermarkMs,
            "ceil(p*N) rank mapping"
        );
    }
}
