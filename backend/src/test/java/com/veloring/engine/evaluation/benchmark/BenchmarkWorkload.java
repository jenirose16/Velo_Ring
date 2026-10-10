package com.veloring.engine.evaluation.benchmark;

import com.veloring.engine.event.TransactionEvent;
import com.veloring.engine.evaluation.GroundTruthEvaluator;

import java.util.List;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public record BenchmarkWorkload(
    List<TransactionEvent> events,
    List<GroundTruthEvaluator.GroundTruthRecord> groundTruth
) {
    public String baseWorkloadFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Serialize events
            for (TransactionEvent ev : events) {
                digest.update(ev.getTransactionId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(ev.getSenderAccountId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(ev.getReceiverAccountId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                if (ev.getAmount() != null) {
                    digest.update(ev.getAmount().toString().getBytes(StandardCharsets.UTF_8));
                }
                digest.update((byte) 0);
                digest.update(ev.getEventTime().toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                if (ev.getProcessedAt() != null) {
                    digest.update(ev.getProcessedAt().toString().getBytes(StandardCharsets.UTF_8));
                }
                digest.update((byte) 0);
                digest.update(ev.getCurrency().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(ev.getChannel().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }

            // Serialize ground truth (excluding eligibleUnderPolicy which is window-specific)
            for (GroundTruthEvaluator.GroundTruthRecord gt : groundTruth) {
                digest.update(gt.experimentId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(gt.scenarioId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(gt.scenarioType().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(gt.transactionId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(gt.role().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update((byte) (gt.injectedPattern() ? 1 : 0));
                digest.update((byte) 0);
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    public String evaluationFingerprint(long windowSizeSec, long allowedLatenessSec) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash the base fingerprint
            digest.update(baseWorkloadFingerprint().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(String.valueOf(windowSizeSec).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(String.valueOf(allowedLatenessSec).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);

            // Serialize window-specific ground truth eligibility
            for (GroundTruthEvaluator.GroundTruthRecord gt : groundTruth) {
                digest.update(gt.transactionId().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update((byte) (gt.eligibleUnderPolicy() ? 1 : 0));
                digest.update((byte) 0);
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
