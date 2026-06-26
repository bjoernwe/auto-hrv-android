// app.jsx — Auto HRV prototype shell: theme tokens, navigation, tweaks
const { useState, useEffect, useRef, useCallback } = React;
const {
  MIcon, TopBar,
  ConnectScreen, BreatheScreen,
} = window;

const ACCENTS = {
  Cyan: '#1FD3E0',
  Violet: '#9B8CFF',
  Mint: '#3FDDA0',
  Coral: '#FF7A66',
};

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "accent": "#1FD3E0",
  "intensity": "Calm",
  "typeface": "Modern",
  "reduceMotion": false
}/*EDITMODE-END*/;

function ConnectHeader({ onBack }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 14px 12px', flexShrink: 0 }}>
      <button onClick={onBack} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 6, display: 'grid', placeItems: 'center' }}>
        <MIcon name="arrow_back" size={24} color="var(--on)" />
      </button>
      <span style={{ fontSize: 18, fontWeight: 600, color: 'var(--on)' }}>Sensor</span>
    </div>
  );
}

function App() {
  const [t, setTweak] = window.useTweaks(TWEAK_DEFAULTS);
  const [showConnect, setShowConnect] = useState(false);
  const [connected, setConnected] = useState(true);
  const [liveHR, setLiveHR] = useState(61);
  const [fit, setFit] = useState(1);

  const calm = t.intensity === 'Calm';
  const uiFont = t.typeface === 'Material'
    ? "'Roboto', system-ui, sans-serif"
    : "'Hanken Grotesk', system-ui, sans-serif";

  // live HR wander
  useEffect(() => {
    if (!connected) return;
    const id = setInterval(() => {
      setLiveHR((h) => {
        const next = h + (Math.random() < 0.5 ? -1 : 1) * (Math.random() < 0.4 ? 1 : 0);
        return Math.max(56, Math.min(67, next));
      });
    }, 1050);
    return () => clearInterval(id);
  }, [connected]);

  // fit-to-viewport scaling
  useEffect(() => {
    const onResize = () => {
      const s = Math.min(1, (window.innerHeight - 40) / 892, (window.innerWidth - 24) / 412);
      setFit(s);
    };
    onResize();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  let screen;
  if (showConnect) screen = <ConnectScreen connected={connected} battery={87} liveHR={liveHR} onToggle={() => setConnected((c) => !c)} />;
  else screen = <BreatheScreen calm={calm} reduceMotion={t.reduceMotion} />;

  return (
    <div className="app-root" style={{
      '--accent': t.accent,
      '--ui': uiFont,
      fontFamily: uiFont,
      minHeight: '100vh', display: 'grid', placeItems: 'center',
      background: 'radial-gradient(1200px 800px at 50% -10%, #15171c 0%, var(--page) 60%)',
    }}>
      <div style={{ transform: `scale(${fit})`, transformOrigin: 'center center' }}>
        <window.AndroidDevice dark width={412} height={892}>
          <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: 'var(--bg)' }}>
            {showConnect
              ? <ConnectHeader onBack={() => setShowConnect(false)} />
              : <TopBar title="Auto HRV" connected={connected} battery={87} device={connected ? 'H10' : 'Offline'} onDeviceTap={() => setShowConnect(true)} />}
            {screen}
          </div>
        </window.AndroidDevice>
      </div>

      <window.TweaksPanel>
        <window.TweakSection label="Theme" />
        <window.TweakColor label="Accent" value={t.accent}
          options={Object.values(ACCENTS)}
          onChange={(v) => setTweak('accent', v)} />
        <window.TweakRadio label="Glow" value={t.intensity}
          options={['Calm', 'Vivid']}
          onChange={(v) => setTweak('intensity', v)} />
        <window.TweakSection label="Type & motion" />
        <window.TweakRadio label="Typeface" value={t.typeface}
          options={['Modern', 'Material']}
          onChange={(v) => setTweak('typeface', v)} />
        <window.TweakToggle label="Reduce motion" value={t.reduceMotion}
          onChange={(v) => setTweak('reduceMotion', v)} />
      </window.TweaksPanel>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
