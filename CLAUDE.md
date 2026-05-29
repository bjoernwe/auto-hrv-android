# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app for real-time HRV (Heart Rate Variability) monitoring via Bluetooth LE using the [Polar BLE SDK](https://github.com/polarofficial/polar-ble-sdk).

- **Language**: Kotlin, JVM 17
- **Min SDK**: 24, **Target/Compile SDK**: 33/36
- **Build tools**: AGP 8.13.2, Kotlin 2.1.0, Gradle 8.13

## Architecture

MVVM with manual dependency injection (no Hilt/Koin). `PolarApplication` holds a lazy singleton `PolarRepository`; `PolarViewModel` exposes `StateFlow` for UI state.

Streaming data uses **RxJava3** (from the Polar SDK) bridged to Kotlin Coroutines via `kotlinx-coroutines-rx3`. New code should use the Coroutines/Flow side; only touch RxJava when interfacing directly with the Polar SDK.

## UI direction: full Compose rewrite

The `feature/compose` branch is a planned **full rewrite** of all Views to Jetpack Compose. Do not add new XML layouts or Views — implement all new UI in Compose.

Compose dependencies are not yet in `app/build.gradle`; add them before writing Compose code.

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

**Hardcoded device ID**: `DEVICE_ID = "E7A9AB27"` is hardcoded in `PolarRepository`. When adding device-selection UI, this constant is the only place to change.

**Bluetooth permissions split by API level**: Permissions differ between API ≤30 and API 31+. `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` are for API 31+; `BLUETOOTH`/`BLUETOOTH_ADMIN`/`ACCESS_FINE_LOCATION` are for API ≤30 (`maxSdkVersion=30`). Both sets must be maintained in the manifest and handled at runtime.

## Branch conventions

- `feature/<name>` — new features
- `fix/<name>` — bug fixes
- `chore/<name>` — tooling, dependencies, cleanup