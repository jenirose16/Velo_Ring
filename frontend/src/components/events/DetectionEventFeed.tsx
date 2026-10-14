import React, { useState } from 'react';
import {
  ShieldAlert,
  ArrowRight,
  Clock,
  ChevronRight
} from 'lucide-react';
import type { CycleDetectionEvent } from '../../types';

interface DetectionEventFeedProps {
  events: CycleDetectionEvent[];
  onSelectEvent?: (event: CycleDetectionEvent) => void;
  onNavigateToNetwork?: () => void;
}

export const DetectionEventFeed: React.FC<DetectionEventFeedProps> = ({
  events,
  onSelectEvent,
  onNavigateToNetwork
}) => {
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);

  return (
    <div className="panel" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div className="panel-header">
        <div>
          <div className="panel-title">
            <ShieldAlert size={16} style={{ color: 'var(--status-danger)' }} />
            <span>Candidate Ring Detections (`NOVEL_CYCLE_DETECTED`)</span>
          </div>
          <div className="panel-subtitle">
            Synchronously identified directed transaction cycles evaluated across the 60s temporal sliding window.
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="badge badge-danger">
            {events.length} Suspicious Pattern{events.length === 1 ? '' : 's'}
          </span>
          {onNavigateToNetwork && (
            <button
              className="btn btn-secondary btn-sm"
              onClick={onNavigateToNetwork}
              title="Inspect topology in full network view"
            >
              <span>Explore Graph</span>
              <ChevronRight size={13} />
            </button>
          )}
        </div>
      </div>

      {events.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '32px 0', color: 'var(--text-tertiary)', fontSize: 13 }}>
          No novel cycle detection events in the current monitoring interval.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {events.map((evt) => {
            const isSelected = selectedEventId === evt.eventId;
            return (
              <div
                key={evt.eventId}
                onClick={() => {
                  setSelectedEventId(evt.eventId);
                  if (onSelectEvent) onSelectEvent(evt);
                }}
                style={{
                  backgroundColor: isSelected ? 'var(--bg-active)' : 'var(--bg-surface-elevated)',
                  border: isSelected ? '1px solid var(--border-focus)' : '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '12px 14px',
                  cursor: 'pointer',
                  transition: 'all var(--transition-fast)'
                }}
                onMouseEnter={(e) => {
                  if (!isSelected) e.currentTarget.style.borderColor = 'var(--border-medium)';
                }}
                onMouseLeave={(e) => {
                  if (!isSelected) e.currentTarget.style.borderColor = 'var(--border-subtle)';
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span className="badge badge-warning" style={{ fontSize: 10 }}>
                      {evt.category.replace(/_/g, ' ')}
                    </span>
                    <span className="mono" style={{ fontSize: 11.5, fontWeight: 600, color: 'var(--text-primary)' }}>
                      {evt.eventId}
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, color: 'var(--text-tertiary)' }}>
                    <Clock size={12} />
                    <span className="mono">{evt.detectionTime.replace('T', ' ').replace('Z', '')}</span>
                  </div>
                </div>

                {/* Implicated Cycle Flow visualization */}
                <div style={{
                  padding: '8px 12px',
                  backgroundColor: 'var(--bg-canvas)',
                  borderRadius: 'var(--radius-xs)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  marginBottom: 8,
                  overflowX: 'auto'
                }}>
                  {evt.edges.map((edge, idx) => (
                    <React.Fragment key={edge.txId}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 11.5 }}>
                        <span className="mono" style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{edge.senderAccountId}</span>
                        <ArrowRight size={12} style={{ color: 'var(--status-warning)' }} />
                        <span className="mono" style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{edge.receiverAccountId}</span>
                        {edge.amount && (
                          <span className="tabular-nums" style={{ color: 'var(--text-tertiary)', fontSize: 10.5 }}>
                            (${edge.amount.toFixed(0)})
                          </span>
                        )}
                      </div>
                      {idx < evt.edges.length - 1 && (
                        <span style={{ color: 'var(--border-strong)' }}>•</span>
                      )}
                    </React.Fragment>
                  ))}
                </div>

                {/* Details Footer */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 11, color: 'var(--text-secondary)' }}>
                  <div>
                    <span>Total Cycle Volume: </span>
                    <strong className="tabular-nums" style={{ color: 'var(--status-warning)' }}>
                      ${evt.totalCycleVolume.toFixed(2)} USD
                    </strong>
                    {evt.e2eDelayMs !== undefined && (
                      <span style={{ marginLeft: 12, color: 'var(--text-tertiary)' }}>
                        E2E Detection Delay: <span className="tabular-nums mono">{evt.e2eDelayMs}ms</span>
                      </span>
                    )}
                  </div>
                  <div style={{ fontSize: 10.5, color: 'var(--text-tertiary)' }}>
                    Source: <span className="mono">{evt.sourceAttribution}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
