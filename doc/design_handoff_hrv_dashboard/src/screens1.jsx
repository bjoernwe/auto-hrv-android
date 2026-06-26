// screens1.jsx — Connect screen + shared ScreenScroll
const { MIcon, Card, SectionLabel, PillButton } = window;

function ScreenScroll({ children }) {
  return (
    <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: '4px 18px 22px' }}>
      {children}
    </div>
  );
}

// ── CONNECT ────────────────────────────────────────────────
function ConnectScreen({ connected, battery, liveHR, onToggle }) {
  return (
    <ScreenScroll>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <Card pad={22} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', gap: 4 }}>
          <div style={{ position: 'relative', width: 116, height: 116, display: 'grid', placeItems: 'center', marginBottom: 4 }}>
            {connected && [0, 1].map((i) => (
              <div key={i} style={{
                position: 'absolute', inset: 0, borderRadius: '50%',
                border: '1px solid color-mix(in srgb, var(--accent) 40%, transparent)',
                animation: `cc-ripple 2.8s ${i * 1.4}s ease-out infinite`,
              }} />
            ))}
            <div style={{
              width: 88, height: 88, borderRadius: '50%',
              background: connected ? 'color-mix(in srgb, var(--accent) 16%, var(--surface-2))' : 'var(--surface-2)',
              border: `1px solid ${connected ? 'color-mix(in srgb, var(--accent) 35%, transparent)' : 'var(--outline)'}`,
              display: 'grid', placeItems: 'center',
            }}>
              <MIcon name="cardiology" size={40} fill={1} color={connected ? 'var(--accent)' : 'var(--muted)'} />
            </div>
          </div>
          <div style={{ fontSize: 21, fontWeight: 600, color: 'var(--on)', whiteSpace: 'nowrap' }}>Polar H10</div>
          <div style={{ fontFamily: 'var(--mono)', fontSize: 12, color: 'var(--faint)', letterSpacing: '0.08em' }}>SENSOR · E7A9AB27</div>
          <div style={{
            marginTop: 12, display: 'inline-flex', alignItems: 'center', gap: 8,
            padding: '7px 14px', borderRadius: 999,
            background: connected ? 'color-mix(in srgb, var(--accent) 14%, transparent)' : 'var(--surface-2)',
          }}>
            <span style={{ width: 7, height: 7, borderRadius: 999, background: connected ? 'var(--accent)' : 'var(--faint)' }} />
            <span style={{ fontSize: 13, fontWeight: 600, color: connected ? 'var(--accent)' : 'var(--muted)' }}>
              {connected ? 'Connected' : 'Not connected'}
            </span>
          </div>
        </Card>

        {connected && (
          <div style={{ display: 'flex', gap: 12 }}>
            {[
              { icon: 'battery_5_bar', v: `${battery}%`, l: 'Battery' },
              { icon: 'signal_cellular_alt', v: 'Strong', l: 'Signal' },
              { icon: 'ecg_heart', v: 'Good', l: 'Contact' },
            ].map((s) => (
              <div key={s.l} style={{
                flex: 1, background: 'var(--surface)', border: '1px solid var(--outline)',
                borderRadius: 'var(--r-tile)', padding: '14px 10px', textAlign: 'center',
              }}>
                <MIcon name={s.icon} size={20} color="var(--accent)" />
                <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--on)', marginTop: 6 }}>{s.v}</div>
                <div style={{ fontSize: 11.5, color: 'var(--muted)', marginTop: 2 }}>{s.l}</div>
              </div>
            ))}
          </div>
        )}

        {connected && (
          <Card style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <SectionLabel>Live stream</SectionLabel>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 6 }}>
                <span style={{ fontSize: 30, fontWeight: 600, color: 'var(--on)', fontVariantNumeric: 'tabular-nums' }}>{liveHR}</span>
                <span style={{ fontSize: 13, color: 'var(--muted)', fontFamily: 'var(--mono)' }}>bpm</span>
              </div>
            </div>
            <MIcon name="favorite" size={30} fill={1} color="var(--accent)" style={{ animation: 'cc-beat 1s ease-in-out infinite' }} />
          </Card>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 2 }}>
          <PillButton variant={connected ? 'outline' : 'filled'} full
            icon={connected ? 'bluetooth_disabled' : 'bluetooth_searching'} onClick={onToggle}>
            {connected ? 'Disconnect' : 'Scan for sensors'}
          </PillButton>
          <div style={{ textAlign: 'center', fontSize: 12.5, color: 'var(--faint)', padding: '0 16px', lineHeight: 1.5 }}>
            Auto HRV reconnects to your H10 automatically whenever it's in range.
          </div>
        </div>
      </div>
    </ScreenScroll>
  );
}

Object.assign(window, { ScreenScroll, ConnectScreen });
