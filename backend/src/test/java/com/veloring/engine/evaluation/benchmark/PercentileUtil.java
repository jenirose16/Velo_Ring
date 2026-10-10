package com.veloring.engine.evaluation.benchmark;

import java.util.List;

public class PercentileUtil {

    /**
     * Calculates the nearest-rank percentile for a sorted list of values.
     * Uses rank = ceil((p / 100.0) * N) - 1 for 0-based indexing.
     */
    public static Long calculate(List<Long> sorted, double p) {
        if (sorted == null || sorted.isEmpty()) {
            return null;
        }
        if (p <= 0 || p > 100) {
            throw new IllegalArgumentException("Percentile must be in (0, 100]");
        }
        int index = (int) Math.ceil((p / 100.0) * sorted.size()) - 1;
        if (index < 0) index = 0;
        if (index >= sorted.size()) index = sorted.size() - 1;
        return sorted.get(index);
    }
}
