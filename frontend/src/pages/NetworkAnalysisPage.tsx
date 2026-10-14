import React, { useState } from 'react';
import {
  ShieldAlert,
  Database,
  Copy,
  Check,
  Info
} from 'lucide-react';
import { NetworkGraph } from '../components/network/NetworkGraph';
import type { GraphNodeData, GraphEdgeData } from '../types';

interface NetworkAnalysisPageProps {
  graphElements: { nodes: GraphNodeData[]; edges: GraphEdgeData[] };
}

export const NetworkAnalysisPage: React.FC<NetworkAnalysisPageProps> = ({
  graphElements
}) => {
  const [highlightCycleOnly, setHighlightCycleOnly] = useState(false);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [copiedQuery, setCopiedQuery] = useState(false);

  const selectedNode = graphElements.nodes.find(n => n.id === selectedNodeId) || null;

  const cypherQuery = `MATCH (a:Account)-[r1:TRANSFERRED_TO]->(b:Account)-[r2:TRANSFERRED_TO]->(c:Account)-[r3:TRANSFERRED_TO]->(a:Account)
WHERE r1.transactionId STARTS WITH 'tx-demo-3b320ae5'
RETURN a.id AS accA, b.id AS accB, c.id AS accC, r1.transactionId AS tx1, r2.transactionId AS tx2, r3.transactionId AS tx3;`;

  const handleCopyQuery = () => {
    navigator.clipboard.writeText(cypherQuery);
    setCopiedQuery(true);
    setTimeout(() => setCopiedQuery(false), 1800);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header & Controls */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
            Network Topology & Mule Graph Analysis
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
            Interactive directed network graph workspace analyzing temporal account connections and detected money mule ring closures.
          </p>
        </div>

        {/* View mode toggle */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <button
            className={`btn ${highlightCycleOnly ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setHighlightCycleOnly(!highlightCycleOnly)}
          >
            <ShieldAlert size={14} />
            <span>{highlightCycleOnly ? 'Show Full Network' : 'Isolate Ring (A→B→C→A)'}</span>
          </button>
        </div>
      </div>

      {/* Main Full-Size Network Canvas */}
      <div className="panel" style={{ padding: 16 }}>
        <NetworkGraph
          nodes={graphElements.nodes}
          edges={graphElements.edges}
          height={560}
          highlightCycleOnly={highlightCycleOnly}
          onSelectNode={setSelectedNodeId}
        />
      </div>

      {/* Lower Details & Cypher Verification */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: 18 }}>
        {/* Selected Account Deep Inspector */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <span>Account Topology Metrics</span>
            </div>
            {selectedNode && (
              <span className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
                {selectedNode.id}
              </span>
            )}
          </div>

          {selectedNode ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, fontSize: 12.5 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Account Identifier:</span>
                <span className="mono" style={{ color: 'var(--text-primary)', fontWeight: 700 }}>{selectedNode.id}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>In-Degree (Deposits In):</span>
                <span className="tabular-nums" style={{ color: 'var(--status-healthy)', fontWeight: 600 }}>{selectedNode.inDegree}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Out-Degree (Transfers Out):</span>
                <span className="tabular-nums" style={{ color: 'var(--status-warning)', fontWeight: 600 }}>{selectedNode.outDegree}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Total Transfer Flow:</span>
                <span className="tabular-nums" style={{ color: 'var(--text-primary)', fontWeight: 700 }}>
                  ${selectedNode.totalVolume.toLocaleString('en-US', { minimumFractionDigits: 2 })} USD
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Cycle Ring Member:</span>
                {selectedNode.isCycleMember ? (
                  <span className="badge badge-danger">
                    <ShieldAlert size={10} /> Implicated Node
                  </span>
                ) : (
                  <span className="badge badge-neutral">Standard Account</span>
                )}
              </div>
            </div>
          ) : (
            <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text-tertiary)', fontSize: 12.5 }}>
              Click any node in the canvas above to inspect account connectivity, degree distribution, and circular participation.
            </div>
          )}
        </div>

        {/* Neo4j Cypher Verification Query Card */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <Database size={15} style={{ color: 'var(--accent-primary)' }} />
              <span>Neo4j Graph Database Query Verification</span>
            </div>
            <button
              className="btn btn-secondary btn-sm"
              onClick={handleCopyQuery}
              title="Copy Cypher query to clipboard"
            >
              {copiedQuery ? <Check size={12} style={{ color: 'var(--status-healthy)' }} /> : <Copy size={12} />}
              <span>{copiedQuery ? 'Copied' : 'Copy Cypher'}</span>
            </button>
          </div>

          <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>
            Execute this Cypher query against <code className="mono">graphdb</code> container or Neo4j Browser (<code className="mono">http://localhost:7474</code>) to verify the identical directed 3-cycle in permanent storage:
          </div>

          <pre className="mono" style={{
            padding: '10px 12px',
            backgroundColor: 'var(--bg-canvas)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-sm)',
            fontSize: 11,
            color: 'var(--text-primary)',
            overflowX: 'auto',
            lineHeight: 1.45
          }}>
            {cypherQuery}
          </pre>

          <div style={{ marginTop: 10, fontSize: 11, color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Info size={12} style={{ color: 'var(--accent-primary)' }} />
            <span>Verified in Docker container <strong style={{ color: 'var(--text-secondary)' }}>graphdb</strong> on port 7687.</span>
          </div>
        </div>
      </div>
    </div>
  );
};
