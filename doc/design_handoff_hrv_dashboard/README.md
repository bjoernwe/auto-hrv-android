# Handoff: Auto HRV — Main Dashboard ("Breathe" screen)

## Overview
This is the primary screen of **Auto HRV**, an Android app that pairs with a chest-strap
heart-rate sensor (e.g. Polar H10) and guides the user through **resonance-frequency
breathing**. The app detects the breathing rate at which the user's heart-rate variability
(HRV) is maximized, paces their breath with an animated orb, and shows the live
beat-to-beat (R–R interval) wave responding to each breath.

This document specifies the **main dashboard only** (the "Breathe" screen — the default
view of the app).

## About the design files
The files in this bundle are **design references created in HTML/React** — a prototype that
demonstrates the intended look, motion, and behavior. **They are not production code to port
directly.** The task is to **recreate this screen natively in the Android app** using the
project's existing stack and patterns.

- `Auto HRV - Dark Material.html` + `src/*.jsx` — the live prototype. Open the HTML file in a
  browser to see the animations and interactions.
- `dashboard.png` — screenshot of the screen.

## Target stack
**Jetpack Compose + Material 3.** The design is "Material-informed" but uses a custom dark
theme and custom data-viz, not stock M3 components. Map the tokens below into a custom
`ColorScheme` / `MaterialTheme`, and build the charts as custom `Canvas` composables.

## Fidelity
**High-fidelity.** Colors, typography, spacing, radii, and motion are final. Recreate
pixel-accurately, but using Compose primitives and idioms (don't try to reproduce CSS
`color-mix`/SVG literally — translate to `Color.copy(alpha=…)`, `Brush.radialGradient`, etc.).

---

## Design tokens

### Colors
The whole screen is dark. The accent is the only chromatic color; everything else is a
near-neutral cool grey.

| Token | Hex | Compose | Usage |
|---|---|---|---|
| page | `#060708` | window/scaffold background | Behind the device; near-black |
| bg | `#0A0B0E` | `background` | Screen surface (column background) |
| surface | `#131519` | `surface` | Cards |
| surface-2 | `#1A1D23` | `surfaceVariant` | Chips, status pill, sensor icon bg |
| surface-3 | `#21252C` | — | Pressed/elevated state |
| outline | `rgba(255,255,255,0.07)` | `outlineVariant` | Card & divider borders (1px) |
| outline-strong | `rgba(255,255,255,0.13)` | `outline` | Outline-button borders |
| on | `#ECEFF3` | `onSurface` | Primary text / numbers |
| muted | `#9298A2` | `onSurfaceVariant` | Secondary text, units |
| faint | `#5C626C` | — | Tertiary text, axis labels, mono labels |
| **accent** | `#1FD3E0` (cyan) | `primary` | Pacer orb, chart lines, active values, dot |
| on-accent | `#06181B` | `onPrimary` | Text on filled accent surfaces |

> The prototype lets the user theme the accent (Cyan `#1FD3E0`, Violet `#9B8CFF`,
> Mint `#3FDDA0`, Coral `#FF7A66`). Cyan is the default and the only one you must ship;
> treat accent as a single theme color (`primary`).

**Accent tints used throughout** (derive with `accent.copy(alpha=…)` over the surface):
- 16% accent over surface-2 → connected sensor icon bg
- 14–16% accent → "Connected" / "In resonance" chip background
- 35–40% accent → ring borders, glow edges

### Typography
Three families. Load via downloadable fonts or bundle:
- **Hanken Grotesk** (`--ui`) — all UI text, headings, numbers. Weights 400/500/600/700.
- **JetBrains Mono** (`--mono`) — units (bpm, ms, s), section labels, axis ticks, device ID.
  Weights 400/500/600.
- *(Roboto is only used by the prototype's "Material" typeface toggle — optional.)*

| Role | Family | Size (dp/sp) | Weight | Notes |
|---|---|---|---|---|
| App title ("Auto HRV") | Hanken | 18 | 600 | letter-spacing −0.01em |
| Hero numbers (resonance, swing) | Hanken | 21–24 | 600 | tabular-nums |
| Pacer phase label ("Inhale"/"Exhale") | Hanken | 19 | 700 | color `#04181B` (on orb) |
| Metric value | Hanken | 19 | 600 | tabular-nums |
| Section label (e.g. "R–R INTERVAL", "AUTOCORRELATION") | JetBrains Mono | 11 | 600 | UPPERCASE, letter-spacing 0.14em, color faint |
| Units / small mono (bpm, ms, s, "/min") | JetBrains Mono | 10.5–13 | 400 | color muted |
| Body / helper text | Hanken | 12–13 | 400 | color muted |
| Axis ticks ("0s", "26s lag") | JetBrains Mono | 10 | 400 | color faint |

Always use **tabular / monospaced figures** for any live-updating number so it doesn't jitter.

### Spacing, radius, layout
- Screen content padding: **18dp** horizontal.
- Vertical gap between major blocks: **12dp**.
- Card corner radius (`--r-card`): **24dp**. Card padding: **16dp** (18dp default elsewhere).
- Tile/row corner radius (`--r-tile`): **18dp**.
- Pill / chip / fully-rounded: **999dp** (full).
- Card border: **1dp** outline. Card shadow: soft, large — `0 10px 30px rgba(0,0,0,0.35)`
  plus a 1px inset top highlight `rgba(255,255,255,0.03)`. In Compose, approximate with a
  low-elevation shadow + the 1dp border.
- Device frame in prototype is **412 × 892** (a generic large Android phone). On device,
  the screen is full-width; the 412dp width is just the mock canvas.

---

## Layout (top → bottom)

A single vertical, scrollable column. Content is **vertically centered** when it fits
(`margin: auto 0` in the prototype → center the column in available space, allow scroll if
it overflows). Order is intentional — pacer is the hero, metrics are least prominent.

```
┌─────────────────────────────────────────┐
│ TopBar:  [♥ icon] Auto HRV    [● H10 87%]│  ← app bar, 10/18/12 padding
├─────────────────────────────────────────┤
│                                          │
│            ◯  Breathing Pacer            │  ① hero, 188dp orb
│              "Exhale"                     │
│                                          │
│   [ 10.8 s | 5.6 /min  ● In resonance ]  │  ← resonance readout pill
│                                          │
│   ┌─ Card ──────────────────────────┐    │  ② R–R interval plot
│   │ R–R INTERVAL          284 ms     │    │
│   │ beat-to-beat, ms      SWING      │    │
│   │   ∿∿∿ scrolling wave ∿∿∿  ●      │    │
│   └─────────────────────────────────┘    │
│                                          │
│   ┌─ Card ──────────────────────────┐    │  ③ autocorrelation + band slider
│   │ AUTOCORRELATION      peak → pace │    │
│   │   ╲╱╲ curve w/ peak marker ╱     │    │
│   │   ○────────●━━━━●────────○ slider │    │
│   └─────────────────────────────────┘    │
│                                          │
│   ┌─ row ───────────────────────────┐    │  ④ metrics (3-up)
│   │ HEART RATE♥ │  HRV   │ INTERVAL  │    │
│   │   65 bpm    │ 62 ms  │  828 ms   │    │
│   └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### TopBar
- Row, space-between, padding `10dp / 18dp / 12dp`.
- **Left:** 30dp rounded-square (radius 9dp) with 18% accent background containing a filled
  heart icon (accent, 17dp) + "Auto HRV" title (18sp/600).
- **Right:** **Status chip** — a tappable pill (`surface-2`, 1dp outline, radius 999):
  - 8dp status dot — accent when connected (with a soft pulsing `0 0 0 3dp` accent glow ring,
    `cc-pulse` 2.4s ease-in-out), faint grey when disconnected.
  - device name ("H10", 13sp/500, on-surface) + battery % ("87%", 12sp mono, muted).
  - Tapping it opens the Sensor screen (out of scope here).

### ① Breathing pacer (hero)
An animated orb that the user breathes with. Build as a `Canvas`/`Box` composable driven by
a single `scale` value in **0..1** (0 = fully exhaled, 1 = fully inhaled).
- **Outer guide ring:** fixed circle, diameter = full size (188dp), 1dp border at 22% accent.
- **Orb:** circle whose diameter = `size * (0.34 + 0.66 * scale)` — i.e. it shrinks to 34%
  on full exhale and grows to 100% on full inhale.
  - Fill: radial gradient, light center → accent → darker edge
    (`radialGradient` from `lerp(accent, white, .15)` at center → `accent` at 45% → 
    `lerp(accent, black, .4)` at edge, center offset ~50%/38%).
  - Glow: a blurred accent radial behind it + a soft `boxShadow` (~40dp blur, 55% accent).
    "Calm" intensity = softer glow (0.5×), "Vivid" = stronger (0.8×). Ship Calm.
- **Center label:** the phase word ("Inhale" / "Exhale"), 19sp/700, color `#04181B`
  (dark, since it sits on the bright orb).

**Motion / the breathing clock.** One animation clock drives both the orb and the heartbeat
stream (they must stay phase-locked). Cycle length = the detected resonance period in seconds
(~10.8s). Inhale:exhale ratio = **1 : 1.5** (inhale is shorter). Within each phase, ease the
scale with a raised-cosine: `scale = 0.5 - 0.5*cos(π * progressThroughPhase)` on inhale, and
the mirror on exhale. Drive this with a Compose `withInfiniteAnimationFrameNanos` loop or a
physics/`Animatable`, computing scale each frame from elapsed time mod cycle.

### Resonance readout pill
A small centered pill (`surface`, 1dp outline, radius 999, padding `7/8/7/16`):
- detected resonance period **"10.8 s"** (21sp/600 + mono "s")
- 1dp vertical divider
- breaths/min **"5.6 /min"** (= 60 / period)
- a status chip on the right: accent dot + **"In resonance"** (12sp/600 accent). When the
  detected value is actively changing it flips to **"Re-tuning"** and the dot pulses
  (`cc-pulse` 1.2s).

### ② R–R interval plot (Card)
- Header row: left = "R–R INTERVAL" section label + "beat-to-beat, ms" sub (muted, 12sp).
  Right = big **swing** number (24sp/600 **accent**) + "ms" + "SWING" mono caption.
  *Swing* = (max − min) R–R over the recent window.
- **Chart** (custom `Canvas`, ~134dp tall, full card width):
  - A **scrolling line** of beat-to-beat R–R intervals (ms) over a ~26s window, newest on the
    right. The wave visibly rises and falls with the breath (respiratory sinus arrhythmia):
    inhale → HR speeds up → R–R shortens; exhale → R–R lengthens.
  - Line: 2.4dp, accent, with a left-to-right opacity gradient (fades in on the left).
  - Area fill below the line: vertical accent gradient, 30% → 0% alpha.
  - A dashed horizontal **baseline** at the resting rate (~945ms), 1dp outline, dash `2 6`.
  - Small accent dots at each beat (r≈1.7dp), fading in toward the left.
  - **Leading "now" dot** at the right end: 4.2dp accent dot with a 2dp surface-colored ring
    and a soft 10dp halo.
  - Y range maps roughly 740–1150ms across the height; X maps the time window across the width.

### ③ Autocorrelation chart + band slider (Card)
- Header: "AUTOCORRELATION" section label, right caption "peak → pace" (muted).
- **Chart** (custom `Canvas`, ~92dp tall):
  - A damped-cosine autocorrelation curve over lag 0→26s: `exp(-t/13) * cos(2π t / peakLag)`.
    `peakLag` is the detected resonance (~10.8s).
  - Zero line (1dp outline) across the middle.
  - **Peak marker:** a dashed vertical accent line at the peak lag + an accent dot (4.5dp,
    2dp surface ring) on the curve, with the lag value labeled above it ("10.8s", mono 11sp
    accent). Animate position changes smoothly (~0.6s ease).
  - **Search band:** the area *outside* the user's [lo, hi] band is shaded darker
    (52% bg overlay); band edges drawn as dashed accent lines (35% opacity).
  - Axis labels bottom-left "0s", bottom-right "26s lag" (mono 10sp faint).
- **Dual-thumb band slider** below the chart, horizontally aligned so its thumbs line up with
  the band-edge lines in the chart above (track is inset by the same ~2.15% the chart pads by):
  - 3dp rail (outline); active segment between thumbs filled at 38% accent.
  - Two thumbs: 18dp visible circle (surface fill, 2.5dp accent border) inside a 40dp
    touch target. Value labels above each thumb (mono 10sp accent, e.g. "8.5s", "14.0s").
    Range 0–26s, step 0.5. lo can't cross hi.
  - Changing the band re-clamps the detected peak into the new window.
  - In Compose, build as a custom dual-thumb slider (M3 `RangeSlider` restyled, or custom
    `pointerInput` drag) — note the **44dp minimum hit target** requirement.

### ④ Metrics row
A single `surface` row (radius 18dp, 1dp outline, padding `11/4`), 3 equal columns separated
by 1dp vertical dividers:
| Heart rate | HRV | Interval |
|---|---|---|
| 65 bpm | 62 ms | 828 ms |
- Each: mono uppercase label (10.5sp faint, letter-spacing 0.1em) over value (19sp/600
  on-surface, tabular) + unit (mono 10.5sp muted).
- "Heart rate" label has a tiny filled heart (11dp accent) that beats (`cc-beat`, see motion).

---

## Live data & state
The prototype fakes the biosignal; in the real app these come from the sensor's R–R stream.
State needed for this screen:

| State | Type | Meaning / source |
|---|---|---|
| `connected` | Bool | Sensor link status → drives TopBar chip |
| `battery` | Int % | From sensor |
| `beats` | rolling list of `{t, rr}` | Beat-to-beat R–R intervals → RR plot + metrics |
| `detected` (peakLag) | Float (s) | Resonance period from autocorrelation → pacer cycle, readout, ACF peak |
| `band` | `[lo, hi]` (s) | User-set search window for the peak |
| `syncing` | Bool | True briefly when `detected` shifts → "Re-tuning" state |
| derived: `hr`, `hrv (RMSSD)`, `rr`, `swing` | computed over recent window | Metrics + plot header |

**Derivations** (compute over a recent ~12s window of `beats`):
- `hr = round(60000 / mean(rr))`
- `hrv = RMSSD = sqrt(mean(Δrr²))` over successive intervals
- `swing = max(rr) − min(rr)`
- `breaths/min = 60 / detected`
- The pacer cycle length eases smoothly toward `detected` (don't snap).

## Motion reference
- `cc-beat` — heart icon scale pulse, 1s loop: keyframes scale 1 → 1.22 (14%) → 1 (28%) →
  1.1 (42%) → 1 (56%) → 1. A double-thump heartbeat.
- `cc-pulse` — status dot glow ring, 2.4s ease-in-out: ring `0 0 0 3dp` (25% accent) ↔
  `0 0 0 6dp` (6% accent).
- `cc-ripple` — (sensor screen only) expanding rings, not on this dashboard.
- Pacer breathing — see ① above.
- **Respect reduced-motion:** the prototype has a "Reduce motion" toggle that freezes the
  pacer at mid-scale and stops loops. Honor the system reduce-motion setting on Android.

## Assets
No raster assets. All icons are simple line/fill glyphs (heart, ECG, bluetooth, battery,
signal, arrow-back, etc.) — use Material Symbols / `Icons.*` equivalents. The named icons in
the prototype map to Material Symbol names directly (`favorite`, `cardiology`, `monitor_heart`,
`battery_5_bar`, `signal_cellular_alt`, `bluetooth_searching`, `arrow_back`, …). All charts and
the pacer are drawn programmatically — build them as Compose `Canvas` composables.

## Files in this bundle
- `Auto HRV - Dark Material.html` — prototype entry point (theme tokens live in its `<style>`).
- `src/ui.jsx` — shared primitives: `MIcon`, `Card`, `SectionLabel`, `StatusChip`,
  `PillButton`, `TopBar`. **Token values and component styling live here.**
- `src/screens2.jsx` — **the dashboard ("Breathe") screen — your primary reference.**
- `src/viz.jsx` — the charts + breathing engine: `RRTachogram`, `ACFChart`, `BandSlider`,
  `BreathingPacer`, `useBreathEngine`. **The math for the wave/curve/pacer lives here.**
- `src/screens1.jsx`, `src/app.jsx`, `android-frame.jsx`, `tweaks-panel.jsx` — shell, the
  Sensor screen, device frame, and the prototype's tweak panel (not part of the shipped UI).
- `dashboard.png` — screenshot of the dashboard.
- `connect-screen.png` — screenshot of the Sensor screen (connected state).
- `connect-screen-disconnected.png` — screenshot of the Sensor screen (disconnected state).

---

## Screen 2: Sensor / Connect screen

**Entry point:** tapping the status chip (`H10 87%`) in the TopBar navigates here.
Back-navigation via the `←` icon in the header returns to the dashboard.

**Reference file:** `src/screens1.jsx` — `ConnectScreen` component.
**Screenshots:** `connect-screen.png` (connected) · `connect-screen-disconnected.png` (disconnected).

### Navigation bar
A slim back-nav bar replaces the TopBar on this screen:
- Row, padding `10/14/12`. Left: 24dp `arrow_back` icon button (transparent bg, no border,
  6dp internal padding) + "Sensor" title (18sp/600 on-surface).
- No status chip — this is the page that manages the sensor.

### Layout
A scrollable `Column` with 16dp item gap, 4dp top / 22dp bottom padding, 18dp horizontal padding.

```
┌─────────────────────────────────────┐
│ ← Sensor                            │  ← nav bar
├─────────────────────────────────────┤
│  ┌─ Card (centred) ──────────────┐  │  ① sensor identity card
│  │     [ripple rings]            │  │
│  │       ◯ heart icon orb        │  │
│  │       Polar H10               │  │
│  │  SENSOR · E7A9AB27 (mono)     │  │
│  │  [ ● Connected ]              │  │
│  └───────────────────────────────┘  │
│                                      │
│  [Battery 87%] [Signal: Strong] [Contact: Good]  │  ← 3-up stat tiles (connected only)
│                                      │
│  ┌─ Card ───────────────────────┐   │  ③ live HR card (connected only)
│  │ LIVE STREAM          ♥ (beat)│   │
│  │ 59 bpm                       │   │
│  └──────────────────────────────┘   │
│                                      │
│  [ Disconnect / Scan for sensors ]   │  ④ primary CTA button
│  Auto HRV reconnects automatically…  │  ← helper text
└─────────────────────────────────────┘
```

### ① Sensor identity card
Card (`surface`, 1dp outline, 24dp radius), padding 22dp, centred column, 4dp gap.

- **Orb container** (116×116dp, grid centred):
  - When connected: two expanding ripple rings — concentric circles (116dp), 1dp border at
    40% accent, expanding outward via scale animation; two rings offset by 1.4s each, 2.8s
    loop, `ease-out` fade to `opacity=0`. See `cc-ripple` above.
  - **Orb** (88dp circle): when connected — 16% accent over surface-2 bg, 35% accent border;
    when disconnected — surface-2 bg, outline border.
  - **Icon** inside orb: `cardiology` / filled heart, 40dp. Accent when connected, muted when not.

- **Device name:** "Polar H10", 21sp/600 on-surface, `white-space: nowrap`.
- **Device ID:** "SENSOR · E7A9AB27", mono 12sp, faint, letter-spacing 0.08em. (This would
  come from the BLE peripheral name + MAC/UUID.)
- **Status pill** (inline, margin-top 12dp): pill shape (radius 999):
  - **Connected:** bg = 14% accent, 8dp accent dot + "Connected" 13sp/600 accent, padding `7/14`.
  - **Disconnected:** bg = surface-2, 8dp faint dot + "Not connected" 13sp/600 muted, padding `7/14`.

### ② Stat tiles (visible only when connected)
A `Row` of 3 equal tiles, 12dp gap.
Each tile: `surface`, 1dp outline, 18dp radius, padding `14/10`, centred column.
- Icon (20dp accent): `battery_5_bar`, `signal_cellular_alt`, `ecg_heart`.
- Value (15sp/600 on-surface, margin-top 6dp): "87%", "Strong", "Good".
- Label (11.5sp muted, margin-top 2dp): "Battery", "Signal", "Contact".

### ③ Live HR card (visible only when connected)
`Card`, row layout, space-between, default padding (18dp).
- **Left:** "LIVE STREAM" section label + the live heart rate as a large number:
  30sp/600 on-surface (tabular) + "bpm" (mono 13sp muted) baseline-aligned, margin-top 6dp.
- **Right:** filled heart icon (`favorite`), 30dp accent, beating with `cc-beat` 1s loop.

Live HR wanders ±1 bpm in a ~56–67 bpm range, updating ~every second from the sensor stream.

### ④ CTA button + helper text
`PillButton` (full-width, height 52dp, radius 999):
- **Connected state:** `outline` variant — transparent bg, `outline-strong` border (1dp),
  on-surface text/icon. Icon: `bluetooth_disabled`. Label: "Disconnect".
- **Disconnected state:** `filled` variant — solid accent bg, on-accent text/icon. Icon:
  `bluetooth_searching`. Label: "Scan for sensors".

Below the button: centred helper text, 12.5sp faint, padding `0/16`, line-height 1.5:
> "Auto HRV reconnects to your H10 automatically whenever it's in range."

### State & navigation
| State | Connected → | Disconnected → |
|---|---|---|
| Stat tiles | Visible | Hidden |
| Live HR card | Visible | Hidden |
| Button | "Disconnect" (outline) | "Scan for sensors" (filled) |
| Sensor orb | Accent bg + ripple rings | Neutral bg, no rings |
| Status dot | Accent, pulsing | Faint |

Tapping "Disconnect" ends the BLE session → UI switches to disconnected state in-place (no nav).
Tapping "Scan for sensors" starts BLE scan → show a loading/scanning state (out of scope here).
