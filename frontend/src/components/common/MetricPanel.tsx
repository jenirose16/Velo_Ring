import type { LucideIcon } from 'lucide-react';

interface MetricPanelProps {
  title: string;
  value: string | number;
  unit?: string;
  subtitle?: string;
  icon?: LucideIcon;
  source: string;
  statusVariant?: 'healthy' | 'warning' | 'danger' | 'info' | 'neutral';
  badgeText?: string;
}

export const MetricPanel: React.FC<MetricPanelProps> = ({
  title,
  value,
  unit,
  subtitle,
  icon: Icon,
  source,
  statusVariant = 'neutral',
  badgeText
}) => {
  const getAccentColor = () => {
    switch (statusVariant) {
      case 'healthy': return 'var(--status-healthy)';
      case 'warning': return 'var(--status-warning)';
      case 'danger': return 'var(--status-danger)';
      case 'info': return 'var(--accent-primary)';
      default: return 'var(--border-medium)';
    }
  };

  return (
    <div className="panel" style={{
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      position: 'relative',
      overflow: 'hidden'
    }}>
      {/* Top subtle accent bar */}
      <div style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        height: 2,
        backgroundColor: getAccentColor()
      }} />

      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
          <span style={{ fontSize: 11.5, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            {title}
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            {badgeText && (
              <span className={`badge badge-${statusVariant}`}>
                {badgeText}
              </span>
            )}
            {Icon && <Icon size={15} style={{ color: getAccentColor(), opacity: 0.85 }} />}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, margin: '4px 0 6px 0' }}>
          <span className="tabular-nums" style={{ fontSize: 26, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            {value}
          </span>
          {unit && (
            <span style={{ fontSize: 12, color: 'var(--text-tertiary)', fontWeight: 500 }}>
              {unit}
            </span>
          )}
        </div>

        {subtitle && (
          <div style={{ fontSize: 11.5, color: 'var(--text-secondary)', marginBottom: 8 }}>
            {subtitle}
          </div>
        )}
      </div>

      <div style={{
        paddingTop: 8,
        borderTop: '1px solid var(--border-subtle)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        fontSize: 10.5,
        color: 'var(--text-tertiary)'
      }}>
        <span>Source</span>
        <span className="mono" style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>
          {source}
        </span>
      </div>
    </div>
  );
};
