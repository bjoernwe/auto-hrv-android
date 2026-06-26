// screens2.jsx — single live calibration screen: pace + watch your heart breathe
// Priority order top→bottom: pacer · RR plot · autocorrelation · metrics
const { MIcon, Card, Divider, SectionLabel } = window;
const { BreathingPacer, RRTachogram, ACFChart, BandSlider, useBreathEngine } = window;
const { useState, useEffect, useRef } = React;

// ── BREATHE — the whole app: pace + live feedback, always on ──
function BreatheScreen({ calm, reduceMotion }) {
  const ratio = 1.5;
  const targetRef = useRef(10.9);
  const [detected, setDetected] = useState(10.9);
  const [syncing, setSyncing] = useState(false);
  const syncTimer = useRef(null);
  const [band, setBand] = useState([8.5, 14]);
  const bandRef = useRef([8.5, 14]);
  const handleBandChange = (newBand) => {
    bandRef.current = newBand;
    setBand(newBand);
    // snap detected value immediately if it falls outside the new window
    const [bMin, bMax] = newBand;
    const cur = targetRef.current;
    if (cur < bMin || cur > bMax) {
      const clamped = Math.max(bMin, Math.min(bMax, cur));
      targetRef.current = clamped;
      setDetected(clamped);
    }
  };

  // one engine: drives the pacer orb AND the beat stream behind the RR plot
  const eng = useBreathEngine({ targetRef, ratio, reduceMotion, windowSec: 26 });

  // live bio-data: the autocorrelation peak drifts → drives the resonance target
  useEffect(() => {
    const id = setInterval(() => {
      const prev = targetRef.current;
      const [bMin, bMax] = bandRef.current;
      const raw = 10.8 + Math.sin(Date.now() / 9000) * 0.55 + (Math.random() - 0.5) * 0.3;
      const aim = Math.max(bMin, Math.min(bMax, raw));
      const next = +(prev + (aim - prev) * 0.45).toFixed(2);
      targetRef.current = next;
      setDetected(next);
      if (Math.abs(next - prev) > 0.07) {
        setSyncing(true);
        clearTimeout(syncTimer.current);
        syncTimer.current = setTimeout(() => setSyncing(false), 2600);
      }
    }, 1500);
    return () => clearInterval(id);
  }, []);

  const breaths = (60 / detected).toFixed(1);

  return (
    <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: '4px 18px 18px', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, margin: 'auto 0' }}>

        {/* ① PACER — the hero */}
        <BreathingPacer scale={eng.scale} phase={eng.phase} size={188} calm={calm} />

        {/* resonance readout — the pacer's live target */}
        <div style={{
          display: 'flex', alignItems: 'center',
          background: 'var(--surface)', border: '1px solid var(--outline)',
          borderRadius: 999, padding: '7px 8px 7px 16px',
        }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
            <span style={{ fontSize: 21, fontWeight: 600, color: 'var(--on)', fontVariantNumeric: 'tabular-nums' }}>{detected.toFixed(1)}</span>
            <span style={{ fontSize: 12, color: 'var(--muted)', fontFamily: 'var(--mono)' }}>s</span>
          </div>
          <span style={{ width: 1, height: 18, background: 'var(--outline)', margin: '0 13px' }} />
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
            <span style={{ fontSize: 21, fontWeight: 600, color: 'var(--on)', fontVariantNumeric: 'tabular-nums' }}>{breaths}</span>
            <span style={{ fontSize: 12, color: 'var(--muted)', fontFamily: 'var(--mono)' }}>/min</span>
          </div>
          <span style={{
            marginLeft: 11, display: 'inline-flex', alignItems: 'center', gap: 6,
            padding: '6px 11px', borderRadius: 999,
            background: syncing ? 'color-mix(in srgb, var(--accent) 16%, transparent)' : 'color-mix(in srgb, var(--accent) 10%, transparent)',
          }}>
            <span style={{
              width: 7, height: 7, borderRadius: 999, background: 'var(--accent)',
              animation: syncing ? 'cc-pulse 1.2s ease-in-out infinite' : 'none',
            }} />
            <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--accent)', whiteSpace: 'nowrap' }}>
              {syncing ? 'Re-tuning' : 'In resonance'}
            </span>
          </span>
        </div>

        {/* ② RR PLOT — watch your heart breathe with the pacer */}
        <Card pad={16} style={{ width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
            <div>
              <SectionLabel style={{ whiteSpace: 'nowrap' }}>R–R interval</SectionLabel>
              <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 3, whiteSpace: 'nowrap' }}>beat-to-beat, ms</div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, justifyContent: 'flex-end' }}>
                <span style={{ fontSize: 24, fontWeight: 600, color: 'var(--accent)', fontVariantNumeric: 'tabular-nums' }}>{eng.stats.amp}</span>
                <span style={{ fontSize: 12, color: 'var(--muted)', fontFamily: 'var(--mono)' }}>ms</span>
              </div>
              <div style={{ fontSize: 11, color: 'var(--faint)', fontFamily: 'var(--mono)', letterSpacing: '0.04em', marginTop: 1 }}>SWING</div>
            </div>
          </div>
          <div style={{ marginTop: 10 }}>
            <RRTachogram beats={eng.beats} now={eng.now} windowSec={eng.windowSec} height={134} />
          </div>
        </Card>

        {/* ③ AUTOCORRELATION — the signal the pacer is tuned to */}
        <Card pad={16} style={{ width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <SectionLabel>Autocorrelation</SectionLabel>
            <span style={{ fontSize: 11.5, color: 'var(--muted)' }}>peak → pace</span>
          </div>
          <div style={{ marginTop: 6 }}>
            <ACFChart height={92} peakLag={detected} bandMin={band[0]} bandMax={band[1]} />
          </div>
          <div style={{ marginTop: 10 }}>
            <BandSlider minVal={0} maxVal={26} step={0.5} value={band} onChange={handleBandChange} />
          </div>
        </Card>

        {/* ④ METRICS — supporting, least prominent */}
        <div style={{
          width: '100%', display: 'flex', alignItems: 'stretch',
          background: 'var(--surface)', border: '1px solid var(--outline)',
          borderRadius: 'var(--r-tile)', padding: '11px 4px',
        }}>
          {[
            { l: 'Heart rate', v: eng.stats.hr, u: 'bpm', beat: true },
            { l: 'HRV', v: eng.stats.hrv, u: 'ms' },
            { l: 'Interval', v: eng.stats.rr, u: 'ms' },
          ].map((m, i) => (
            <div key={m.l} style={{
              flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3,
              borderLeft: i ? '1px solid var(--outline)' : 'none',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                <span style={{ fontSize: 10.5, fontFamily: 'var(--mono)', letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--faint)' }}>{m.l}</span>
                {m.beat && <MIcon name="favorite" size={11} fill={1} color="var(--accent)" style={{ animation: 'cc-beat 1s ease-in-out infinite' }} />}
              </div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 3 }}>
                <span style={{ fontSize: 19, fontWeight: 600, color: 'var(--on)', fontVariantNumeric: 'tabular-nums' }}>{m.v}</span>
                <span style={{ fontSize: 10.5, color: 'var(--muted)', fontFamily: 'var(--mono)' }}>{m.u}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { BreatheScreen });
