# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app for real-time HRV (Heart Rate Variability) monitoring via Bluetooth LE using the [Polar BLE SDK](https://github.com/polarofficial/polar-ble-sdk).

- **Language**: Kotlin, JVM 17
- **Min SDK**: 33, **Target/Compile SDK**: 37/37
- **Build tools**: AGP 9.2.1, Kotlin 2.2.10, Gradle 9.5.1

The name is currently a left-over from a Polar example project (PolarSdkEcgHtDemo). The name should be replaced to become "Auto HRV" wherever possible and appropriate.

## Architecture

MVVM with **Hilt** for dependency injection. `AutoHrvApplication` is annotated `@HiltAndroidApp`; repositories (`HrvRepository`, `BreathingSettingsRepository`) are `@Singleton`, injected via constructor; `MainViewModel` exposes `StateFlow` for UI state.

UI is built with **Jetpack Compose**. Entry point is `MainActivity` → `MainScreen`. The `ui/` package is organized per on-screen section (`ui/topbar/`, `ui/coupling/`, `ui/metrics/`, `ui/acf/`, `ui/spectrogram/`), plus `ui/commons/` for shared primitives (cards, headers, chart math), `ui/permissions/` for Android-framework permission plumbing, and `ui/theme/`.

`domain/` is organized per business concern — `domain/signal/` (pure DSP math, no Android/coroutines), `domain/breathing/`, `domain/metrics/`, `domain/spectral/` — each with its own `model/` (domain data classes, `*BO` postfix, e.g. `BreathingPatternBO`, `SpectrogramSliceBO`) and `usecase/` (single-action classes with `operator fun invoke`, per [Android's use case convention](https://developer.android.com/topic/architecture/domain-layer)). Each concern's `*Business` class (e.g. `BreathingBusiness`, `SpectrogramBusiness`, `MetricsBusiness`) is an app-scoped `@Singleton` that reads history from `HrvRepository`, reads user-set state from a settings repository if any, composes the concern's use cases, and exposes the results as hot `StateFlow`s for the ViewModel — it holds no mutable state of its own. User-editable settings (e.g. `BreathingSettingsRepository.targetInOutBias`) live in `data/settings/`, not in a `*Business` class, so state and its derivation stay separate; `MainViewModel` reads/writes such settings repositories directly rather than through a `*Business` class.

Streaming data uses **RxJava3** (from the Polar SDK) bridged to Kotlin Coroutines via `kotlinx-coroutines-rx3`. New code should use the Coroutines/Flow side; only touch RxJava when interfacing directly with the Polar SDK.

### Layer responsibilities

Follows [Android's recommended app architecture](https://developer.android.com/topic/architecture): each layer owns a distinct kind of state, and a value should be transformed into its final display form as late as possible.

- **Data layer** (`data/`) — owns shared, hot observable data: `HrvRepository` subscribes to the Polar BLE stream once and exposes shared (`shareIn`/`stateIn`) flows and a rolling RR history buffer, so every consumer reads from the same subscription rather than each opening its own.
- **Domain layer** (`domain/`: `model/`, `usecase/`, `*Business` classes) — computes domain-meaningful values (e.g. RR intervals, ACF peak, lag seconds) and exposes them in their natural units. No knowledge of how a value will be displayed: no 0..1 normalization for a chart axis, no color/threshold decisions, no display formatting or string resources. If a comment in this layer says something like "left as X since normalization is a display concern," that's the pattern working correctly.
- **UI layer** (`MainViewModel` + `MainScreen`/Compose) — owns presentation logic: normalizing a domain value into the range a chart or progress indicator expects, choosing colors/labels based on thresholds, formatting numbers/durations for display. The ViewModel maps domain `StateFlow`s into UI state; Composables stay declarative and don't reach back into the domain layer directly.

When adding a value that a `*Business`/use case class exposes, ask "is this the domain fact, or a decision about how to draw it?" — the former belongs in `domain/`, the latter in `MainViewModel` (or a UI-layer mapper, e.g. `ui/acf/AcfHistogramShaping.kt`).

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

## Branch conventions

- `feature/<name>` — new features
- `fix/<name>` — bug fixes
- `chore/<name>` — tooling, dependencies, cleanup
