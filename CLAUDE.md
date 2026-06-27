# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app for real-time HRV (Heart Rate Variability) monitoring via Bluetooth LE using the [Polar BLE SDK](https://github.com/polarofficial/polar-ble-sdk).

- **Language**: Kotlin, JVM 17
- **Min SDK**: 24, **Target/Compile SDK**: 37/37
- **Build tools**: AGP 9.2.1, Kotlin 2.2.10, Gradle 9.5.1

The name is currently a left-over from a Polar example project (PolarSdkEcgHtDemo). The name should be replaced to become "Auto HRV" wherever possible and appropriate.

## Architecture

MVVM with **Hilt** for dependency injection. `AutoHrvApplication` is annotated `@HiltAndroidApp`; `HrvRepository` is a `@Singleton` injected via constructor; `HrvViewModel` exposes `StateFlow` for UI state.

UI is built with **Jetpack Compose**. Entry point is `HRActivity` → `HRScreen`. There is a `domain/` layer containing use cases (e.g. `BreathingPacerUseCase`, `TimeSeriesStatsUseCase`).

Streaming data uses **RxJava3** (from the Polar SDK) bridged to Kotlin Coroutines via `kotlinx-coroutines-rx3`. New code should use the Coroutines/Flow side; only touch RxJava when interfacing directly with the Polar SDK.

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

## Key gotchas

**Hardcoded device ID**: `DEVICE_ID = "E7A9AB27"` is hardcoded in `HrvRepository`. When adding device-selection UI, this constant is the only place to change.

## Branch conventions

- `feature/<name>` — new features
- `fix/<name>` — bug fixes
- `chore/<name>` — tooling, dependencies, cleanup
