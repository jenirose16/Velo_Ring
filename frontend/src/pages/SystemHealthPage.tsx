import React from 'react';
import {
  Server,
  Database,
  Radio,
  RefreshCw,
  Zap
} from 'lucide-react';
import type { SystemTelemetry } from '../types';

interface SystemHealthPageProps {
  telemetry: SystemTelemetry;
  onRefresh: () => void;
  isRefreshing: boolean;
}

export const SystemHealthPage: React.FC<SystemHealthPageProps> = ({
  telemetry,
  onRefresh,
  isRefreshing
}) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
            Infrastructure Health & Processing Telemetry
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
            Direct telemetry aggregated via Spring Boot Actuator (<code className="mono">port 8081</code>) and Engine Health API (<code className="mono">port 8080</code>).
          </p>
        </div>

        <button
          className="btn btn-secondary btn-sm"
          onClick={onRefresh}
          disabled={isRefreshing}
        >
          <RefreshCw size={13} style={{ animation: isRefreshing ? 'spin 1s linear infinite' : 'none' }} />
          <span>Sync Diagnostics</span>
        </button>
      </div>

      {/* Grid of Component Health Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: 16
      }}>
        {/* Spring Boot Engine */}
        <div className="panel" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Server size={18} style={{ color: 'var(--accent-primary)' }} />
                <span style={{ fontWeight: 600, fontSize: 14 }}>Spring Boot Backend</span>
              </div>
              <span className={`badge ${telemetry.backendHealth === 'UP' ? 'badge-healthy' : 'badge-danger'}`}>
                {telemetry.backendHealth}
              </span>
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.45 }}>
              Engine service running on port 8080 with Actuator diagnostics on 8081. Handles Kafka consumption, constraint validation, and scheduling.
            </div>
          </div>
          <div style={{ marginTop: 14, paddingTop: 10, borderTop: '1px solid var(--border-subtle)', fontSize: 11, color: 'var(--text-tertiary)', display: 'flex', justifyContent: 'space-between' }}>
            <span>Endpoint: /api/v1/health</span>
            <span className="mono" style={{ color: 'var(--status-healthy)' }}>HTTP 200 OK</span>
          </div>
        </div>

        {/* Neo4j Database */}
        <div className="panel" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Database size={18} style={{ color: 'var(--accent-primary)' }} />
                <span style={{ fontWeight: 600, fontSize: 14 }}>Neo4j Graph Database</span>
              </div>
              <span className={`badge ${telemetry.neo4jStatus === 'UP' ? 'badge-healthy' : 'badge-neutral'}`}>
                {telemetry.neo4jStatus}
              </span>
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.45 }}>
              Community Edition 5.18.0 running in Docker container <code className="mono">graphdb</code>. Persistent relationship store on Bolt port 7687.
            </div>
          </div>
          <div style={{ marginTop: 14, paddingTop: 10, borderTop: '1px solid var(--border-subtle)', fontSize: 11, color: 'var(--text-tertiary)', display: 'flex', justifyContent: 'space-between' }}>
            <span>Driver Verification:</span>
            <span className="mono" style={{ color: 'var(--status-healthy)' }}>neo4jDriver.verifyConnectivity()</span>
          </div>
        </div>

        {/* Apache Kafka Ingest */}
        <div className="panel" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Radio size={18} style={{ color: 'var(--accent-primary)' }} />
                <span style={{ fontWeight: 600, fontSize: 14 }}>Kafka Consumer Bus</span>
              </div>
              <span className={`badge ${telemetry.recordsConsumedTotal > 0 ? 'badge-healthy' : 'badge-neutral'}`}>
                {telemetry.recordsConsumedTotal > 0 ? 'ACTIVE STREAM' : 'LISTENING'}
              </span>
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.45 }}>
              Topic: <code className="mono">payment-events</code>. Consumer group <code className="mono">veloring-consumer-group</code> listening on broker port 9092.
            </div>
          </div>
          <div style={{ marginTop: 14, paddingTop: 10, borderTop: '1px solid var(--border-subtle)', fontSize: 11, color: 'var(--text-tertiary)', display: 'flex', justifyContent: 'space-between' }}>
            <span>Total Verified Ingest:</span>
            <span className="tabular-nums mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
              {telemetry.recordsConsumedTotal} records
            </span>
          </div>
        </div>
      </div>

      {/* Telemetry Measurements Table */}
      <div className="panel">
        <div className="panel-header">
          <div className="panel-title">
            <Zap size={16} style={{ color: 'var(--status-warning)' }} />
            <span>Active Engine Micrometer Metrics (Actuator)</span>
          </div>
          <span className="mono" style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>
            Roundtrip Ping: {telemetry.responseTimeMs}ms
          </span>
        </div>

        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Metric Identifier</th>
                <th>Measured Value</th>
                <th>Units</th>
                <th>Description</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                  kafka.consumer.fetch.manager.records.consumed.total
                </td>
                <td className="tabular-nums" style={{ fontWeight: 700, color: 'var(--status-healthy)' }}>
                  {telemetry.recordsConsumedTotal.toFixed(1)}
                </td>
                <td>Records</td>
                <td>Total Kafka transactions ingested by spring consumer factory</td>
                <td><span className="badge badge-healthy">Active Ingest</span></td>
              </tr>
              <tr>
                <td className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                  veloring.detect.e2e.delay.count
                </td>
                <td className="tabular-nums" style={{ fontWeight: 700, color: telemetry.cycleDetectionCount > 0 ? 'var(--status-warning)' : 'var(--text-primary)' }}>
                  {telemetry.cycleDetectionCount.toFixed(1)}
                </td>
                <td>Cycles</td>
                <td>Total candidate circular mule rings detected by scheduled cycle engine</td>
                <td>
                  <span className={`badge ${telemetry.cycleDetectionCount > 0 ? 'badge-warning' : 'badge-neutral'}`}>
                    {telemetry.cycleDetectionCount > 0 ? 'Rings Found' : 'Clean'}
                  </span>
                </td>
              </tr>
              <tr>
                <td className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                  veloring.detect.e2e.delay.max
                </td>
                <td className="tabular-nums mono" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                  {telemetry.cycleDetectionMaxDelaySec.toFixed(3)}
                </td>
                <td>Seconds</td>
                <td>Maximum observed latency from transaction processedAt to detection time</td>
                <td><span className="badge badge-neutral">Bounded</span></td>
              </tr>
              <tr>
                <td className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                  veloring.temporal.window_size_seconds
                </td>
                <td className="tabular-nums mono" style={{ fontWeight: 600, color: 'var(--accent-primary)' }}>
                  60.0
                </td>
                <td>Seconds</td>
                <td>Sliding event-time temporal evaluation boundary</td>
                <td><span className="badge badge-info">Configured</span></td>
              </tr>
              <tr>
                <td className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                  veloring.temporal.allowed_lateness_seconds
                </td>
                <td className="tabular-nums mono" style={{ fontWeight: 600, color: 'var(--accent-primary)' }}>
                  60.0
                </td>
                <td>Seconds</td>
                <td>Out-of-order latency tolerance threshold before dropping events</td>
                <td><span className="badge badge-info">Configured</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
