// ui.jsx — shared UI primitives for Auto HRV (dark, Material-informed)
// Exports to window: MIcon, Card, SectionLabel, MetricTile, StatusChip,
//   PillButton, TopBar, BottomNav, Divider

// Inline SVG icon set — renders reliably in screenshots, PDF, PPTX
// (icon-font ligatures don't rasterize in html-to-image exports).
const FILLED = new Set(['favorite', 'cardiology', 'ecg_heart', 'auto_awesome', 'stop', 'pause', 'play_arrow']);

function MIcon({ name, size = 22, fill = 0, color = 'currentColor', style }) {
  const sw = fill ? 2.3 : 1.9;
  const stroke = { fill: 'none', stroke: color, strokeWidth: sw, strokeLinecap: 'round', strokeLinejoin: 'round' };
  let body;
  switch (name) {
    case 'favorite':
    case 'cardiology':
    case 'ecg_heart':
      body = <path d="M12 20s-7-4.6-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.4-7 10-7 10z" fill={color} />;
      break;
    case 'monitor_heart':
      body = <path d="M3 12h3.5l2-5 3 10 2-5H21" {...stroke} />;
      break;
    case 'graphic_eq':
      body = <path d="M5 10v4M10 5v14M15 8v8M20 11v2" {...stroke} />;
      break;
    case 'air':
      body = <g {...stroke}><path d="M3 8h9.5a2.4 2.4 0 1 0-2.4-2.6" /><path d="M3 12h13a2.4 2.4 0 1 1-2.4 2.6" /><path d="M3 16h7" /></g>;
      break;
    case 'auto_graph':
      body = <g {...stroke}><path d="M4 18l5-5 3 2 7-8" /><path d="M16 7h3v3" /></g>;
      break;
    case 'timeline':
      body = <g {...stroke}><path d="M4 15l5-5 3 2 8-7" /><circle cx="4" cy="15" r="1.6" fill={color} stroke="none" /><circle cx="9" cy="10" r="1.6" fill={color} stroke="none" /><circle cx="12" cy="12" r="1.6" fill={color} stroke="none" /><circle cx="20" cy="5" r="1.6" fill={color} stroke="none" /></g>;
      break;
    case 'self_improvement':
      body = <g {...stroke}><circle cx="12" cy="12" r="8" /><circle cx="12" cy="12" r="2.4" fill={color} stroke="none" /></g>;
      break;
    case 'battery_5_bar':
      body = <g {...stroke}><rect x="3" y="8" width="16" height="8" rx="2" /><path d="M21 11v2" /><rect x="5" y="10" width="10" height="4" rx="1" fill={color} stroke="none" /></g>;
      break;
    case 'signal_cellular_alt':
      body = <g {...stroke}><path d="M5 18v-3M11 18v-7M17 18V7" /></g>;
      break;
    case 'bluetooth_searching':
    case 'bluetooth_disabled':
      body = <path d="M7 8.5L17 15.5l-5 3.5V5l5 3.5L7 15.5" {...stroke} />;
      break;
    case 'auto_awesome':
      body = <path d="M12 3l1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7z" fill={color} />;
      break;
    case 'arrow_back':
      body = <g {...stroke}><path d="M19 12H5" /><path d="M12 19l-7-7 7-7" /></g>;
      break;
    case 'stop':
      body = <rect x="6" y="6" width="12" height="12" rx="2.5" fill={color} />;
      break;
    case 'pause':
      body = <path d="M7 5h3v14H7zM14 5h3v14h-3z" fill={color} />;
      break;
    case 'play_arrow':
      body = <path d="M8 5.5v13l10-6.5z" fill={color} />;
      break;
    default:
      body = <circle cx="12" cy="12" r="7" {...stroke} />;
  }
  return (
    <svg width={size} height={size} viewBox="0 0 24 24"
      style={{ display: 'inline-block', flexShrink: 0, verticalAlign: 'middle', ...style }}>
      {body}
    </svg>
  );
}

function Card({ children, style, pad = 18, onClick }) {
  return (
    <div
      onClick={onClick}
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--outline)',
        borderRadius: 'var(--r-card)',
        padding: pad,
        boxShadow: '0 1px 0 rgba(255,255,255,0.03) inset, 0 10px 30px rgba(0,0,0,0.35)',
        boxSizing: 'border-box',
        ...style,
      }}
    >{children}</div>
  );
}

function Divider({ style }) {
  return <div style={{ height: 1, background: 'var(--outline)', ...style }} />;
}

function SectionLabel({ children, style }) {
  return (
    <div style={{
      fontFamily: 'var(--mono)', fontSize: 11, fontWeight: 600,
      letterSpacing: '0.14em', textTransform: 'uppercase',
      color: 'var(--faint)', ...style,
    }}>{children}</div>
  );
}

function StatusChip({ connected, battery, device, onClick }) {
  return (
    <button onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 8,
      background: 'var(--surface-2)', border: '1px solid var(--outline)',
      borderRadius: 999, padding: '6px 12px 6px 10px', cursor: 'pointer',
      font: 'inherit',
    }}>
      <span style={{
        width: 8, height: 8, borderRadius: 999,
        background: connected ? 'var(--accent)' : 'var(--faint)',
        boxShadow: connected ? '0 0 0 3px color-mix(in srgb, var(--accent) 25%, transparent)' : 'none',
        animation: connected ? 'cc-pulse 2.4s ease-in-out infinite' : 'none',
      }} />
      <span style={{ fontSize: 13, color: 'var(--on)', fontWeight: 500 }}>{device}</span>
      {connected && battery != null && (
        <span style={{ fontSize: 12, color: 'var(--muted)', fontFamily: 'var(--mono)', fontVariantNumeric: 'tabular-nums' }}>{battery}%</span>
      )}
    </button>
  );
}

function PillButton({ children, icon, variant = 'filled', onClick, style, full }) {
  const variants = {
    filled: { background: 'var(--accent)', color: '#06181b', border: '1px solid transparent', fontWeight: 600 },
    tonal: { background: 'color-mix(in srgb, var(--accent) 16%, var(--surface-2))', color: 'var(--accent)', border: '1px solid color-mix(in srgb, var(--accent) 24%, transparent)', fontWeight: 600 },
    outline: { background: 'transparent', color: 'var(--on)', border: '1px solid var(--outline-strong)', fontWeight: 500 },
    ghost: { background: 'transparent', color: 'var(--muted)', border: '1px solid transparent', fontWeight: 500 },
  };
  return (
    <button onClick={onClick} style={{
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 9,
      height: 52, padding: '0 24px', borderRadius: 999, cursor: 'pointer',
      fontSize: 15.5, fontFamily: 'var(--ui)', letterSpacing: '0.01em',
      width: full ? '100%' : 'auto',
      transition: 'filter .15s ease, transform .1s ease',
      ...variants[variant], ...style,
    }}
      onMouseDown={(e) => e.currentTarget.style.transform = 'scale(0.98)'}
      onMouseUp={(e) => e.currentTarget.style.transform = 'scale(1)'}
      onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
    >
      {icon && <MIcon name={icon} size={20} fill={variant === 'filled' ? 1 : 0} />}
      {children}
    </button>
  );
}

function TopBar({ title, connected, battery, device, onDeviceTap }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 18px 12px', flexShrink: 0,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{
          width: 30, height: 30, borderRadius: 9,
          background: 'color-mix(in srgb, var(--accent) 18%, transparent)',
          display: 'grid', placeItems: 'center',
        }}>
          <MIcon name="favorite" size={17} fill={1} color="var(--accent)" />
        </div>
        <span style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.01em', color: 'var(--on)', whiteSpace: 'nowrap' }}>{title}</span>
      </div>
      <StatusChip connected={connected} battery={battery} device={device} onClick={onDeviceTap} />
    </div>
  );
}

Object.assign(window, {
  MIcon, Card, Divider, SectionLabel, StatusChip, PillButton, TopBar,
});
