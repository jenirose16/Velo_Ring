import React, { useState, useMemo } from 'react';
import {
  Filter,
  ArrowRight,
  Copy,
  Check,
  ShieldAlert,
  Hash,
  X
} from 'lucide-react';
import { TransactionTable } from '../components/transactions/TransactionTable';
import type { Transaction } from '../types';

interface TransactionExplorerPageProps {
  transactions: Transaction[];
  selectedTx: Transaction | null;
  onSelectTx: (tx: Transaction | null) => void;
}

export const TransactionExplorerPage: React.FC<TransactionExplorerPageProps> = ({
  transactions,
  selectedTx,
  onSelectTx
}) => {
  const [channelFilter, setChannelFilter] = useState<string>('ALL');
  const [flagFilter, setFlagFilter] = useState<'ALL' | 'FLAGGED' | 'STANDARD'>('ALL');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const filteredTransactions = useMemo(() => {
    return transactions.filter(tx => {
      if (channelFilter !== 'ALL' && tx.channel !== channelFilter) return false;
      if (flagFilter === 'FLAGGED' && !tx.isCandidateCycle) return false;
      if (flagFilter === 'STANDARD' && tx.isCandidateCycle) return false;
      return true;
    });
  }, [transactions, channelFilter, flagFilter]);

  const handleCopy = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 1500);
  };

  const totalVolume = transactions.reduce((acc, tx) => acc + tx.amount, 0);
  const flaggedCount = transactions.filter(tx => tx.isCandidateCycle).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header */}
      <div>
        <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
          Transaction Stream Explorer
        </h1>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
          Search, filter, and inspect payment transactions ingested from Apache Kafka <code className="mono">payment-events</code>.
        </p>
      </div>

      {/* Quick Summary Pill Bar */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
        gap: 12
      }}>
        <div className="panel" style={{ padding: '12px 16px' }}>
          <div style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase', fontWeight: 600 }}>Total Stream Volume</div>
          <div className="tabular-nums" style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', marginTop: 4 }}>
            ${totalVolume.toLocaleString('en-US', { minimumFractionDigits: 2 })}
          </div>
        </div>
        <div className="panel" style={{ padding: '12px 16px' }}>
          <div style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase', fontWeight: 600 }}>Total Ingested Events</div>
          <div className="tabular-nums" style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', marginTop: 4 }}>
            {transactions.length}
          </div>
        </div>
        <div className="panel" style={{ padding: '12px 16px' }}>
          <div style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase', fontWeight: 600 }}>Cycle Candidates</div>
          <div className="tabular-nums" style={{ fontSize: 20, fontWeight: 700, color: 'var(--status-warning)', marginTop: 4 }}>
            {flaggedCount} Hops
          </div>
        </div>
        <div className="panel" style={{ padding: '12px 16px' }}>
          <div style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase', fontWeight: 600 }}>Temporal Window</div>
          <div className="tabular-nums mono" style={{ fontSize: 20, fontWeight: 700, color: 'var(--accent-primary)', marginTop: 4 }}>
            60s SLIDING
          </div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="panel" style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <Filter size={14} style={{ color: 'var(--text-tertiary)' }} />
          <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)' }}>Filters:</span>

          {/* Channel selector */}
          <select
            value={channelFilter}
            onChange={(e) => setChannelFilter(e.target.value)}
            style={{ fontSize: 12, padding: '4px 8px' }}
          >
            <option value="ALL">All Payment Channels</option>
            <option value="WEB">WEB Rail</option>
            <option value="MOBILE">MOBILE Rail</option>
            <option value="ACH">ACH Inbound</option>
            <option value="POS">POS Retail</option>
            <option value="SWIFT">SWIFT Wire</option>
          </select>

          {/* Flag selector */}
          <select
            value={flagFilter}
            onChange={(e) => setFlagFilter(e.target.value as any)}
            style={{ fontSize: 12, padding: '4px 8px' }}
          >
            <option value="ALL">All Transaction Types</option>
            <option value="FLAGGED">Cycle Ring Candidates Only</option>
            <option value="STANDARD">Standard Transfers Only</option>
          </select>
        </div>

        <button
          className="btn btn-ghost btn-sm"
          onClick={() => { setChannelFilter('ALL'); setFlagFilter('ALL'); }}
        >
          <span>Reset Filters</span>
        </button>
      </div>

      {/* Main Split: Table + Detail Inspector */}
      <div style={{ display: 'grid', gridTemplateColumns: selectedTx ? '1fr 340px' : '1fr', gap: 18, transition: 'all var(--transition-normal)' }}>
        <div className="panel">
          <TransactionTable
            transactions={filteredTransactions}
            onSelectTransaction={onSelectTx}
            pageSize={12}
          />
        </div>

        {/* Selected Transaction Inspector Card */}
        {selectedTx && (
          <div className="panel" style={{ height: 'fit-content', position: 'sticky', top: 20 }}>
            <div className="panel-header" style={{ marginBottom: 14 }}>
              <div className="panel-title">
                <Hash size={14} />
                <span>Transaction Inspector</span>
              </div>
              <button
                onClick={() => onSelectTx(null)}
                style={{ color: 'var(--text-tertiary)', padding: 2 }}
              >
                <X size={15} />
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 12.5 }}>
              <div>
                <span style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Transaction ID</span>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 2 }}>
                  <span className="mono" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{selectedTx.transactionId}</span>
                  <button
                    onClick={() => handleCopy(selectedTx.transactionId, 'txId')}
                    style={{ color: 'var(--text-tertiary)' }}
                  >
                    {copiedKey === 'txId' ? <Check size={13} style={{ color: 'var(--status-healthy)' }} /> : <Copy size={13} />}
                  </button>
                </div>
              </div>

              <div style={{ padding: '10px 12px', backgroundColor: 'var(--bg-canvas)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)' }}>
                <div style={{ fontSize: 11, color: 'var(--text-tertiary)', marginBottom: 6 }}>TRANSFER PATH</div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span className="mono" style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{selectedTx.senderAccountId}</span>
                  <ArrowRight size={14} style={{ color: 'var(--status-warning)' }} />
                  <span className="mono" style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{selectedTx.receiverAccountId}</span>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Amount:</span>
                <span className="tabular-nums" style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: 14 }}>
                  ${selectedTx.amount.toFixed(2)} {selectedTx.currency}
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Event Time:</span>
                <span className="mono" style={{ fontSize: 11.5 }}>{selectedTx.eventTime}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Ingest Channel:</span>
                <span className="badge badge-neutral">{selectedTx.channel}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Detection Status:</span>
                {selectedTx.isCandidateCycle ? (
                  <span className="badge badge-warning">
                    <ShieldAlert size={10} /> Mule Candidate
                  </span>
                ) : (
                  <span className="badge badge-healthy">Standard Transfer</span>
                )}
              </div>

              {selectedTx.role && (
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                  <span style={{ color: 'var(--text-secondary)' }}>Scenario Role:</span>
                  <span className="mono" style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{selectedTx.role}</span>
                </div>
              )}

              {/* Raw JSON Payload Accordion */}
              <div style={{ marginTop: 6 }}>
                <div style={{ fontSize: 11, color: 'var(--text-tertiary)', marginBottom: 4, textTransform: 'uppercase' }}>
                  Raw Kafka Event Payload
                </div>
                <pre className="mono" style={{
                  padding: '8px 10px',
                  backgroundColor: 'var(--bg-canvas)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-xs)',
                  fontSize: 10.5,
                  overflowX: 'auto',
                  color: 'var(--text-secondary)'
                }}>
                  {JSON.stringify(selectedTx, null, 2)}
                </pre>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
