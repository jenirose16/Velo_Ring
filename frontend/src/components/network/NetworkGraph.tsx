import React, { useEffect, useRef, useState } from 'react';
import cytoscape from 'cytoscape';
import type { Core } from 'cytoscape';
import {
  Maximize2,
  ZoomIn,
  ZoomOut,
  RotateCcw,
  Info,
  Layers,
  X,
  ArrowRight,
  ShieldAlert
} from 'lucide-react';
import type { GraphNodeData, GraphEdgeData } from '../../types';

interface NetworkGraphProps {
  nodes: GraphNodeData[];
  edges: GraphEdgeData[];
  height?: string | number;
  highlightCycleOnly?: boolean;
  onSelectNode?: (nodeId: string | null) => void;
  onSelectEdge?: (edgeId: string | null) => void;
}

export const NetworkGraph: React.FC<NetworkGraphProps> = ({
  nodes,
  edges,
  height = 420,
  highlightCycleOnly = false,
  onSelectNode,
  onSelectEdge
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const cyRef = useRef<Core | null>(null);

  const [selectedNode, setSelectedNode] = useState<GraphNodeData | null>(null);
  const [selectedEdge, setSelectedEdge] = useState<GraphEdgeData | null>(null);
  const [activeLayout, setActiveLayout] = useState<'cose' | 'circle' | 'concentric'>('cose');

  // Initialize and update Cytoscape
  useEffect(() => {
    if (!containerRef.current) return;

    // Filter elements if highlightCycleOnly is on
    const filteredNodes = highlightCycleOnly ? nodes.filter(n => n.isCycleMember) : nodes;
    const filteredEdges = highlightCycleOnly ? edges.filter(e => e.isCycleEdge) : edges;

    const elements = [
      ...filteredNodes.map(n => ({
        group: 'nodes' as const,
        data: {
          id: n.id,
          label: n.id,
          isCycle: n.isCycleMember ? 'true' : 'false',
          volume: n.totalVolume,
          inDegree: n.inDegree,
          outDegree: n.outDegree
        }
      })),
      ...filteredEdges.map(e => ({
        group: 'edges' as const,
        data: {
          id: e.id,
          source: e.source,
          target: e.target,
          amount: e.amount,
          currency: e.currency,
          txId: e.txId,
          eventTime: e.eventTime,
          isCycle: e.isCycleEdge ? 'true' : 'false'
        }
      }))
    ];

    if (cyRef.current && !cyRef.current.destroyed()) {
      try {
        cyRef.current.removeAllListeners();
        cyRef.current.destroy();
      } catch {
        // ignore destroyed instance
      }
      cyRef.current = null;
    }

    const cy = cytoscape({
      container: containerRef.current,
      elements,
      style: [
        // Default Node Style
        {
          selector: 'node',
          style: {
            'label': 'data(label)',
            'font-family': 'JetBrains Mono, monospace',
            'font-size': '10px',
            'font-weight': 600,
            'color': '#f1f3f7',
            'text-valign': 'bottom',
            'text-margin-y': 6,
            'background-color': '#1f2638',
            'border-width': 2,
            'border-color': '#3b4866',
            'width': 34,
            'height': 34,
            'transition-property': 'background-color, border-color, border-width, width, height',
            'transition-duration': 0.2
          }
        },
        // Candidate Cycle Node (Amber/Crimson Institutional Highlight)
        {
          selector: 'node[isCycle = "true"]',
          style: {
            'background-color': '#2a1a1c',
            'border-color': '#ef4444',
            'border-width': 2.5,
            'color': '#ffccd0',
            'width': 40,
            'height': 40
          }
        },
        // Node Selected State
        {
          selector: 'node:selected',
          style: {
            'border-color': '#38bdf8',
            'border-width': 3.5,
            'background-color': '#0f314d'
          }
        },
        // Edge Default Style
        {
          selector: 'edge',
          style: {
            'width': 1.6,
            'line-color': '#333f57',
            'target-arrow-color': '#475778',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
            'arrow-scale': 1.1,
            'opacity': 0.85
          }
        },
        // Candidate Cycle Edge Style (Directed Mule Loop)
        {
          selector: 'edge[isCycle = "true"]',
          style: {
            'width': 2.6,
            'line-color': '#f59e0b',
            'target-arrow-color': '#f59e0b',
            'target-arrow-shape': 'triangle',
            'arrow-scale': 1.3,
            'opacity': 1.0,
            'line-dash-pattern': [6, 3]
          }
        },
        // Edge Selected State
        {
          selector: 'edge:selected',
          style: {
            'width': 3.5,
            'line-color': '#38bdf8',
            'target-arrow-color': '#38bdf8',
            'opacity': 1.0
          }
        }
      ],
      layout: {
        name: activeLayout,
        animate: true,
        animationDuration: 300,
        padding: 40,
        ...(activeLayout === 'cose' ? {
          nodeRepulsion: () => 650000,
          idealEdgeLength: () => 110,
          gravity: 0.25
        } : {})
      },
      minZoom: 0.4,
      maxZoom: 3.5,
      wheelSensitivity: 0.2
    });

    // Event handlers
    cy.on('tap', 'node', (evt) => {
      const node = evt.target;
      const id = node.id();
      const nodeData = nodes.find(n => n.id === id) || null;
      setSelectedNode(nodeData);
      setSelectedEdge(null);
      if (onSelectNode) onSelectNode(id);
    });

    cy.on('tap', 'edge', (evt) => {
      const edge = evt.target;
      const id = edge.id();
      const edgeData = edges.find(e => e.id === id) || null;
      setSelectedEdge(edgeData);
      setSelectedNode(null);
      if (onSelectEdge) onSelectEdge(id);
    });

    cy.on('tap', (evt) => {
      if (evt.target === cy) {
        setSelectedNode(null);
        setSelectedEdge(null);
        if (onSelectNode) onSelectNode(null);
        if (onSelectEdge) onSelectEdge(null);
      }
    });

    cyRef.current = cy;

    return () => {
      try {
        if (!cy.destroyed()) {
          cy.removeAllListeners();
          cy.destroy();
        }
      } catch {
        // ignore
      }
      cyRef.current = null;
    };
  }, [nodes, edges, activeLayout, highlightCycleOnly]);

  const handleZoomIn = () => cyRef.current?.zoom(cyRef.current.zoom() * 1.25);
  const handleZoomOut = () => cyRef.current?.zoom(cyRef.current.zoom() * 0.8);
  const handleFit = () => cyRef.current?.fit(undefined, 35);
  const handleReset = () => {
    setSelectedNode(null);
    setSelectedEdge(null);
    cyRef.current?.fit(undefined, 35);
  };

  return (
    <div style={{ position: 'relative', width: '100%', height, backgroundColor: 'var(--bg-canvas)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)', overflow: 'hidden' }}>

      {/* Cytoscape Canvas Container */}
      <div ref={containerRef} style={{ width: '100%', height: '100%' }} />

      {/* Floating Toolbar Top Right */}
      <div style={{
        position: 'absolute',
        top: 14,
        right: 14,
        display: 'flex',
        alignItems: 'center',
        gap: 6,
        backgroundColor: 'var(--bg-surface-elevated)',
        border: '1px solid var(--border-medium)',
        borderRadius: 'var(--radius-sm)',
        padding: '3px 5px',
        boxShadow: 'var(--shadow-md)',
        zIndex: 5
      }}>
        <button className="btn btn-ghost btn-sm" onClick={handleZoomIn} title="Zoom In">
          <ZoomIn size={14} />
        </button>
        <button className="btn btn-ghost btn-sm" onClick={handleZoomOut} title="Zoom Out">
          <ZoomOut size={14} />
        </button>
        <button className="btn btn-ghost btn-sm" onClick={handleFit} title="Fit View">
          <Maximize2 size={14} />
        </button>
        <div style={{ width: 1, height: 16, backgroundColor: 'var(--border-subtle)' }} />
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => setActiveLayout(l => l === 'cose' ? 'concentric' : (l === 'concentric' ? 'circle' : 'cose'))}
          title={`Switch Layout (Current: ${activeLayout})`}
        >
          <Layers size={14} />
          <span style={{ fontSize: 10.5, textTransform: 'capitalize' }}>{activeLayout}</span>
        </button>
        <button className="btn btn-ghost btn-sm" onClick={handleReset} title="Reset Selection">
          <RotateCcw size={14} />
        </button>
      </div>

      {/* Top Left Watermark Badge */}
      <div style={{
        position: 'absolute',
        top: 14,
        left: 14,
        display: 'flex',
        alignItems: 'center',
        gap: 6,
        backgroundColor: 'rgba(20, 23, 32, 0.88)',
        backdropFilter: 'blur(4px)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-xs)',
        padding: '3px 8px',
        fontSize: 10.5,
        color: 'var(--text-secondary)',
        zIndex: 5
      }}>
        <Info size={12} style={{ color: 'var(--accent-primary)' }} />
        <span>Interactive Network Visualizer • Directed Transfers</span>
      </div>

      {/* Floating Inspector Panel for Selected Node / Edge */}
      {(selectedNode || selectedEdge) && (
        <div style={{
          position: 'absolute',
          bottom: 14,
          right: 14,
          width: 290,
          backgroundColor: 'var(--bg-surface-elevated)',
          border: '1px solid var(--border-medium)',
          borderRadius: 'var(--radius-sm)',
          padding: '14px 16px',
          boxShadow: 'var(--shadow-lg)',
          zIndex: 6,
          animation: 'fadeIn 180ms ease-out'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              {selectedNode ? 'Account Inspection' : 'Transaction Edge Inspection'}
            </span>
            <button
              onClick={() => { setSelectedNode(null); setSelectedEdge(null); }}
              style={{ color: 'var(--text-tertiary)', padding: 2 }}
            >
              <X size={14} />
            </button>
          </div>

          {selectedNode && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 7, fontSize: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span className="mono" style={{ fontWeight: 700, fontSize: 13, color: 'var(--text-primary)' }}>
                  {selectedNode.id}
                </span>
                {selectedNode.isCycleMember && (
                  <span className="badge badge-warning" style={{ fontSize: 9.5 }}>
                    <ShieldAlert size={10} /> Cycle Member
                  </span>
                )}
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>In-Degree (Deposits):</span>
                <span className="tabular-nums" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{selectedNode.inDegree}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>Out-Degree (Transfers):</span>
                <span className="tabular-nums" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{selectedNode.outDegree}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>Total Volume:</span>
                <span className="tabular-nums" style={{ color: 'var(--status-healthy)', fontWeight: 600 }}>
                  ${selectedNode.totalVolume.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                </span>
              </div>
            </div>
          )}

          {selectedEdge && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 7, fontSize: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span className="mono" style={{ fontWeight: 600, fontSize: 11.5, color: 'var(--text-primary)' }}>
                  {selectedEdge.txId}
                </span>
                {selectedEdge.isCycleEdge && (
                  <span className="badge badge-warning" style={{ fontSize: 9.5 }}>
                    Mule Hop
                  </span>
                )}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border-subtle)' }}>
                <span className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{selectedEdge.source}</span>
                <ArrowRight size={13} style={{ color: 'var(--status-warning)' }} />
                <span className="mono" style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{selectedEdge.target}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>Amount:</span>
                <span className="tabular-nums" style={{ color: 'var(--text-primary)', fontWeight: 700 }}>
                  ${selectedEdge.amount.toFixed(2)} {selectedEdge.currency}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>Timestamp (UTC):</span>
                <span className="mono" style={{ fontSize: 11 }}>{selectedEdge.eventTime.replace('T', ' ').replace('Z', '')}</span>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Bottom Left Legend */}
      <div style={{
        position: 'absolute',
        bottom: 14,
        left: 14,
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        backgroundColor: 'rgba(20, 23, 32, 0.92)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-sm)',
        padding: '5px 12px',
        fontSize: 11,
        color: 'var(--text-secondary)',
        zIndex: 5
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
          <div style={{ width: 9, height: 9, borderRadius: '50%', backgroundColor: '#1f2638', border: '1.5px solid #3b4866' }} />
          <span>Account Node</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
          <div style={{ width: 9, height: 9, borderRadius: '50%', backgroundColor: '#2a1a1c', border: '1.5px solid #ef4444' }} />
          <span>Candidate Ring Account</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
          <div style={{ width: 14, height: 2, backgroundColor: '#333f57' }} />
          <span>Direct Transfer</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
          <div style={{ width: 14, height: 2, backgroundColor: '#f59e0b', borderTop: '1px dashed #f59e0b' }} />
          <span>Closed Mule Hop</span>
        </div>
      </div>
    </div>
  );
};
