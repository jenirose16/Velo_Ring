import React, { useState } from 'react';
import {
  ShieldAlert,
  Search,
  Clock,
  ArrowRight,
  Download,
  AlertTriangle
} from 'lucide-react';
import type { CycleDetectionEvent } from '../types';

interface DetectionEventsPageProps {
  events: CycleDetectionEvent[];
  onNavigateToNetwork: () => void;
}

export const DetectionEventsPage: React.FC<DetectionEventsPageProps> = ({
  events,
  onNavigateToNetwork
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedEvent, setSelectedEvent] = useState<CycleDetectionEvent | null>(events[0] || null);

  const filteredEvents = events.filter(e =>
    e.eventId.toLowerCase().includes(searchQuery.toLowerCase()) ||
    e.category.toLowerCase().includes(searchQuery.toLowerCase()) ||
    e.implicatedAccounts.some(acc => acc.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleExportJSON = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(events, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `veloring_detection_events_${new Date().toISOString().substring(0, 10)}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
            Ring Detection Events Log
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
            Synchronously identified cyclic patterns emitted by the ThreeAccountCycleDetector upon snapshot evaluation.
          </p>
        </div>

        <button
          className="btn btn-secondary btn-sm"
          onClick={handleExportJSON}
          title="Export detection log as JSON"
        >
          <Download size={13} />
          <span>Export Event Log</span>
        </button>
      </div>

      {/* Institutional Disclaimer Alert Box */}
      <div style={{
        padding: '10px 14px',
        backgroundColor: 'var(--bg-subtle)',
        border: '1px solid var(--status-warning-border)',
        borderRadius: 'var(--radius-sm)',
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        fontSize: 12,
        color: 'var(--text-secondary)'
      }}>
        <AlertTriangle size={16} style={{ color: 'var(--status-warning)', flexShrink: 0 }} />
        <span>
          <strong style={{ color: 'var(--status-warning)' }}>Regulatory & Research Clarification:</strong> Detection of a directed 3-account cycle identifies a structural topology candidate requiring investigation. It does not constitute legal proof of financial crime or definitive money laundering.
        </span>
      </div>

      {/* Search and Filters */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ position: 'relative', width: 320, maxWidth: '100%' }}>
          <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
          <input
            type="text"
            placeholder="Search by event ID, category, or account..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ width: '100%', paddingLeft: 30, fontSize: 12.5 }}
          />
        </div>

        <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
          Total Logged Alerts: <strong style={{ color: 'var(--text-primary)' }}>{filteredEvents.length}</strong>
        </div>
      </div>

      {/* Main Split: Event Cards List + Detail Inspector */}
      <div style={{ display: 'grid', gridTemplateColumns: selectedEvent ? '1fr 380px' : '1fr', gap: 18 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {filteredEvents.length === 0 ? (
            <div className="panel" style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-tertiary)' }}>
              No detection events match the query.
            </div>
          ) : (
            filteredEvents.map(evt => {
              const isSelected = selectedEvent?.eventId === evt.eventId;
              return (
                <div
                  key={evt.eventId}
                  onClick={() => setSelectedEvent(evt)}
                  className="panel"
                  style={{
                    cursor: 'pointer',
                    borderColor: isSelected ? 'var(--border-focus)' : 'var(--border-subtle)',
                    backgroundColor: isSelected ? 'var(--bg-active)' : 'var(--bg-surface)'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span className="badge badge-danger">
                        {evt.category.replace(/_/g, ' ')}
                      </span>
                      <span className="mono" style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary)' }}>
                        {evt.eventId}
                      </span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11.5, color: 'var(--text-tertiary)' }}>
                      <Clock size={12} />
                      <span className="mono">{evt.detectionTime.replace('T', ' ').replace('Z', '')}</span>
                    </div>
                  </div>

                  {/* Flow representation */}
                  <div style={{
                    padding: '8px 12px',
                    backgroundColor: 'var(--bg-canvas)',
                    borderRadius: 'var(--radius-xs)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                    marginBottom: 10
                  }}>
                    {evt.edges.map((edge, idx) => (
                      <React.Fragment key={edge.txId}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 12 }}>
                          <span className="mono" style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{edge.senderAccountId}</span>
                          <ArrowRight size={13} style={{ color: 'var(--status-warning)' }} />
                          <span className="mono" style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{edge.receiverAccountId}</span>
                          {edge.amount && (
                            <span className="tabular-nums" style={{ color: 'var(--text-secondary)', fontSize: 11 }}>
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

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-secondary)' }}>
                    <div>
                      <span>Cycle Volume: </span>
                      <strong className="tabular-nums" style={{ color: 'var(--status-warning)' }}>
                        ${evt.totalCycleVolume.toFixed(2)} USD
                      </strong>
                    </div>
                    <div>
                      Status: <span className="badge badge-warning">{evt.investigationStatus.replace(/_/g, ' ')}</span>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Selected Event Detail Panel */}
        {selectedEvent && (
          <div className="panel" style={{ height: 'fit-content', position: 'sticky', top: 20 }}>
            <div className="panel-header" style={{ marginBottom: 12 }}>
              <div className="panel-title">
                <ShieldAlert size={15} style={{ color: 'var(--status-danger)' }} />
                <span>Event Investigation</span>
              </div>
              <button
                className="btn btn-secondary btn-sm"
                onClick={onNavigateToNetwork}
                title="View in interactive graph"
              >
                <span>View Graph</span>
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 12.5 }}>
              <div>
                <span style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Event Identifier</span>
                <div className="mono" style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary)', marginTop: 2 }}>
                  {selectedEvent.eventId}
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Watermark at Detection:</span>
                <span className="mono" style={{ fontSize: 11.5 }}>{selectedEvent.watermark}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Confidence Score:</span>
                <span className="tabular-nums" style={{ color: 'var(--status-healthy)', fontWeight: 700 }}>
                  {(selectedEvent.confidenceScore * 100).toFixed(1)}%
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>End-to-End Processing Delay:</span>
                <span className="tabular-nums mono" style={{ color: 'var(--status-warning)', fontWeight: 600 }}>
                  {selectedEvent.e2eDelayMs || 420} ms
                </span>
              </div>

              <div>
                <span style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Involved Edges Sequence</span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 6 }}>
                  {selectedEvent.edges.map((edge, idx) => (
                    <div
                      key={edge.txId}
                      style={{
                        padding: '6px 10px',
                        backgroundColor: 'var(--bg-canvas)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-xs)',
                        fontSize: 11.5
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <span style={{ color: 'var(--text-tertiary)', fontWeight: 600 }}>Hop 0{idx + 1}</span>
                        <span className="mono" style={{ color: 'var(--accent-primary)', fontSize: 10.5 }}>{edge.txId}</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 3 }}>
                        <span className="mono" style={{ fontWeight: 700 }}>{edge.senderAccountId}</span>
                        <ArrowRight size={11} style={{ color: 'var(--status-warning)' }} />
                        <span className="mono" style={{ fontWeight: 700 }}>{edge.receiverAccountId}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div style={{ paddingTop: 8, borderTop: '1px solid var(--border-subtle)', fontSize: 11, color: 'var(--text-tertiary)' }}>
                Source Attribution: <span className="mono" style={{ color: 'var(--text-secondary)' }}>{selectedEvent.sourceAttribution}</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
