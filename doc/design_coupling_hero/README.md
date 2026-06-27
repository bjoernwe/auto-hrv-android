# Design: Coupling Hero

Replaces the breathing-orb hero on the main dashboard with a chart that shows the breath pacer
and live heart-rate trace on a shared time axis.

## Concept

The hero makes the app's core insight *visible*: during resonance-frequency breathing, the heart
oscillates at the same frequency as the breath. You watch the two rhythms ride together.

**Key honesty constraint**: breath and heart are not in-phase — the heart trails the breath by a
measured lag τ (baroreflex delay, person- and frequency-dependent). The design treats τ as a
first-class readout, not something to hide.

## Waves

- **Teal (breath)**: synthetic pacer wave reconstructed from `BreathingState` phase/progress +
  `BreathingPattern` cycle length. Shown as a smooth sinusoid with area fill.
- **Coral (heart)**: live beat-to-beat R–R intervals from `rrsMsHistory`, inverted
  (inhale → HR ↑ → RR ↓ → display rises) so both curves peak together during inhale.
  The faint ghost behind the main line makes the wave shape visible even at low amplitude.
- **White bloom**: when `isInResonance`, a soft radial gradient brightens the right portion,
  making the coupling visually self-evident.

## Palette (Dusk Instrument)

| Token | Hex | Role |
|---|---|---|
| void (bg) | `#0B0E16` | Screen background |
| breath | `#5EE0C8` | Teal — same as `primary` in existing theme |
| heart | `#FF9E7D` | Coral — same as `secondary` in existing theme |
| lock | `#FBFEFF` | Near-white bloom on lock |

## Live prototype

Open `coupling-hero.html` in a browser. It auto-cycles tuning → locked over ~24 s so you can
watch the transition. Pass `?frame=tuning` or `?frame=locked` for static screenshots.

## Future: τ computation

`lagSeconds` in `HrUiState` is currently `null` (stubbed). To compute it:
- Cross-correlate a synthetic pacer time series against the resampled RR series.
- The cross-correlation peak position = τ.
- Same machinery as the existing ACF (`TimeSeriesStatsUseCase`), pointed at two signals.
- When available, the chart can render the lag-compensated aligned heart trace + the
  τ bracket overlay.