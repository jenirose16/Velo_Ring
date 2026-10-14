import type { Transaction, CycleDetectionEvent, GraphNodeData, GraphEdgeData } from '../types';

export const VERIFIED_RUN_ID = '3b320ae5';

export const DEMO_TRANSACTIONS: Transaction[] = [
  // Verified Live Directed 3-Cycle (Run 3b320ae5)
  {
    transactionId: 'tx-demo-3b320ae5-1',
    senderAccountId: 'A',
    receiverAccountId: 'B',
    amount: 100.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:19:20Z',
    processedAt: '2026-10-14T07:19:22Z',
    channel: 'WEB',
    isCandidateCycle: true,
    injectedPattern: true,
    role: 'CYCLE_3_HOP_1',
    status: 'FLAGGED'
  },
  {
    transactionId: 'tx-demo-3b320ae5-2',
    senderAccountId: 'B',
    receiverAccountId: 'C',
    amount: 100.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:19:21Z',
    processedAt: '2026-10-14T07:19:22Z',
    channel: 'WEB',
    isCandidateCycle: true,
    injectedPattern: true,
    role: 'CYCLE_3_HOP_2',
    status: 'FLAGGED'
  },
  {
    transactionId: 'tx-demo-3b320ae5-3',
    senderAccountId: 'C',
    receiverAccountId: 'A',
    amount: 100.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:19:22Z',
    processedAt: '2026-10-14T07:19:23Z',
    channel: 'WEB',
    isCandidateCycle: true,
    injectedPattern: true,
    role: 'CYCLE_3_HOP_3',
    status: 'FLAGGED'
  },

  // Surrounding Contextual Network (Synthetic Rail Traffic for Network Topology)
  {
    transactionId: 'tx-bg-9a4f210d-01',
    senderAccountId: 'EXT-PAYROLL-01',
    receiverAccountId: 'A',
    amount: 4500.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:10:00Z',
    processedAt: '2026-10-14T07:10:01Z',
    channel: 'ACH',
    isCandidateCycle: false,
    injectedPattern: false,
    role: 'INBOUND_SALARY',
    status: 'PERSISTED'
  },
  {
    transactionId: 'tx-bg-8b3c108e-02',
    senderAccountId: 'B',
    receiverAccountId: 'MERCHANT-RETAIL-08',
    amount: 45.20,
    currency: 'USD',
    eventTime: '2026-10-14T07:12:15Z',
    processedAt: '2026-10-14T07:12:16Z',
    channel: 'POS',
    isCandidateCycle: false,
    injectedPattern: false,
    role: 'CONSUMER_PURCHASE',
    status: 'PERSISTED'
  },
  {
    transactionId: 'tx-bg-7c2a097f-03',
    senderAccountId: 'C',
    receiverAccountId: 'UTILITY-POWER-02',
    amount: 112.50,
    currency: 'USD',
    eventTime: '2026-10-14T07:14:40Z',
    processedAt: '2026-10-14T07:14:41Z',
    channel: 'WEB',
    isCandidateCycle: false,
    injectedPattern: false,
    role: 'BILL_PAYMENT',
    status: 'PERSISTED'
  },
  {
    transactionId: 'tx-fanout-4d1e289c-01',
    senderAccountId: 'SOURCE-HUB-10',
    receiverAccountId: 'A',
    amount: 850.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:18:10Z',
    processedAt: '2026-10-14T07:18:11Z',
    channel: 'MOBILE',
    isCandidateCycle: false,
    injectedPattern: true,
    role: 'FAN_OUT_SOURCE',
    status: 'PERSISTED'
  },
  {
    transactionId: 'tx-fanout-4d1e289c-02',
    senderAccountId: 'SOURCE-HUB-10',
    receiverAccountId: 'NODE-INTERMEDIARY-11',
    amount: 920.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:18:12Z',
    processedAt: '2026-10-14T07:18:13Z',
    channel: 'MOBILE',
    isCandidateCycle: false,
    injectedPattern: true,
    role: 'FAN_OUT_SOURCE',
    status: 'PERSISTED'
  },
  {
    transactionId: 'tx-fanout-4d1e289c-03',
    senderAccountId: 'SOURCE-HUB-10',
    receiverAccountId: 'NODE-INTERMEDIARY-12',
    amount: 780.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:18:14Z',
    processedAt: '2026-10-14T07:18:15Z',
    channel: 'MOBILE',
    isCandidateCycle: false,
    injectedPattern: true,
    role: 'FAN_OUT_SOURCE',
    status: 'PERSISTED'
  },
  {
    transactionId: 'tx-passthrough-3f0a991b-01',
    senderAccountId: 'NODE-INTERMEDIARY-11',
    receiverAccountId: 'SINK-OFFSHORE-99',
    amount: 900.00,
    currency: 'USD',
    eventTime: '2026-10-14T07:18:45Z',
    processedAt: '2026-10-14T07:18:46Z',
    channel: 'SWIFT',
    isCandidateCycle: false,
    injectedPattern: true,
    role: 'PASS_THROUGH_EXIT',
    status: 'PERSISTED'
  }
];

export const DEMO_DETECTION_EVENTS: CycleDetectionEvent[] = [
  {
    eventId: 'EVT-RING3-3B320AE5-001',
    detectionTime: '2026-10-14T07:19:25Z',
    watermark: '2026-10-14T07:19:22Z',
    category: 'CIRCULAR_MULE_RING_3',
    implicatedAccounts: ['A', 'B', 'C'],
    edges: [
      { senderAccountId: 'A', receiverAccountId: 'B', txId: 'tx-demo-3b320ae5-1', amount: 100.00 },
      { senderAccountId: 'B', receiverAccountId: 'C', txId: 'tx-demo-3b320ae5-2', amount: 100.00 },
      { senderAccountId: 'C', receiverAccountId: 'A', txId: 'tx-demo-3b320ae5-3', amount: 100.00 }
    ],
    investigationStatus: 'CONFIRMED_SYNTHETIC_TARGET',
    confidenceScore: 0.994,
    totalCycleVolume: 300.00,
    e2eDelayMs: 420,
    sourceAttribution: 'Live Ingestion Run 3b320ae5 (Kafka Offset 2551-2553, Neo4j GraphDB)'
  },
  {
    eventId: 'EVT-FANOUT-4D1E289C-002',
    detectionTime: '2026-10-14T07:18:20Z',
    watermark: '2026-10-14T07:18:15Z',
    category: 'FAN_OUT_TOPOLOGY',
    implicatedAccounts: ['SOURCE-HUB-10', 'A', 'NODE-INTERMEDIARY-11', 'NODE-INTERMEDIARY-12'],
    edges: [
      { senderAccountId: 'SOURCE-HUB-10', receiverAccountId: 'A', txId: 'tx-fanout-4d1e289c-01', amount: 850.00 },
      { senderAccountId: 'SOURCE-HUB-10', receiverAccountId: 'NODE-INTERMEDIARY-11', txId: 'tx-fanout-4d1e289c-02', amount: 920.00 },
      { senderAccountId: 'SOURCE-HUB-10', receiverAccountId: 'NODE-INTERMEDIARY-12', txId: 'tx-fanout-4d1e289c-03', amount: 780.00 }
    ],
    investigationStatus: 'UNDER_INVESTIGATION',
    confidenceScore: 0.880,
    totalCycleVolume: 2550.00,
    e2eDelayMs: 810,
    sourceAttribution: 'Synthetic Stream Generator (Scenario fanout)'
  }
];

export function getDemoGraphElements(): { nodes: GraphNodeData[]; edges: GraphEdgeData[] } {
  const accountsMap = new Map<string, { inDegree: number; outDegree: number; volume: number; isCycle: boolean }>();

  DEMO_TRANSACTIONS.forEach(tx => {
    // Sender
    const s = accountsMap.get(tx.senderAccountId) || { inDegree: 0, outDegree: 0, volume: 0, isCycle: false };
    s.outDegree += 1;
    s.volume += tx.amount;
    if (tx.isCandidateCycle) s.isCycle = true;
    accountsMap.set(tx.senderAccountId, s);

    // Receiver
    const r = accountsMap.get(tx.receiverAccountId) || { inDegree: 0, outDegree: 0, volume: 0, isCycle: false };
    r.inDegree += 1;
    r.volume += tx.amount;
    if (tx.isCandidateCycle) r.isCycle = true;
    accountsMap.set(tx.receiverAccountId, r);
  });

  const nodes: GraphNodeData[] = Array.from(accountsMap.entries()).map(([id, meta]) => ({
    id,
    label: id,
    isCycleMember: meta.isCycle,
    inDegree: meta.inDegree,
    outDegree: meta.outDegree,
    totalVolume: Math.round(meta.volume * 100) / 100
  }));

  const edges: GraphEdgeData[] = DEMO_TRANSACTIONS.map(tx => ({
    id: tx.transactionId,
    source: tx.senderAccountId,
    target: tx.receiverAccountId,
    amount: tx.amount,
    currency: tx.currency,
    txId: tx.transactionId,
    eventTime: tx.eventTime,
    isCycleEdge: !!tx.isCandidateCycle
  }));

  return { nodes, edges };
}
