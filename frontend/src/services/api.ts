import type {
  BackendHealthResponse,
  ActuatorMetricResponse,
  SystemTelemetry,
  BenchmarkRecord,
  BenchmarkStats
} from '../types';

const TIMEOUT_MS = 4000;

async function fetchWithTimeout(url: string): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(url, { signal: controller.signal });
    clearTimeout(timeoutId);
    return res;
  } catch (err) {
    clearTimeout(timeoutId);
    throw err;
  }
}

export async function fetchSystemTelemetry(): Promise<SystemTelemetry> {
  const startTime = performance.now();

  let healthData: BackendHealthResponse | null = null;
  let recordsConsumed = 0;
  let cycleCount = 0;
  let cycleMaxDelay = 0;
  let cycleTotalTime = 0;
  let errorMsg: string | null = null;

  // 1. Query Main Health API (/api/v1/health)
  try {
    const res = await fetchWithTimeout('/api/v1/health');
    if (res.ok) {
      healthData = await res.json();
    } else {
      errorMsg = `Health API responded with HTTP ${res.status}`;
    }
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Connection failed';
    errorMsg = `Backend unreachable: ${message}`;
  }

  // 2. Query Actuator Kafka Consumer Ingest Metric
  try {
    const res = await fetchWithTimeout('/actuator/metrics/kafka.consumer.fetch.manager.records.consumed.total');
    if (res.ok) {
      const metric: ActuatorMetricResponse = await res.json();
      const countMeasurement = metric.measurements?.find(m => m.statistic === 'COUNT');
      if (countMeasurement) {
        recordsConsumed = countMeasurement.value;
      }
    }
  } catch {
    // Actuator endpoint optional/offline
  }

  // 3. Query Actuator Cycle Detection Latency Metric
  try {
    const res = await fetchWithTimeout('/actuator/metrics/veloring.detect.e2e.delay');
    if (res.ok) {
      const metric: ActuatorMetricResponse = await res.json();
      const count = metric.measurements?.find(m => m.statistic === 'COUNT')?.value || 0;
      const max = metric.measurements?.find(m => m.statistic === 'MAX')?.value || 0;
      const total = metric.measurements?.find(m => m.statistic === 'TOTAL_TIME')?.value || 0;
      cycleCount = count;
      cycleMaxDelay = max;
      cycleTotalTime = total;
    }
  } catch {
    // Metric endpoint optional
  }

  const responseTimeMs = Math.round(performance.now() - startTime);

  return {
    backendHealth: healthData?.status === 'UP' ? 'UP' : 'DOWN',
    neo4jStatus: healthData?.neo4j === 'UP' ? 'UP' : (healthData ? 'DOWN' : 'UNKNOWN'),
    kafkaTopicStatus: recordsConsumed > 0 ? 'HEALTHY' : (healthData ? 'HEALTHY' : 'UNKNOWN'),
    recordsConsumedTotal: recordsConsumed,
    cycleDetectionCount: cycleCount,
    cycleDetectionMaxDelaySec: cycleMaxDelay,
    cycleDetectionTotalTimeSec: cycleTotalTime,
    lastSuccessfulPoll: healthData ? new Date().toISOString() : null,
    pollingError: errorMsg,
    responseTimeMs
  };
}

export async function loadBenchmarkDataset(): Promise<BenchmarkRecord[]> {
  const res = await fetch('/data/window_sensitivity_paired.csv');
  if (!res.ok) {
    throw new Error(`Failed to load benchmark artifact: HTTP ${res.status}`);
  }
  const text = await res.text();
  const lines = text.trim().split('\n');
  if (lines.length < 2) return [];

  const records: BenchmarkRecord[] = [];
  // Skip header: experimentId,evaluatedSeed,baseWorkloadFingerprint,evaluationFingerprint,windowSizeSec,allowedLatenessSec,precision,recall,f1
  for (let i = 1; i < lines.length; i++) {
    const parts = lines[i].split(',').map(s => s.trim());
    if (parts.length >= 9) {
      records.push({
        experimentId: parts[0],
        evaluatedSeed: parseInt(parts[1], 10),
        baseWorkloadFingerprint: parts[2],
        evaluationFingerprint: parts[3],
        windowSizeSec: parseInt(parts[4], 10),
        allowedLatenessSec: parseInt(parts[5], 10),
        precision: parseFloat(parts[6]),
        recall: parseFloat(parts[7]),
        f1: parseFloat(parts[8])
      });
    }
  }
  return records;
}

export function computeBenchmarkStats(records: BenchmarkRecord[]): BenchmarkStats[] {
  const groupedByWindow = new Map<number, BenchmarkRecord[]>();

  records.forEach(r => {
    const list = groupedByWindow.get(r.windowSizeSec) || [];
    list.push(r);
    groupedByWindow.set(r.windowSizeSec, list);
  });

  const stats: BenchmarkStats[] = [];

  groupedByWindow.forEach((group, windowSize) => {
    const n = group.length;
    if (n === 0) return;

    const meanPrecision = group.reduce((acc, r) => acc + r.precision, 0) / n;
    const meanRecall = group.reduce((acc, r) => acc + r.recall, 0) / n;
    const f1s = group.map(r => r.f1).sort((a, b) => a - b);
    const meanF1 = f1s.reduce((acc, v) => acc + v, 0) / n;

    // Sample standard deviation
    const variance = f1s.reduce((acc, v) => acc + Math.pow(v - meanF1, 2), 0) / (n > 1 ? n - 1 : 1);
    const stdDevF1 = Math.sqrt(variance);

    const minF1 = f1s[0];
    const maxF1 = f1s[f1s.length - 1];
    const medianF1 = f1s[Math.floor(n / 2)];
    const p95F1 = f1s[Math.floor(n * 0.95)] || maxF1;

    stats.push({
      windowSizeSec: windowSize,
      sampleCount: n,
      meanPrecision,
      meanRecall,
      meanF1,
      stdDevF1,
      minF1,
      maxF1,
      medianF1,
      p95F1
    });
  });

  return stats.sort((a, b) => a.windowSizeSec - b.windowSizeSec);
}
