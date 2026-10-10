package com.veloring.engine.evaluation.benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvReporter {
    public static void write(Path outputPath, List<BenchmarkMetrics> metricsList) throws IOException {
        Files.createDirectories(outputPath.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Write header
            writer.write("experimentId,seed,evaluatedSeed,workloadFingerprint,workloadSize,requestedCycles,generatedCycles,windowSizeSec,allowedLatenessSec," +
                    "eventSpacingMs,outOfOrderCount,maxLatenessMs,finalWindowOnly," +
                    "warmupIterations,measuredIterations,engineInsertionMedianNanos,engineInsertionP95Nanos," +
                    "engineInsertionP99Nanos,snapshotMedianNanos,snapshotP95Nanos,snapshotP99Nanos," +
                    "detectionMedianNanos,detectionP95Nanos,detectionP99Nanos,combinedMedianNanos," +
                    "combinedP95Nanos,combinedP99Nanos,attemptedInsertionThroughput,acceptedInsertionThroughput,detectionThroughput," +
                    "truePositives,ordinaryFalsePositives,ineligibleTargetDetections,falseNegatives,precision,recall,f1," +
                    "attemptedCount,acceptedCount,rejectedCount,ambiguousTargetsCount,ignoredAmbiguousDetections,validEvaluatedCycleTargets," +
                    "ineligibleTargetsCount,retainedTransactionCount,finalWatermarkMs,percentileMethod\n");

            for (BenchmarkMetrics m : metricsList) {
                writer.write(String.format("%s,%d,%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%b,%d,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s\n",
                    escape(m.experimentId()), m.seed(), m.evaluatedSeed(), escape(m.workloadFingerprint()), m.workloadSize(), m.requestedCycles(), m.generatedCycles(),
                    m.windowSizeSec(), m.allowedLatenessSec(), m.eventSpacingMs(), m.outOfOrderCount(), m.maxLatenessMs(), m.finalWindowOnly(),
                    m.warmupIterations(), m.measuredIterations(),
                    formatLong(m.engineInsertionMedianNanos()), formatLong(m.engineInsertionP95Nanos()), formatLong(m.engineInsertionP99Nanos()),
                    formatLong(m.snapshotMedianNanos()), formatLong(m.snapshotP95Nanos()), formatLong(m.snapshotP99Nanos()),
                    formatLong(m.detectionMedianNanos()), formatLong(m.detectionP95Nanos()), formatLong(m.detectionP99Nanos()),
                    formatLong(m.combinedMedianNanos()), formatLong(m.combinedP95Nanos()), formatLong(m.combinedP99Nanos()),
                    formatDouble(m.attemptedInsertionThroughput()), formatDouble(m.acceptedInsertionThroughput()), formatDouble(m.detectionThroughput()),
                    m.truePositives(), m.ordinaryFalsePositives(), m.ineligibleTargetDetections(), m.falseNegatives(),
                    formatDouble(m.precision()), formatDouble(m.recall()), formatDouble(m.f1()),
                    m.attemptedCount(), m.acceptedCount(), m.rejectedCount(), m.ambiguousTargetsCount(), m.ignoredAmbiguousDetections(), m.validEvaluatedCycleTargets(),
                    m.ineligibleTargetsCount(), m.retainedTransactionCount(), m.finalWatermarkMs(), escape(m.percentileMethod())
                ));
            }
        }
    }

    public static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static String formatLong(Long val) {
        return val == null ? "N/A" : val.toString();
    }

    public static String formatDouble(Double val) {
        return (val == null || val.isNaN() || val.isInfinite()) ? "N/A" : String.format("%.2f", val);
    }
}
