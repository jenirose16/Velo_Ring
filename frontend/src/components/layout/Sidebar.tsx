import React from 'react';
import {
  LayoutDashboard,
  ArrowLeftRight,
  Network,
  ShieldAlert,
  FlaskConical,
  Activity,
  Settings,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import type { NavigationTab } from '../../types';

interface SidebarProps {
  currentTab: NavigationTab;
  onSelectTab: (tab: NavigationTab) => void;
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  cycleCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  isCollapsed,
  onToggleCollapse,
  cycleCount
}) => {
  const navItems = [
    { id: 'overview' as NavigationTab, label: 'Overview', icon: LayoutDashboard },
    { id: 'explorer' as NavigationTab, label: 'Transaction Explorer', icon: ArrowLeftRight },
    { id: 'network' as NavigationTab, label: 'Network Analysis', icon: Network },
    { id: 'events' as NavigationTab, label: 'Detection Events', icon: ShieldAlert, badge: cycleCount > 0 ? cycleCount : undefined, badgeColor: 'var(--status-danger)' },
    { id: 'evaluation' as NavigationTab, label: 'Evaluation Lab', icon: FlaskConical },
    { id: 'health' as NavigationTab, label: 'System Health', icon: Activity },
    { id: 'settings' as NavigationTab, label: 'Settings', icon: Settings },
  ];

  return (
    <aside style={{
      width: isCollapsed ? 'var(--sidebar-collapsed-width)' : 'var(--sidebar-width)',
      height: '100vh',
      backgroundColor: 'var(--bg-app)',
      borderRight: '1px solid var(--border-subtle)',
      display: 'flex',
      flexDirection: 'column',
      transition: 'width var(--transition-normal)',
      flexShrink: 0,
      userSelect: 'none',
      zIndex: 20
    }}>
      {/* Brand Header */}
      <div style={{
        height: 'var(--topbar-height)',
        padding: isCollapsed ? '0' : '0 18px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: isCollapsed ? 'center' : 'space-between',
        borderBottom: '1px solid var(--border-subtle)',
      }}>
        {!isCollapsed ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {/* Custom VeloRing SVG Logomark: Interlocking Temporal Torus */}
            <svg width="24" height="24" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="14" cy="14" r="11" stroke="var(--border-strong)" strokeWidth="2.5" strokeDasharray="3 3"/>
              <path d="M7 14C7 10.134 10.134 7 14 7C17.866 7 21 10.134 21 14" stroke="var(--accent-primary)" strokeWidth="2.8" strokeLinecap="round"/>
              <path d="M21 14C21 17.866 17.866 21 14 21C11.5 21 9 19.5 8 17.5" stroke="var(--status-warning)" strokeWidth="2.8" strokeLinecap="round"/>
              <circle cx="14" cy="7" r="2.2" fill="var(--accent-primary)"/>
              <circle cx="8" cy="17.5" r="2.2" fill="var(--status-warning)"/>
            </svg>
            <div>
              <div style={{ fontWeight: 700, fontSize: 14, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
                Velo<span style={{ color: 'var(--accent-primary)' }}>Ring</span>
              </div>
              <div style={{ fontSize: 9.5, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>
                Temporal Graph Engine
              </div>
            </div>
          </div>
        ) : (
          <svg width="22" height="22" viewBox="0 0 28 28" fill="none">
            <circle cx="14" cy="14" r="11" stroke="var(--border-strong)" strokeWidth="2.5" strokeDasharray="3 3"/>
            <path d="M7 14C7 10.134 10.134 7 14 7" stroke="var(--accent-primary)" strokeWidth="2.8" strokeLinecap="round"/>
            <path d="M21 14C21 17.866 17.866 21 14 21" stroke="var(--status-warning)" strokeWidth="2.8" strokeLinecap="round"/>
          </svg>
        )}
      </div>

      {/* Nav List */}
      <nav style={{ flex: 1, padding: '14px 8px', display: 'flex', flexDirection: 'column', gap: 4, overflowY: 'auto' }}>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              title={isCollapsed ? item.label : undefined}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: isCollapsed ? '10px 0' : '9px 12px',
                justifyContent: isCollapsed ? 'center' : 'flex-start',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: isActive ? 'var(--bg-subtle)' : 'transparent',
                color: isActive ? 'var(--text-primary)' : 'var(--text-secondary)',
                fontWeight: isActive ? 600 : 400,
                border: isActive ? '1px solid var(--border-medium)' : '1px solid transparent',
                transition: 'all var(--transition-fast)',
                position: 'relative'
              }}
              onMouseEnter={(e) => {
                if (!isActive) {
                  e.currentTarget.style.backgroundColor = 'var(--bg-hover)';
                  e.currentTarget.style.color = 'var(--text-primary)';
                }
              }}
              onMouseLeave={(e) => {
                if (!isActive) {
                  e.currentTarget.style.backgroundColor = 'transparent';
                  e.currentTarget.style.color = 'var(--text-secondary)';
                }
              }}
            >
              <Icon size={17} style={{ color: isActive ? 'var(--accent-primary)' : 'currentColor', flexShrink: 0 }} />
              {!isCollapsed && (
                <span style={{ fontSize: 13, flex: 1, textAlign: 'left', whiteSpace: 'nowrap' }}>
                  {item.label}
                </span>
              )}
              {!isCollapsed && item.badge !== undefined && (
                <span style={{
                  backgroundColor: item.badgeColor,
                  color: '#ffffff',
                  fontSize: 10,
                  fontWeight: 700,
                  padding: '1px 6px',
                  borderRadius: 'var(--radius-full)'
                }}>
                  {item.badge}
                </span>
              )}
              {isCollapsed && item.badge !== undefined && (
                <span style={{
                  position: 'absolute',
                  top: 6,
                  right: 14,
                  width: 6,
                  height: 6,
                  backgroundColor: item.badgeColor,
                  borderRadius: '50%'
                }} />
              )}
            </button>
          );
        })}
      </nav>

      {/* Footer Info & Collapse Toggle */}
      <div style={{
        padding: '12px 14px',
        borderTop: '1px solid var(--border-subtle)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: isCollapsed ? 'center' : 'space-between',
        fontSize: 11,
        color: 'var(--text-tertiary)'
      }}>
        {!isCollapsed && (
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>VeloRing v0.0.1</div>
            <div>Phase 8 Institutional Demo</div>
          </div>
        )}
        <button
          onClick={onToggleCollapse}
          title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          style={{
            padding: 6,
            borderRadius: 'var(--radius-xs)',
            color: 'var(--text-tertiary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = 'var(--bg-hover)';
            e.currentTarget.style.color = 'var(--text-primary)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = 'transparent';
            e.currentTarget.style.color = 'var(--text-tertiary)';
          }}
        >
          {isCollapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
      </div>
    </aside>
  );
};
