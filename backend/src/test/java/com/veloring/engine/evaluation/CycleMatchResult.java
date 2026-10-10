package com.veloring.engine.evaluation;

/**
 * Result of evaluating detected cycles against ground-truth injected patterns.
 */
public record CycleMatchResult(
    int truePositives,
    int ordinaryFalsePositives, // Unmatched detected cycles
    int ineligibleTargetDetections, // Detections of cycles that should have been evicted
    int falseNegatives,
    int ambiguousTargets,
    int ignoredAmbiguousDetections,
    int totalDetectedCycles,
    int totalInjectedTargets,
    int ineligibleTargets
) {
    /**
     * @return Precision, or Double.NaN if no cycles were detected.
     */
    public double precision() {
        int totalFalsePositives = ordinaryFalsePositives + ineligibleTargetDetections;
        if (truePositives + totalFalsePositives == 0) return Double.NaN;
        return (double) truePositives / (truePositives + totalFalsePositives);
    }

    /**
     * @return Recall, or Double.NaN if there were no unambiguous target patterns.
     */
    public double recall() {
        if (truePositives + falseNegatives == 0) return Double.NaN;
        return (double) truePositives / (truePositives + falseNegatives);
    }

    /**
     * @return F1 Score, or Double.NaN if precision or recall are NaN, or their sum is 0.
     */
    public double f1Score() {
        double p = precision();
        double r = recall();
        if (Double.isNaN(p) || Double.isNaN(r) || p + r == 0) return Double.NaN;
        return 2 * (p * r) / (p + r);
    }
}
