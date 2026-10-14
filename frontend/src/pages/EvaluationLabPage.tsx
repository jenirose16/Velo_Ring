import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  Search,
  Info,
  Hash
} from 'lucide-react';
import type { BenchmarkRecord, BenchmarkStats } from '../types';
import { loadBenchmarkDataset, computeBenchmarkStats } from '../services/api';

export const EvaluationLabPage: React.FC = () => {
  const [records, setRecords] = useState<BenchmarkRecord[]>([]);
  const [stats, setStats] = useState<BenchmarkStats[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [seedSearch, setSeedSearch] = useState('');
  const [selectedWindow, setSelectedWindow] = useState<number | 'ALL'>('ALL');

  useEffect(() => {
    let isMounted = true;
    loadBenchmarkDataset()
      .then(data => {
        if (!isMounted) return;
        setRecords(data);
        setStats(computeBenchmarkStats(data));
        setIsLoading(false);
      })
      .catch(err => {
        if (!isMounted) return;
        setError(err.message);
        setIsLoading(false);
      });

    return () => { isMounted = false; };
  }, []);

  const filteredRecords = records.filter(r => {
    if (selectedWindow !== 'ALL' && r.windowSizeSec !== selectedWindow) return false;
    if (seedSearch.trim()) {
      return r.evaluatedSeed.toString().includes(seedSearch.trim());
    }
    return true;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
            Evaluation Lab & Window Sensitivity Benchmarks
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
            Empirical sensitivity evaluation of the temporal window engine comparing detection precision, recall, and F1 across sliding window sizes.
          </p>
        </div>

        <div className="badge badge-info">
          <span>Artifact: window_sensitivity_paired.csv</span>
        </div>
      </div>

      {/* Methodology & Reproducibility Notice */}
      <div style={{
        padding: '12px 16px',
        backgroundColor: 'var(--bg-subtle)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-sm)',
        display: 'flex',
        alignItems: 'flex-start',
        gap: 12,
        fontSize: 12,
        color: 'var(--text-secondary)',
        lineHeight: 1.45
      }}>
        <Info size={16} style={{ color: 'var(--accent-primary)', flexShrink: 0, marginTop: 2 }} />
        <div>
          <strong style={{ color: 'var(--text-primary)' }}>Experimental Design & Reproducibility Standards:</strong>
          {' '}Data drawn directly from Phase 8 paired seed benchmark suite (<code className="mono">backend/target/veloring-benchmarks/window_sensitivity_paired.csv</code>).
          Each experiment evaluates 10,000 synthetic transactions per seed with deterministic injected ground-truth 3-cycles.
          Synthetic evaluation metrics measure algorithmic detection fidelity and do not establish operational Anti-Money Laundering effectiveness on uncalibrated production rails.
        </div>
      </div>

      {/* Benchmark Statistics Table */}
      <div className="panel">
        <div className="panel-header">
          <div>
            <div className="panel-title">
              <BarChart3 size={16} style={{ color: 'var(--status-healthy)' }} />
              <span>Paired Window Size Sensitivity Comparison</span>
            </div>
            <div className="panel-subtitle">
              Macro-averaged evaluation metrics across 100 paired seed iterations (Seeds 52–151) under 60s allowed lateness.
            </div>
          </div>
        </div>

        {isLoading ? (
          <div style={{ padding: '36px 0', textAlign: 'center', color: 'var(--text-tertiary)' }}>
            Loading benchmark dataset artifact...
          </div>
        ) : error ? (
          <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--status-danger)' }}>
            Error loading benchmark dataset: {error}
          </div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Window Size</th>
                  <th>Evaluation Runs</th>
                  <th>Mean Precision</th>
                  <th>Mean Recall</th>
                  <th>Mean F1 Score</th>
                  <th>F1 StdDev (σ)</th>
                  <th>Min F1</th>
                  <th>Median F1</th>
                  <th>P95 F1</th>
                  <th>Max F1</th>
                </tr>
              </thead>
              <tbody>
                {stats.map(st => (
                  <tr key={st.windowSizeSec}>
                    <td className="mono" style={{ fontWeight: 700, color: 'var(--accent-primary)' }}>
                      {st.windowSizeSec}s Window
                    </td>
                    <td className="tabular-nums" style={{ color: 'var(--text-primary)' }}>
                      {st.sampleCount} seeds
                    </td>
                    <td className="tabular-nums" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {(st.meanPrecision * 100).toFixed(1)}%
                    </td>
                    <td className="tabular-nums" style={{ fontWeight: 600, color: 'var(--status-healthy)' }}>
                      {(st.meanRecall * 100).toFixed(1)}%
                    </td>
                    <td className="tabular-nums" style={{ fontWeight: 700, color: st.meanF1 > 0.8 ? 'var(--status-healthy)' : 'var(--status-warning)' }}>
                      {(st.meanF1 * 100).toFixed(1)}%
                    </td>
                    <td className="tabular-nums mono" style={{ color: 'var(--text-tertiary)' }}>
                      ±{(st.stdDevF1 * 100).toFixed(2)}%
                    </td>
                    <td className="tabular-nums" style={{ color: 'var(--text-secondary)' }}>
                      {(st.minF1 * 100).toFixed(1)}%
                    </td>
                    <td className="tabular-nums" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {(st.medianF1 * 100).toFixed(1)}%
                    </td>
                    <td className="tabular-nums" style={{ color: 'var(--text-primary)' }}>
                      {(st.p95F1 * 100).toFixed(1)}%
                    </td>
                    <td className="tabular-nums" style={{ fontWeight: 600, color: 'var(--status-healthy)' }}>
                      {(st.maxF1 * 100).toFixed(1)}%
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Filter and Raw Data Explorer */}
      <div className="panel">
        <div className="panel-header">
          <div>
            <div className="panel-title">
              <Hash size={15} />
              <span>Per-Seed Raw Benchmark Observations ({records.length} total samples)</span>
            </div>
            <div className="panel-subtitle">
              Cryptographically fingerprinted benchmark executions recorded by the JUnit 5 test harness.
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {/* Window filter buttons */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              {(['ALL', 10, 30, 60] as const).map(w => (
                <button
                  key={w}
                  className={`btn ${selectedWindow === w ? 'btn-primary' : 'btn-secondary'} btn-sm`}
                  onClick={() => setSelectedWindow(w)}
                >
                  {w === 'ALL' ? 'All Windows' : `${w}s`}
                </button>
              ))}
            </div>

            <div style={{ position: 'relative', width: 160 }}>
              <Search size={12} style={{ position: 'absolute', left: 8, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
              <input
                type="text"
                placeholder="Search seed..."
                value={seedSearch}
                onChange={(e) => setSeedSearch(e.target.value)}
                style={{ width: '100%', paddingLeft: 26, fontSize: 11.5, padding: '3px 8px 3px 24px' }}
              />
            </div>
          </div>
        </div>

        <div className="table-container" style={{ maxHeight: 380, overflowY: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Experiment ID</th>
                <th>Seed</th>
                <th>Window (s)</th>
                <th>Lateness (s)</th>
                <th>Precision</th>
                <th>Recall</th>
                <th>F1 Score</th>
                <th>Workload Fingerprint</th>
              </tr>
            </thead>
            <tbody>
              {filteredRecords.slice(0, 50).map((r, idx) => (
                <tr key={`${r.experimentId}-${r.evaluatedSeed}-${r.windowSizeSec}-${idx}`}>
                  <td className="mono" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                    {r.experimentId}
                  </td>
                  <td className="tabular-nums mono" style={{ color: 'var(--text-secondary)' }}>
                    {r.evaluatedSeed}
                  </td>
                  <td className="tabular-nums" style={{ color: 'var(--accent-primary)', fontWeight: 600 }}>
                    {r.windowSizeSec}s
                  </td>
                  <td className="tabular-nums" style={{ color: 'var(--text-secondary)' }}>
                    {r.allowedLatenessSec}s
                  </td>
                  <td className="tabular-nums" style={{ color: 'var(--text-primary)' }}>
                    {(r.precision * 100).toFixed(0)}%
                  </td>
                  <td className="tabular-nums" style={{ color: 'var(--status-healthy)', fontWeight: 600 }}>
                    {(r.recall * 100).toFixed(0)}%
                  </td>
                  <td className="tabular-nums" style={{ fontWeight: 700, color: r.f1 > 0.8 ? 'var(--status-healthy)' : 'var(--status-warning)' }}>
                    {(r.f1 * 100).toFixed(0)}%
                  </td>
                  <td className="mono" style={{ fontSize: 10, color: 'var(--text-tertiary)' }} title={r.baseWorkloadFingerprint}>
                    {r.baseWorkloadFingerprint.substring(0, 16)}...
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
