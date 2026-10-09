package com.veloring.engine.evaluation;

/**
 * Result of evaluating detected cycles against ground-truth injected patterns.
 */
public record CycleMatchResult(
    int truePositives,
    int falsePositives, // Unmatched detected cycles
    int falseNegatives,
    int ambiguousTargets,
    int ignoredAmbiguousDetections,
    int totalDetectedCycles,
    int totalInjectedTargets
) {
    /**
     * @return Precision, or Double.NaN if no cycles were detected.
     */
    public double precision() {
        if (truePositives + falsePositives == 0) return Double.NaN;
        return (double) truePositives / (truePositives + falsePositives);
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
