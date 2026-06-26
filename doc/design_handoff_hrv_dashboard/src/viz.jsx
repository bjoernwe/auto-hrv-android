// viz.jsx — RR tachogram + autocorrelation chart + breathing pacer + engine
// Exports to window: RRTachogram, ACFChart, BreathingPacer, useBreathEngine
const { useState: useVizState, useRef: useVizRef, useEffect: useVizEffect } = React;

// ── helpers ────────────────────────────────────────────────
function pathFrom(points) {
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p[0].toFixed(1)},${p[1].toFixed(1)}`).join(' ');
}
function breathEase(p) { return 0.5 - 0.5 * Math.cos(Math.PI * p); }

// ── RR / breathing engine ──────────────────────────────────
// Physiology, lightly faked: respiratory sinus arrhythmia (RSA). As you inhale
// (scale→1) the heart speeds up so the R–R interval SHORTENS; as you exhale
// (scale→0) it slows and the interval LENGTHENS. One clock drives the pacer orb
// and the beat stream, so the tachogram wave is phase-locked to the breath.
const RR_BASE = 945;  // ms  (~63 bpm resting)
const RR_AMP = 158;   // ms  half-swing of the wave at full breath depth

// useBreathEngine: returns { scale, phase } for the pacer (read every frame),
// a rolling beats[] buffer + `now` for the tachogram, and throttled `stats`.
function useBreathEngine({ targetRef, ratio = 1.5, reduceMotion = false, windowSec = 26 }) {
  const clock = useVizRef({ pos: 0, eff: targetRef.current, scale: 0, phase: 'Inhale' });
  const beats = useVizRef([]);
  const nextBeat = useVizRef(0);
  const elapsed = useVizRef(0);
  const depthRef = useVizRef(0.92);
  const [, tick] = useVizState(0);
  const [stats, setStats] = useVizState({ hr: 60, hrv: 0, amp: 0, rr: RR_BASE });

  // seed a full window of history once, so the plot is never empty on mount
  if (beats.current.length === 0) {
    const cyc = targetRef.current;
    let tt = -windowSec;
    while (tt < 0) {
      const ph = (((tt % cyc) + cyc) % cyc) / cyc;
      const sc = 0.5 - 0.5 * Math.cos(2 * Math.PI * ph);
      const rr = RR_BASE + RR_AMP * 0.9 * (0.5 - sc) * 2;
      beats.current.push({ t: tt, rr });
      tt += rr / 1000;
    }
    const lastSeed = beats.current[beats.current.length - 1];
    nextBeat.current = lastSeed.t + lastSeed.rr / 1000;
  }

  // animation clock + beat emitter
  useVizEffect(() => {
    if (reduceMotion) {
      clock.current = { pos: 0, eff: targetRef.current, scale: 0.55, phase: 'Breathe' };
      tick((x) => x + 1);
      return;
    }
    let raf, last = performance.now();
    const loop = (now) => {
      const dt = Math.min(0.05, (now - last) / 1000); last = now;
      const c = clock.current;
      // ease the breathing cycle toward the live resonance target
      c.eff += (targetRef.current - c.eff) * Math.min(1, dt * 0.7);
      const cyc = c.eff;
      const inhale = cyc / (1 + ratio), exhale = cyc - inhale;
      c.pos += dt; if (c.pos >= cyc) c.pos -= cyc;
      const t = c.pos;
      if (t < inhale) { c.phase = 'Inhale'; c.scale = breathEase(t / inhale); }
      else { c.phase = 'Exhale'; c.scale = 1 - breathEase((t - inhale) / exhale); }
      // breath depth drifts slowly → wave amplitude gently grows/shrinks
      depthRef.current = 0.86 + 0.14 * (0.5 + 0.5 * Math.sin(now / 7000));
      elapsed.current += dt;
      // emit beats up to the current time, spacing each by its own R–R interval
      let guard = 0;
      while (elapsed.current >= nextBeat.current && guard++ < 8) {
        const noise = (Math.random() - 0.5) * 13;
        const rr = RR_BASE + RR_AMP * depthRef.current * (0.5 - c.scale) * 2 + noise;
        beats.current.push({ t: nextBeat.current, rr });
        nextBeat.current += rr / 1000;
      }
      // trim to the visible window (+ a little slack)
      const cutoff = elapsed.current - windowSec - 2;
      while (beats.current.length && beats.current[0].t < cutoff) beats.current.shift();
      tick((x) => x + 1);
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(raf);
  }, [reduceMotion, ratio]);

  // throttle the readout numbers so they don't flicker every frame
  useVizEffect(() => {
    const compute = () => {
      const recent = beats.current.filter((b) => b.t >= elapsed.current - 12);
      if (recent.length < 2) return;
      const rrs = recent.map((b) => b.rr);
      const mean = rrs.reduce((a, b) => a + b, 0) / rrs.length;
      let sq = 0;
      for (let i = 1; i < rrs.length; i++) sq += (rrs[i] - rrs[i - 1]) ** 2;
      const rmssd = Math.sqrt(sq / (rrs.length - 1));
      setStats({
        hr: Math.round(60000 / mean),
        hrv: Math.round(rmssd),
        amp: Math.round(Math.max(...rrs) - Math.min(...rrs)),
        rr: Math.round(beats.current[beats.current.length - 1].rr),
      });
    };
    compute();
    const id = setInterval(compute, 1000);
    return () => clearInterval(id);
  }, []);

  return {
    scale: clock.current.scale,
    phase: clock.current.phase,
    beats: beats.current,
    now: elapsed.current,
    windowSec,
    stats,
  };
}

// ── RR tachogram — the live beat-to-beat interval wave ─────
// This is the payoff signal: a scrolling plot of R–R intervals that visibly
// rises and falls with each breath. Bigger, cleaner waves = deeper resonance.
function RRTachogram({ beats, now, windowSec, height = 134 }) {
  const W = 340, H = height, padL = 4, padR = 4, padT = 16, padB = 16;
  const rrMin = RR_BASE - RR_AMP - 46, rrMax = RR_BASE + RR_AMP + 46;
  const xs = (t) => padL + ((t - (now - windowSec)) / windowSec) * (W - padL - padR);
  const ys = (rr) => padT + (1 - (rr - rrMin) / (rrMax - rrMin)) * (H - padT - padB);
  const vis = beats.filter((b) => b.t >= now - windowSec - 1);
  const pts = vis.map((b) => [xs(b.t), ys(b.rr)]);
  const line = pathFrom(pts);
  const last = pts[pts.length - 1];
  const baseY = ys(RR_BASE);
  const area = pts.length
    ? `${line} L${pts[pts.length - 1][0].toFixed(1)},${(H - padB).toFixed(1)} L${pts[0][0].toFixed(1)},${(H - padB).toFixed(1)} Z`
    : '';
  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} style={{ display: 'block', overflow: 'visible' }}>
      <defs>
        <linearGradient id="rrFill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.30" />
          <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
        </linearGradient>
        <linearGradient id="rrStroke" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="var(--accent)" stopOpacity="0" />
          <stop offset="16%" stopColor="var(--accent)" stopOpacity="0.9" />
          <stop offset="100%" stopColor="var(--accent)" stopOpacity="1" />
        </linearGradient>
      </defs>
      {/* resting-rate baseline */}
      <line x1={padL} y1={baseY} x2={W - padR} y2={baseY} stroke="var(--outline)" strokeWidth="1" strokeDasharray="2 6" />
      {area && <path d={area} fill="url(#rrFill)" />}
      <path d={line} fill="none" stroke="url(#rrStroke)" strokeWidth="2.4" strokeLinejoin="round" strokeLinecap="round" />
      {/* beats, fading in from the left */}
      {pts.map((p, i) => (
        <circle key={i} cx={p[0]} cy={p[1]} r="1.7" fill="var(--accent)"
          opacity={0.18 + 0.55 * (i / Math.max(1, pts.length - 1))} />
      ))}
      {/* leading "now" beat */}
      {last && (
        <g>
          <circle cx={last[0]} cy={last[1]} r="10" fill="var(--accent)" opacity="0.16" />
          <circle cx={last[0]} cy={last[1]} r="4.2" fill="var(--accent)" stroke="var(--surface)" strokeWidth="2" />
        </g>
      )}
    </svg>
  );
}

// ── autocorrelation chart with peak marker (drives the pacer) ──
function ACFChart({ height = 130, peakLag = 10.9, maxLag = 26, bandMin = null, bandMax = null }) {
  const W = 372, H = height, padL = 8, padB = 22, padT = 8;
  const xs = (t) => padL + (t / maxLag) * (W - padL * 2);
  const ys = (v) => padT + (1 - (v + 1) / 2) * (H - padB - padT);
  // round the peak so we only recompute the curve when it meaningfully moves
  const key = Math.round(peakLag * 10) / 10;
  const { line, peakX, peakY } = React.useMemo(() => {
    const pts = [];
    for (let t = 0; t <= maxLag; t += 0.3) {
      pts.push([xs(t), ys(Math.exp(-t / 13) * Math.cos((2 * Math.PI * t) / key))]);
    }
    return { line: pathFrom(pts), peakX: xs(key), peakY: ys(Math.exp(-key / 13) * Math.cos(2 * Math.PI)) };
  }, [key, height, maxLag]);

  const hasBand = bandMin !== null && bandMax !== null;
  const plotL = padL, plotR = W - padL, plotT = padT, plotB = H - padB;
  const bx0 = hasBand ? xs(bandMin) : null;
  const bx1 = hasBand ? xs(bandMax) : null;

  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} style={{ display: 'block' }}>
      {/* zero line */}
      <line x1={padL} y1={ys(0)} x2={W - padL} y2={ys(0)} stroke="var(--outline)" strokeWidth="1" />
      {/* out-of-band shading — rendered before the curve so it sits behind */}
      {hasBand && bx0 > plotL && (
        <rect x={plotL} y={plotT} width={bx0 - plotL} height={plotB - plotT}
          fill="color-mix(in srgb, var(--bg) 52%, transparent)" />
      )}
      {hasBand && bx1 < plotR && (
        <rect x={bx1} y={plotT} width={plotR - bx1} height={plotB - plotT}
          fill="color-mix(in srgb, var(--bg) 52%, transparent)" />
      )}
      {/* band edge tick marks */}
      {hasBand && (
        <>
          <line x1={bx0} y1={plotT} x2={bx0} y2={plotB}
            stroke="var(--accent)" strokeWidth="1" strokeOpacity="0.35" strokeDasharray="2 4" />
          <line x1={bx1} y1={plotT} x2={bx1} y2={plotB}
            stroke="var(--accent)" strokeWidth="1" strokeOpacity="0.35" strokeDasharray="2 4" />
        </>
      )}
      {/* peak marker */}
      <line x1={peakX} y1={padT} x2={peakX} y2={H - padB} stroke="color-mix(in srgb, var(--accent) 45%, transparent)" strokeWidth="1.5" strokeDasharray="3 4" style={{ transition: 'all .6s ease' }} />
      <path d={line} fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round" style={{ transition: 'all .6s ease' }} />
      <circle cx={peakX} cy={peakY} r="4.5" fill="var(--accent)" stroke="var(--surface)" strokeWidth="2" style={{ transition: 'all .6s ease' }} />
      <text x={peakX} y={padT + 11} fontFamily="var(--mono)" fontSize="11" fontWeight="600" fill="var(--accent)" textAnchor="middle" style={{ transition: 'all .6s ease' }}>{key.toFixed(1)}s</text>
      <text x={padL} y={H - 6} fontFamily="var(--mono)" fontSize="10" fill="var(--faint)">0s</text>
      <text x={W - padL} y={H - 6} fontFamily="var(--mono)" fontSize="10" fill="var(--faint)" textAnchor="end">{maxLag}s lag</text>
    </svg>
  );
}

// ── dual-thumb band slider ─────────────────────────────────
// Track edges are inset by the same fraction as ACFChart's padL/W so the
// handles align pixel-perfectly with the band-edge lines in the chart above.
const _ACF_W = 372, _ACF_PAD = 8;

function BandSlider({ minVal = 0, maxVal = 26, step = 0.5, value, onChange }) {
  const [lo, hi] = value;
  const trackRef = useVizRef(null);
  const padPct = (_ACF_PAD / _ACF_W) * 100; // ≈ 2.15 %

  const toPercent = (v) => ((v - minVal) / (maxVal - minVal)) * 100;

  const fromEvent = (e) => {
    const rect = trackRef.current.getBoundingClientRect();
    const frac = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    const raw = minVal + frac * (maxVal - minVal);
    return Math.round(raw / step) * step;
  };

  // per-handle pointer-capture drag — no shared drag-state needed
  const makeHandlers = (which) => ({
    onPointerDown(e) {
      e.currentTarget.setPointerCapture(e.pointerId);
      e.preventDefault();
    },
    onPointerMove(e) {
      if (!e.currentTarget.hasPointerCapture(e.pointerId)) return;
      const v = fromEvent(e);
      if (which === 'lo') onChange([Math.max(minVal, Math.min(v, hi - step)), hi]);
      else onChange([lo, Math.max(lo + step, Math.min(v, maxVal))]);
    },
  });

  const loP = toPercent(lo);
  const hiP = toPercent(hi);

  // keep labels from colliding when handles are very close
  const labelsClose = hiP - loP < 14;

  return (
    <div style={{ paddingLeft: `${padPct}%`, paddingRight: `${padPct}%`, userSelect: 'none' }}>
      {/* value labels */}
      <div style={{ position: 'relative', height: 15, marginBottom: 2 }}>
        <span style={{
          position: 'absolute', left: `${loP}%`,
          transform: `translateX(-50%) ${labelsClose ? 'translateY(-10px)' : ''}`,
          fontSize: 10, fontFamily: 'var(--mono)', color: 'var(--accent)', whiteSpace: 'nowrap',
          transition: 'left .15s ease',
        }}>{lo.toFixed(1)}s</span>
        <span style={{
          position: 'absolute', left: `${hiP}%`, transform: 'translateX(-50%)',
          fontSize: 10, fontFamily: 'var(--mono)', color: 'var(--accent)', whiteSpace: 'nowrap',
          transition: 'left .15s ease',
        }}>{hi.toFixed(1)}s</span>
      </div>
      {/* track */}
      <div ref={trackRef} style={{ position: 'relative', height: 32, display: 'flex', alignItems: 'center' }}>
        {/* rail */}
        <div style={{
          position: 'absolute', left: 0, right: 0, height: 3,
          borderRadius: 2, background: 'var(--outline)',
        }} />
        {/* active band fill */}
        <div style={{
          position: 'absolute', left: `${loP}%`, width: `${hiP - loP}%`,
          height: 3, borderRadius: 2,
          background: 'color-mix(in srgb, var(--accent) 38%, transparent)',
          transition: 'left .05s, width .05s',
        }} />
        {/* lo handle — 40 px touch target, 18 px visual */}
        <div {...makeHandlers('lo')} style={{
          position: 'absolute', left: `${loP}%`, top: '50%',
          transform: 'translateX(-50%) translateY(-50%)',
          width: 40, height: 40, borderRadius: '50%',
          display: 'grid', placeItems: 'center',
          cursor: 'ew-resize', touchAction: 'none',
        }}>
          <div style={{
            width: 18, height: 18, borderRadius: '50%',
            background: 'var(--surface)', border: '2.5px solid var(--accent)',
            pointerEvents: 'none',
          }} />
        </div>
        {/* hi handle */}
        <div {...makeHandlers('hi')} style={{
          position: 'absolute', left: `${hiP}%`, top: '50%',
          transform: 'translateX(-50%) translateY(-50%)',
          width: 40, height: 40, borderRadius: '50%',
          display: 'grid', placeItems: 'center',
          cursor: 'ew-resize', touchAction: 'none',
        }}>
          <div style={{
            width: 18, height: 18, borderRadius: '50%',
            background: 'var(--surface)', border: '2.5px solid var(--accent)',
            pointerEvents: 'none',
          }} />
        </div>
      </div>
    </div>
  );
}

// ── breathing pacer (animated) ─────────────────────────────
// scale: 0 (fully exhaled) → 1 (fully inhaled)
function BreathingPacer({ scale = 0, phase = 'Inhale', size = 256, calm = true }) {
  const minR = 0.34, maxR = 1;
  const r = (minR + (maxR - minR) * scale);
  const glow = calm ? 0.5 : 0.8;
  return (
    <div style={{ position: 'relative', width: size, height: size, display: 'grid', placeItems: 'center' }}>
      {/* outer guide ring */}
      <div style={{
        position: 'absolute', width: size, height: size, borderRadius: '50%',
        border: '1px solid color-mix(in srgb, var(--accent) 22%, transparent)',
      }} />
      {/* glow */}
      <div style={{
        position: 'absolute', width: size * r, height: size * r, borderRadius: '50%',
        background: `radial-gradient(circle, color-mix(in srgb, var(--accent) ${Math.round(glow*60)}%, transparent) 0%, transparent 70%)`,
        filter: 'blur(8px)',
      }} />
      {/* filled orb */}
      <div style={{
        position: 'absolute', width: size * r, height: size * r, borderRadius: '50%',
        background: 'radial-gradient(circle at 50% 38%, color-mix(in srgb, var(--accent) 85%, white) 0%, var(--accent) 45%, color-mix(in srgb, var(--accent) 60%, black) 100%)',
        boxShadow: `0 0 ${Math.round(40*glow)}px color-mix(in srgb, var(--accent) 55%, transparent)`,
      }} />
      {/* center label */}
      <div style={{ position: 'relative', textAlign: 'center', color: '#04181b' }}>
        <div style={{ fontSize: 19, fontWeight: 700, letterSpacing: '0.04em' }}>{phase}</div>
      </div>
    </div>
  );
}

Object.assign(window, {
  RRTachogram, ACFChart, BandSlider, BreathingPacer, useBreathEngine,
});
