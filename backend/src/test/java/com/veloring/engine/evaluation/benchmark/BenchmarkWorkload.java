package com.veloring.engine.evaluation.benchmark;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.evaluation.GroundTruthEvaluator;

import java.util.List;

public record BenchmarkWorkload(
    List<TransactionEvent> events,
    List<GroundTruthEvaluator.GroundTruthRecord> groundTruth
) {}
