import React from 'react';
import { RefreshCw, ShieldCheck, Database, Radio, CheckCircle2, XCircle } from 'lucide-react';
import type { NavigationTab, SystemTelemetry } from '../../types';

interface TopBarProps {
  currentTab: NavigationTab;
  telemetry: SystemTelemetry;
  onRefresh: () => void;
  isRefreshing: boolean;
}

const TAB_TITLES: Record<NavigationTab, { title: string; breadcrumb: string }> = {
  overview: { title: 'Transaction Intelligence Overview', breadcrumb: 'VeloRing / Overview' },
  explorer: { title: 'Transaction Explorer', breadcrumb: 'Investigation / Transaction Stream' },
  network: { title: 'Temporal Network Graph Analysis', breadcrumb: 'Investigation / Network Topology' },
  events: { title: 'Ring Detection Event Log', breadcrumb: 'Intelligence / Detection Feed' },
  evaluation: { title: 'Evaluation & Sensitivity Lab', breadcrumb: 'Research / Benchmark Results' },
  health: { title: 'System Architecture & Telemetry', breadcrumb: 'Infrastructure / Diagnostics' },
  settings: { title: 'Workspace Settings & Configuration', breadcrumb: 'System / Configuration' },
};

export const TopBar: React.FC<TopBarProps> = ({
  currentTab,
  telemetry,
  onRefresh,
  isRefreshing
}) => {
  const currentInfo = TAB_TITLES[currentTab];

  return (
    <header style={{
      height: 'var(--topbar-height)',
      backgroundColor: 'var(--bg-app)',
      borderBottom: '1px solid var(--border-subtle)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px',
      flexShrink: 0,
      zIndex: 10
    }}>
      {/* Breadcrumb & Title */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        <div style={{ fontSize: 11, color: 'var(--text-tertiary)', letterSpacing: '0.02em', fontWeight: 500 }}>
          {currentInfo.breadcrumb}
        </div>
        <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.01em' }}>
          {currentInfo.title}
        </div>
      </div>

      {/* Real-time Status Badges & Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        {/* Demonstration Mode Tag */}
        <div className="badge badge-neutral" title="Interactive financial visualization using verified Phase 8 run 3b320ae5 data">
          <ShieldCheck size={12} style={{ color: 'var(--accent-primary)' }} />
          <span>Demo Fixture Active</span>
        </div>

        {/* Backend Connectivity Status */}
        <div
          className={`badge ${telemetry.backendHealth === 'UP' ? 'badge-healthy' : 'badge-danger'}`}
          title={telemetry.pollingError || 'Spring Boot Engine connected via port 8080'}
        >
          {telemetry.backendHealth === 'UP' ? (
            <CheckCircle2 size={12} />
          ) : (
            <XCircle size={12} />
          )}
          <span>Engine: {telemetry.backendHealth}</span>
        </div>

        {/* Neo4j Status */}
        <div
          className={`badge ${telemetry.neo4jStatus === 'UP' ? 'badge-healthy' : 'badge-neutral'}`}
          title="Bolt protocol connectivity verified by Spring Boot backend"
        >
          <Database size={12} />
          <span>Neo4j: {telemetry.neo4jStatus}</span>
        </div>

        {/* Kafka Ingest Status */}
        <div
          className={`badge ${telemetry.recordsConsumedTotal > 0 ? 'badge-healthy' : 'badge-neutral'}`}
          title={`Total records consumed by veloring-consumer-group: ${telemetry.recordsConsumedTotal}`}
        >
          <Radio size={12} />
          <span>Kafka Ingest: {telemetry.recordsConsumedTotal} tx</span>
        </div>

        {/* Divider */}
        <div style={{ width: 1, height: 22, backgroundColor: 'var(--border-subtle)' }} />

        {/* Refresh Action */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {telemetry.lastSuccessfulPoll && (
            <span style={{ fontSize: 11, color: 'var(--text-tertiary)', fontVariantNumeric: 'tabular-nums' }}>
              {new Date(telemetry.lastSuccessfulPoll).toLocaleTimeString()}
            </span>
          )}
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="btn btn-secondary btn-sm"
            title="Poll latest health and actuator telemetry"
          >
            <RefreshCw size={13} style={{ animation: isRefreshing ? 'spin 1s linear infinite' : 'none' }} />
            <span>Sync</span>
          </button>
        </div>
      </div>
      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </header>
  );
};
