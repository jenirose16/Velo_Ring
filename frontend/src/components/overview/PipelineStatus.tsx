import React from 'react';
import {
  Cpu,
  Radio,
  Server,
  Clock,
  Search,
  Database,
  CheckCircle2,
  AlertCircle,
  HelpCircle
} from 'lucide-react';
import type { SystemTelemetry } from '../../types';

interface PipelineStatusProps {
  telemetry: SystemTelemetry;
}

export const PipelineStatus: React.FC<PipelineStatusProps> = ({ telemetry }) => {
  const steps = [
    {
      id: 'gen',
      name: 'Synthetic Generator',
      tech: 'Spring Boot (Java 21)',
      role: 'Payment Stream Simulation',
      details: 'Deterministic Seed 42, Scenarios: cycle3, fanout',
      icon: Cpu,
      status: 'CONFIGURED' as const,
      badgeText: 'STANDALONE'
    },
    {
      id: 'kafka',
      name: 'Apache Kafka',
      tech: 'Confluent KRaft v7.5.3',
      role: 'Event Ingestion Bus',
      details: `Topic: payment-events (partition 0), ${telemetry.recordsConsumedTotal} tx consumed`,
      icon: Radio,
      status: telemetry.recordsConsumedTotal > 0 ? ('UP' as const) : ('UNKNOWN' as const),
      badgeText: telemetry.recordsConsumedTotal > 0 ? 'VERIFIED' : 'ACTIVE LISTENER'
    },
    {
      id: 'consumer',
      name: 'Ingestion Consumer',
      tech: 'Spring Kafka Listener',
      role: 'Validation & Graph Writing',
      details: 'veloring-consumer-group, manual offset ack',
      icon: Server,
      status: telemetry.backendHealth === 'UP' ? ('UP' as const) : ('DOWN' as const),
      badgeText: telemetry.backendHealth === 'UP' ? 'ONLINE (8080)' : 'OFFLINE'
    },
    {
      id: 'temporal',
      name: 'Temporal Window Engine',
      tech: 'In-Memory Event-Time Tree',
      role: 'Active State & Eviction',
      details: 'Window: 60s, Allowed Lateness: 60s, Sync Eviction',
      icon: Clock,
      status: telemetry.backendHealth === 'UP' ? ('UP' as const) : ('DOWN' as const),
      badgeText: '60s WINDOW'
    },
    {
      id: 'detector',
      name: 'Cycle Detector',
      tech: 'ThreeAccountCycleDetector',
      role: 'Graph Ring Evaluation',
      details: 'Fixed delay: 5000ms, Canonical cycle hashing',
      icon: Search,
      status: telemetry.cycleDetectionCount > 0 ? ('UP' as const) : (telemetry.backendHealth === 'UP' ? ('UP' as const) : ('DOWN' as const)),
      badgeText: telemetry.cycleDetectionCount > 0 ? `${telemetry.cycleDetectionCount} DETECTIONS` : 'MONITORING'
    }
  ];

  const getStatusIcon = (status: 'UP' | 'DOWN' | 'CONFIGURED' | 'UNKNOWN') => {
    switch (status) {
      case 'UP': return <CheckCircle2 size={13} style={{ color: 'var(--status-healthy)' }} />;
      case 'DOWN': return <AlertCircle size={13} style={{ color: 'var(--status-danger)' }} />;
      case 'CONFIGURED': return <CheckCircle2 size={13} style={{ color: 'var(--status-info)' }} />;
      default: return <HelpCircle size={13} style={{ color: 'var(--text-tertiary)' }} />;
    }
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <div>
          <div className="panel-title">
            <span>End-to-End Processing Architecture & Engine Pipeline</span>
          </div>
          <div className="panel-subtitle">
            Temporal graph engine stream topology connecting streaming payment rails to in-memory window detection and historical graph persistence.
          </div>
        </div>
        <div className="badge badge-neutral">
          <span>Architectural Integrity</span>
        </div>
      </div>

      {/* Pipeline Steps Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
        gap: 12,
        alignItems: 'stretch',
        position: 'relative'
      }}>
        {steps.map((step, idx) => {
          const Icon = step.icon;
          return (
            <div
              key={step.id}
              style={{
                backgroundColor: 'var(--bg-canvas)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-sm)',
                padding: '12px 14px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                position: 'relative',
                transition: 'border-color var(--transition-fast)'
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Icon size={14} style={{ color: 'var(--accent-primary)' }} />
                    <span style={{ fontSize: 10, color: 'var(--text-tertiary)', fontWeight: 700, textTransform: 'uppercase' }}>
                      Step 0{idx + 1}
                    </span>
                  </div>
                  {getStatusIcon(step.status)}
                </div>

                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2 }}>
                  {step.name}
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 6 }}>
                  {step.tech}
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-tertiary)', lineHeight: 1.35 }}>
                  {step.details}
                </div>
              </div>

              <div style={{ marginTop: 10, paddingTop: 6, borderTop: '1px solid var(--border-subtle)' }}>
                <span className="badge badge-neutral" style={{ fontSize: 9.5, padding: '2px 5px' }}>
                  {step.badgeText}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Parallel Historical Graph Persistence Callout */}
      <div style={{
        marginTop: 14,
        padding: '10px 14px',
        backgroundColor: 'var(--bg-subtle)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-sm)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 10
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Database size={16} style={{ color: 'var(--accent-primary)' }} />
          <div>
            <span style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-primary)' }}>
              Historical Persistence Layer: Neo4j Community 5.18.0 (APOC Plugin)
            </span>
            <span style={{ fontSize: 11.5, color: 'var(--text-secondary)', marginLeft: 8 }}>
              Synchronous relationship merge <code className="mono">(:Account)-[:TRANSFERRED_TO]-&gt;(:Account)</code> executed on Bolt port 7687 prior to temporal window evaluation.
            </span>
          </div>
        </div>
        <div className={`badge ${telemetry.neo4jStatus === 'UP' ? 'badge-healthy' : 'badge-neutral'}`}>
          <span>Neo4j: {telemetry.neo4jStatus}</span>
        </div>
      </div>
    </div>
  );
};
