import React, { useState, useMemo } from 'react';
import {
  Search,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Copy,
  Check,
  ExternalLink,
  ShieldAlert,
  ArrowRight
} from 'lucide-react';
import type { Transaction } from '../../types';

interface TransactionTableProps {
  transactions: Transaction[];
  onSelectTransaction?: (tx: Transaction) => void;
  pageSize?: number;
}

export const TransactionTable: React.FC<TransactionTableProps> = ({
  transactions,
  onSelectTransaction,
  pageSize = 10
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [sortField, setSortField] = useState<'eventTime' | 'amount' | 'transactionId'>('eventTime');
  const [sortAsc, setSortAsc] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

  // Search filter
  const filteredTransactions = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return transactions;
    return transactions.filter(tx =>
      tx.transactionId.toLowerCase().includes(q) ||
      tx.senderAccountId.toLowerCase().includes(q) ||
      tx.receiverAccountId.toLowerCase().includes(q) ||
      tx.channel.toLowerCase().includes(q) ||
      (tx.role && tx.role.toLowerCase().includes(q))
    );
  }, [transactions, searchQuery]);

  // Sorting
  const sortedTransactions = useMemo(() => {
    return [...filteredTransactions].sort((a, b) => {
      let cmp = 0;
      if (sortField === 'amount') {
        cmp = a.amount - b.amount;
      } else if (sortField === 'eventTime') {
        cmp = new Date(a.eventTime).getTime() - new Date(b.eventTime).getTime();
      } else {
        cmp = a.transactionId.localeCompare(b.transactionId);
      }
      return sortAsc ? cmp : -cmp;
    });
  }, [filteredTransactions, sortField, sortAsc]);

  // Pagination
  const totalPages = Math.max(1, Math.ceil(sortedTransactions.length / pageSize));
  const paginatedTransactions = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return sortedTransactions.slice(start, start + pageSize);
  }, [sortedTransactions, currentPage, pageSize]);

  const handleCopy = (text: string, e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(text);
    setCopiedId(text);
    setTimeout(() => setCopiedId(null), 1600);
  };

  const handleRowClick = (tx: Transaction) => {
    setSelectedTx(tx);
    if (onSelectTransaction) onSelectTransaction(tx);
  };

  const toggleSort = (field: 'eventTime' | 'amount' | 'transactionId') => {
    if (sortField === field) {
      setSortAsc(!sortAsc);
    } else {
      setSortField(field);
      setSortAsc(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {/* Search and Table Action Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', width: 280, maxWidth: '100%' }}>
          <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
          <input
            type="text"
            placeholder="Search by ID, sender, receiver..."
            value={searchQuery}
            onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(1); }}
            style={{ width: '100%', paddingLeft: 30, fontSize: 12.5 }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: 'var(--text-secondary)' }}>
          <span>Showing <strong style={{ color: 'var(--text-primary)' }}>{paginatedTransactions.length}</strong> of {filteredTransactions.length} events</span>
        </div>
      </div>

      {/* Table Element */}
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th onClick={() => toggleSort('transactionId')} style={{ cursor: 'pointer' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                  <span>Transaction ID</span>
                  <ArrowUpDown size={11} />
                </div>
              </th>
              <th>Transfer Direction</th>
              <th onClick={() => toggleSort('amount')} style={{ cursor: 'pointer' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                  <span>Amount</span>
                  <ArrowUpDown size={11} />
                </div>
              </th>
              <th onClick={() => toggleSort('eventTime')} style={{ cursor: 'pointer' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                  <span>Event Time (UTC)</span>
                  <ArrowUpDown size={11} />
                </div>
              </th>
              <th>Channel</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {paginatedTransactions.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '36px 0', color: 'var(--text-tertiary)' }}>
                  No transactions match the selected filter criteria.
                </td>
              </tr>
            ) : (
              paginatedTransactions.map((tx) => (
                <tr
                  key={tx.transactionId}
                  onClick={() => handleRowClick(tx)}
                  style={{ cursor: 'pointer' }}
                  className={selectedTx?.transactionId === tx.transactionId ? 'selected' : ''}
                >
                  <td className="mono" style={{ color: 'var(--text-primary)', fontWeight: 500 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span>{tx.transactionId}</span>
                      <button
                        onClick={(e) => handleCopy(tx.transactionId, e)}
                        title="Copy Transaction ID"
                        style={{ color: 'var(--text-tertiary)', padding: 2 }}
                      >
                        {copiedId === tx.transactionId ? <Check size={12} style={{ color: 'var(--status-healthy)' }} /> : <Copy size={12} />}
                      </button>
                    </div>
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
                      <span className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{tx.senderAccountId}</span>
                      <ArrowRight size={12} style={{ color: tx.isCandidateCycle ? 'var(--status-warning)' : 'var(--text-tertiary)' }} />
                      <span className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{tx.receiverAccountId}</span>
                    </div>
                  </td>
                  <td className="tabular-nums" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                    ${tx.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })} {tx.currency}
                  </td>
                  <td className="mono" style={{ fontSize: 11.5 }}>
                    {tx.eventTime.replace('T', ' ').replace('Z', '')}
                  </td>
                  <td>
                    <span className="badge badge-neutral" style={{ fontSize: 10 }}>
                      {tx.channel}
                    </span>
                  </td>
                  <td>
                    {tx.isCandidateCycle ? (
                      <span className="badge badge-warning" style={{ fontSize: 10 }}>
                        <ShieldAlert size={10} /> Ring Candidate
                      </span>
                    ) : (
                      <span className="badge badge-healthy" style={{ fontSize: 10 }}>
                        Persisted
                      </span>
                    )}
                  </td>
                  <td>
                    <button
                      className="btn btn-ghost btn-sm"
                      onClick={(e) => { e.stopPropagation(); handleRowClick(tx); }}
                      title="Inspect Transaction"
                    >
                      <ExternalLink size={13} />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 8, marginTop: 4 }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
            disabled={currentPage === 1}
          >
            <ChevronLeft size={14} />
            <span>Previous</span>
          </button>
          <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
            Page {currentPage} of {totalPages}
          </span>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
            disabled={currentPage === totalPages}
          >
            <span>Next</span>
            <ChevronRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
};
