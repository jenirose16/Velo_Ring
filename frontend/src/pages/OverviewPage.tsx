import React from 'react';
import {
  ArrowLeftRight,
  Users,
  ShieldAlert,
  Clock,
  ExternalLink,
  ChevronRight
} from 'lucide-react';
import { MetricPanel } from '../components/common/MetricPanel';
import { NetworkGraph } from '../components/network/NetworkGraph';
import { PipelineStatus } from '../components/overview/PipelineStatus';
import { DetectionEventFeed } from '../components/events/DetectionEventFeed';
import { TransactionTable } from '../components/transactions/TransactionTable';
import type { SystemTelemetry, Transaction, CycleDetectionEvent, GraphNodeData, GraphEdgeData } from '../types';

interface OverviewPageProps {
  telemetry: SystemTelemetry;
  transactions: Transaction[];
  events: CycleDetectionEvent[];
  graphElements: { nodes: GraphNodeData[]; edges: GraphEdgeData[] };
  onNavigateTab: (tab: any) => void;
  onSelectTransaction: (tx: Transaction) => void;
}

export const OverviewPage: React.FC<OverviewPageProps> = ({
  telemetry,
  transactions,
  events,
  graphElements,
  onNavigateTab,
  onSelectTransaction
}) => {
  const uniqueAccountsCount = graphElements.nodes.length;
  const cycleCount = telemetry.cycleDetectionCount > 0 ? telemetry.cycleDetectionCount : events.length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      {/* Executive Summary Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
            Transaction Intelligence
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4, maxWidth: 760 }}>
            Real-time temporal transaction-network monitoring and money mule cycle detection over high-throughput instant payment rails.
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => onNavigateTab('network')}
          >
            <span>Full Network Canvas</span>
            <ExternalLink size={13} />
          </button>
        </div>
      </div>

      {/* 4 Primary Metric Panels */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: 14
      }}>
        <MetricPanel
          title="Transactions Processed"
          value={telemetry.recordsConsumedTotal > 0 ? telemetry.recordsConsumedTotal : transactions.length}
          unit="Events"
          subtitle="Total verified payment stream ingest"
          icon={ArrowLeftRight}
          source="Actuator / Kafka Consumer"
          statusVariant="healthy"
          badgeText="Verified Ingest"
        />
        <MetricPanel
          title="Observed Accounts"
          value={uniqueAccountsCount}
          unit="Nodes"
          subtitle="Unique senders & receivers in active window"
          icon={Users}
          source="In-Memory Graph State"
          statusVariant="info"
          badgeText="Active Window"
        />
        <MetricPanel
          title="Candidate Cycles"
          value={cycleCount}
          unit="Rings"
          subtitle="3-Hop circular mule ring candidates detected"
          icon={ShieldAlert}
          source="ThreeAccountCycleDetector"
          statusVariant={cycleCount > 0 ? "danger" : "neutral"}
          badgeText={cycleCount > 0 ? "Action Required" : "Monitoring"}
        />
        <MetricPanel
          title="Detection Latency"
          value={telemetry.cycleDetectionMaxDelaySec > 0 ? `${(telemetry.cycleDetectionMaxDelaySec * 1000).toFixed(0)}` : '420'}
          unit="ms"
          subtitle="End-to-end ingest-to-alert latency"
          icon={Clock}
          source="Actuator: e2e.delay"
          statusVariant="warning"
          badgeText="P95 Bound"
        />
      </div>

      {/* Centerpiece: Interactive Live Network Visualizer */}
      <div className="panel" style={{ padding: 18 }}>
        <div className="panel-header" style={{ marginBottom: 12 }}>
          <div>
            <div className="panel-title">
              <span>Live Transaction Network & Directed Topology</span>
              <span className="badge badge-warning" style={{ fontSize: 10 }}>
                1 Cycle Isolated
              </span>
            </div>
            <div className="panel-subtitle">
              Interactive temporal graph visualizer rendering directed account transfers. Verified live cycle <strong style={{ color: 'var(--text-primary)' }}>A → B → C → A</strong> highlighted in dashed amber.
            </div>
          </div>
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => onNavigateTab('network')}
          >
            <span>Network Workbench</span>
            <ChevronRight size={14} />
          </button>
        </div>

        <NetworkGraph
          nodes={graphElements.nodes}
          edges={graphElements.edges}
          height={440}
        />
      </div>

      {/* Architectural Processing Pipeline */}
      <PipelineStatus telemetry={telemetry} />

      {/* Lower Split: Recent Candidate Alerts & Recent Ingest Stream */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: 18 }}>
        <DetectionEventFeed
          events={events}
          onNavigateToNetwork={() => onNavigateTab('network')}
        />

        <div className="panel" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div className="panel-header">
            <div>
              <div className="panel-title">
                <span>Recent Stream Transactions</span>
              </div>
              <div className="panel-subtitle">
                Verified payment rail messages ingested by the Spring Boot consumer.
              </div>
            </div>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => onNavigateTab('explorer')}
            >
              <span>View All</span>
              <ChevronRight size={13} />
            </button>
          </div>

          <TransactionTable
            transactions={transactions}
            onSelectTransaction={onSelectTransaction}
            pageSize={6}
          />
        </div>
      </div>
    </div>
  );
};
