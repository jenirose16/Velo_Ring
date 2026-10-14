import React, { useState } from 'react';
import { Save, Check, RefreshCw, Shield, Database } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const [pollInterval, setPollInterval] = useState('5000');
  const [confidenceThreshold, setConfidenceThreshold] = useState('0.85');
  const [apiHost, setApiHost] = useState('http://localhost:8080');
  const [actuatorHost, setActuatorHost] = useState('http://localhost:8081');
  const [isSaved, setIsSaved] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 2000);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 840 }}>
      <div>
        <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
          Workstation & Pipeline Settings
        </h1>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
          Client-side dashboard polling parameters, telemetry connection targets, and detection alert sensitivity thresholds.
        </p>
      </div>

      <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {/* Telemetry Polling Section */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <RefreshCw size={15} style={{ color: 'var(--accent-primary)' }} />
              <span>Telemetry Polling Configuration</span>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 14, fontSize: 13 }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
                Health & Metrics Sync Frequency
              </label>
              <select
                value={pollInterval}
                onChange={(e) => setPollInterval(e.target.value)}
                style={{ width: 260 }}
              >
                <option value="3000">3 Seconds (High Frequency)</option>
                <option value="5000">5 Seconds (Engine Default)</option>
                <option value="10000">10 Seconds (Standard)</option>
                <option value="30000">30 Seconds (Low Impact)</option>
              </select>
              <div style={{ fontSize: 11.5, color: 'var(--text-tertiary)', marginTop: 4 }}>
                Interval at which the dashboard queries <code className="mono">/api/v1/health</code> and Micrometer actuator endpoints.
              </div>
            </div>
          </div>
        </div>

        {/* Endpoint Configuration Section */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <Database size={15} style={{ color: 'var(--status-healthy)' }} />
              <span>Target Connection Endpoints</span>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 14, fontSize: 13 }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
                Backend REST Base URL (Vite Proxy: /api)
              </label>
              <input
                type="text"
                value={apiHost}
                onChange={(e) => setApiHost(e.target.value)}
                style={{ width: '100%', maxWidth: 400 }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
                Spring Actuator Base URL (Vite Proxy: /actuator)
              </label>
              <input
                type="text"
                value={actuatorHost}
                onChange={(e) => setActuatorHost(e.target.value)}
                style={{ width: '100%', maxWidth: 400 }}
              />
            </div>
          </div>
        </div>

        {/* Alert Thresholds */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <Shield size={15} style={{ color: 'var(--status-warning)' }} />
              <span>Detection Sensitivity Thresholds</span>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 14, fontSize: 13 }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
                Minimum Confidence Score for Ring Escalation: {(parseFloat(confidenceThreshold) * 100).toFixed(0)}%
              </label>
              <input
                type="range"
                min="0.5"
                max="0.99"
                step="0.01"
                value={confidenceThreshold}
                onChange={(e) => setConfidenceThreshold(e.target.value)}
                style={{ width: '100%', maxWidth: 300 }}
              />
              <div style={{ fontSize: 11.5, color: 'var(--text-tertiary)', marginTop: 4 }}>
                Candidate rings with confidence above this threshold trigger prominent visual warnings in the event feed.
              </div>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <button type="submit" className="btn btn-primary">
            {isSaved ? <Check size={14} /> : <Save size={14} />}
            <span>{isSaved ? 'Settings Saved' : 'Save Configuration'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
