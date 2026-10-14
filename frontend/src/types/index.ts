export type NavigationTab =
  | 'overview'
  | 'explorer'
  | 'network'
  | 'events'
  | 'evaluation'
  | 'health'
  | 'settings';

export interface Transaction {
  transactionId: string;
  senderAccountId: string;
  receiverAccountId: string;
  amount: number;
  currency: string;
  eventTime: string;
  processedAt?: string;
  channel: string;
  isCandidateCycle?: boolean;
  injectedPattern?: boolean;
  role?: string;
  status: 'PERSISTED' | 'INGESTED' | 'FLAGGED';
}

export interface CycleEdge {
  txId: string;
  senderAccountId: string;
  receiverAccountId: string;
  amount?: number;
}

export interface CycleDetectionEvent {
  eventId: string;
  detectionTime: string;
  watermark: string;
  category: 'CIRCULAR_MULE_RING_3' | 'FAN_OUT_TOPOLOGY' | 'RAPID_PASS_THROUGH';
  implicatedAccounts: string[];
  edges: CycleEdge[];
  investigationStatus: 'UNDER_INVESTIGATION' | 'CONFIRMED_SYNTHETIC_TARGET' | 'DISMISSED';
  confidenceScore: number;
  totalCycleVolume: number;
  e2eDelayMs?: number;
  sourceAttribution: string;
}

export interface BackendHealthResponse {
  service: string;
  status: 'UP' | 'DOWN';
  neo4j: 'UP' | 'DOWN';
}

export interface ActuatorMetricMeasurement {
  statistic: string;
  value: number;
}

export interface ActuatorMetricResponse {
  name: string;
  description?: string;
  baseUnit?: string;
  measurements: ActuatorMetricMeasurement[];
  availableTags?: { tag: string; values: string[] }[];
}

export interface SystemTelemetry {
  backendHealth: 'UP' | 'DOWN' | 'CONNECTING';
  neo4jStatus: 'UP' | 'DOWN' | 'UNKNOWN';
  kafkaTopicStatus: 'HEALTHY' | 'DISCONNECTED' | 'UNKNOWN';
  recordsConsumedTotal: number;
  cycleDetectionCount: number;
  cycleDetectionMaxDelaySec: number;
  cycleDetectionTotalTimeSec: number;
  lastSuccessfulPoll: string | null;
  pollingError: string | null;
  responseTimeMs: number;
}

export interface BenchmarkRecord {
  experimentId: string;
  evaluatedSeed: number;
  baseWorkloadFingerprint: string;
  evaluationFingerprint: string;
  windowSizeSec: number;
  allowedLatenessSec: number;
  precision: number;
  recall: number;
  f1: number;
}

export interface BenchmarkStats {
  windowSizeSec: number;
  sampleCount: number;
  meanPrecision: number;
  meanRecall: number;
  meanF1: number;
  stdDevF1: number;
  minF1: number;
  maxF1: number;
  medianF1: number;
  p95F1: number;
}

export interface GraphNodeData {
  id: string;
  label: string;
  isCycleMember: boolean;
  inDegree: number;
  outDegree: number;
  totalVolume: number;
}

export interface GraphEdgeData {
  id: string;
  source: string;
  target: string;
  amount: number;
  currency: string;
  txId: string;
  eventTime: string;
  isCycleEdge: boolean;
}
