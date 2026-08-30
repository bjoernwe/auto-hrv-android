# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app for real-time HRV (Heart Rate Variability) monitoring via Bluetooth LE using the [Polar BLE SDK](https://github.com/polarofficial/polar-ble-sdk).

- **Language**: Kotlin, JVM 17
- **Min SDK**: 33, **Target/Compile SDK**: 37/37
- **Build tools**: AGP 9.2.1, Kotlin 2.2.10, Gradle 9.5.1

The name is currently a left-over from a Polar example project (PolarSdkEcgHtDemo). The name should be replaced to become "Auto HRV" wherever possible and appropriate.

## Architecture

MVVM with **Hilt** for dependency injection. `AutoHrvApplication` is annotated `@HiltAndroidApp`; `HrvRepository` is a `@Singleton` injected via constructor; `HrvViewModel` exposes `StateFlow` for UI state.

UI is built with **Jetpack Compose**. Entry point is `HRActivity` → `HRScreen`. There is a `domain/` layer containing use cases (e.g. `BreathingPacerUseCase`, `TimeSeriesStatsUseCase`) and per-feature `*Business` classes (e.g. `BreathingBusiness`, `SpectrogramBusiness`) that compose them into `StateFlow`s the ViewModel consumes.

Streaming data uses **RxJava3** (from the Polar SDK) bridged to Kotlin Coroutines via `kotlinx-coroutines-rx3`. New code should use the Coroutines/Flow side; only touch RxJava when interfacing directly with the Polar SDK.

### Layer responsibilities

Follows [Android's recommended app architecture](https://developer.android.com/topic/architecture): each layer owns a distinct kind of state, and a value should be transformed into its final display form as late as possible.

- **Domain layer** (`domain/`: use cases and `*Business` classes) — computes domain-meaningful values (e.g. RR intervals, ACF peak, lag seconds) and exposes them in their natural units. No knowledge of how a value will be displayed: no 0..1 normalization for a chart axis, no color/threshold decisions, no display formatting or string resources. If a comment in this layer says something like "left as X since normalization is a display concern," that's the pattern working correctly.
- **UI layer** (`HrvViewModel` + `HRScreen`/Compose) — owns presentation logic: normalizing a domain value into the range a chart or progress indicator expects, choosing colors/labels based on thresholds, formatting numbers/durations for display. The ViewModel maps domain `StateFlow`s into UI state; Composables stay declarative and don't reach back into the domain layer directly.

When adding a value that a `*Business`/use case class exposes, ask "is this the domain fact, or a decision about how to draw it?" — the former belongs in `domain/`, the latter in `HrvViewModel`.

## Build & test commands

```bash
./gradlew build               # Full build
./gradlew installDebug        # Build and install debug APK
./gradlew assembleRelease     # Build release APK
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumented tests (device/emulator required)
./gradlew ktlintCheck         # Check Kotlin style
./gradlew ktlintFormat        # Auto-fix style issues
./gradlew detekt              # Static analysis (config: config/detekt/detekt.yml)
```

## Known issues / tech debt

- **Hardcoded device ID**: `DEVICE_ID = "E7A9AB27"` is hardcoded in `HrvRepository`. When adding device-selection UI, this constant is the only place to change.
- **`HrvRepository` RR/HR pipeline duplicated per consumer**: `hrFlow` → `rrMsBeatFlow` → `rrMsResampled1Hz` is cold, so each caller of `getRrsMs1HzHistory` (`BreathingBusiness`, `SpectrogramBusiness`) gets its own BLE subscription, outlier filter, debounce, and resampling loop over the same signal. Fix: give the repository a scope, `shareIn` `hrFlow`, and replace the per-call `.windowedTo(seconds)` with one shared `stateIn`'d rolling buffer that callers `.takeLast(seconds)` from. Deferred — touches the BLE subscription path and the existing ACF pipeline, needs on-device testing.
- **ACF histogram chart-shaping lives in the domain layer, violating [Layer responsibilities](#layer-responsibilities)**: `domain/breathing/AcfHistogram.kt`'s `shapeAcfHistogram` runs accumulated ACF sums through `cap → exp → normalize → sigmoid` to produce chart-ready `[0, 1]` bar heights — a display decision, not a domain fact. `domain/ListNormalization.kt` (`normalizeMinMax`) exists only to support this shaping and is itself a presentation utility misplaced under `domain/`. `BreathingConfig` (`acfHistogramExpGain`, `acfHistogramSigmoidSteepness`, `acfHistogramSigmoidMidpoint`, `acfHistogramIgnoredLeadingLags`) mixes chart-tuning knobs into physiological config. `BreathingBusiness.acfHistogram` exposes the already-shaped `StateFlow<List<Float>>`, and `HrvViewModel` just passes it straight into `HrUiState.acfHistogram` with no transformation. Fix: move the shaping into the ViewModel (or a UI-layer mapper), have the domain layer expose the accumulated raw ACF sums instead, and drop the chart-tuning fields out of `BreathingConfig`. Contrast with `SpectrogramSlice.powerByFreqBin`, which stays raw domain power and is normalized/color-mapped in `SpectrogramChart.kt` at draw time — that's the pattern to follow.

## Branch conventions

- `feature/<name>` — new features
- `fix/<name>` — bug fixes
- `chore/<name>` — tooling, dependencies, cleanup
