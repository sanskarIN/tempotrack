<p align="center">
  <img src="docs/assets/logo.svg" alt="TempoTrack logo" width="120" />
</p>

<h1 align="center">TempoTrack</h1>

<p align="center">
  A precise, privacy-first stopwatch with reliable monotonic timing, laps, searchable local history, and portable exports.
</p>

<p align="center">
  <a href="https://buymeacoffee.com/sanskarIN">
    <img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-sanskarIN-FFDD00?logo=buy-me-a-coffee&logoColor=000000" alt="Buy Me a Coffee" />
  </a>
</p>

> **Made by the Sanskar**

## Highlights

- Start, pause, resume, reset, lap and split timing.
- Millisecond display precision.
- Monotonic timing: Android uses `SystemClock.elapsedRealtimeNanos()` so device sleep is included.
- Fastest, slowest and average lap statistics.
- Named, searchable sessions stored locally.
- CSV and JSON export.
- Persistent active-stopwatch checkpoints.
- Light, dark and system themes.
- Large-control accessibility mode and reduced-motion preference.
- Android and Desktop applications with shared Kotlin/Compose UI and domain logic.
- Desktop floating mini-stopwatch support.
- Desktop keyboard shortcuts: Space start/pause/resume, L lap, R reset.
- No account, ads, analytics SDK, or required network connection.

## Screenshots

Real release screenshots will replace these placeholders once the first tagged build is captured on each supported platform.

| Android | Desktop |
|---|---|
| `docs/screenshots/android-stopwatch.png` | `docs/screenshots/desktop-history.png` |

## Supported platforms

| Platform | Status |
|---|---|
| Android 8.0+ (API 26+) | Primary |
| Windows/macOS/Linux Desktop | Primary |
| iOS | Architecture-ready; native entry point not shipped in v1.0 |

## Tech stack

- Kotlin 2.4.10
- Compose Multiplatform 1.11.0
- Android Gradle Plugin 9.3.0
- Gradle 9.5.0
- Kotlinx Coroutines 1.11.0
- Kotlinx Serialization 1.11.0

The version choices follow the current stable Kotlin/Compose/Android toolchain available when this repository was implemented.

## Quick start

Requirements:

- JDK 17 or newer
- Gradle 9.5.0 (the bootstrap scripts use a standard wrapper JAR if one is present, otherwise they delegate to an installed `gradle`)
- Android Studio with Android SDK 37 for Android builds
- A desktop OS supported by Compose Desktop

```bash
git clone https://github.com/sanskarIN/tempotrack.git
cd tempotrack

# Linux/macOS
./gradlew quality

# Windows
gradlew.bat quality
```

Run desktop:

```bash
./gradlew :desktopApp:run
```

Build Android debug APK:

```bash
./gradlew :androidApp:assembleDebug
```

See [docs/setup.md](docs/setup.md) for full setup instructions.

## Architecture

TempoTrack uses a modular-monolith structure:

- `shared/` — domain model, storage contracts, serialization, shared Compose UI, tests.
- `androidApp/` — Android entry point, Android monotonic clock, file storage/export.
- `desktopApp/` — Desktop entry point, JVM storage/export, mini-window integration.

The stopwatch engine never derives elapsed duration from the wall clock. A `MonotonicClock` is injected so elapsed-time behavior can be tested deterministically.

See [docs/architecture.md](docs/architecture.md) and [docs/adr/0001-monotonic-time.md](docs/adr/0001-monotonic-time.md).

## Testing

```bash
./gradlew :shared:allTests
./gradlew :desktopApp:test
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
```

CI also performs builds and security scanning. See [docs/testing.md](docs/testing.md).

## Build and release

Desktop packages:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Android release builds require your own local signing configuration. Signing secrets are intentionally not committed.

See [docs/release.md](docs/release.md).

## Privacy and security

TempoTrack is local-first. Session data is stored in application-private storage and is not transmitted by the project. Export only happens after an explicit user action.

Read [PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md).

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md), run the quality suite, and keep changes focused.

## License

MIT — see [LICENSE](LICENSE).

## Contact and support

- Business: **sanskarin@outlook.in**
- Business: **sanskarin.business@gmail.com**
- Support: **supportramsandesh@gmail.com**
- GitHub: https://github.com/sanskarIN
- Buy Me a Coffee: https://buymeacoffee.com/sanskarIN

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-sanskarIN-FFDD00?logo=buy-me-a-coffee&logoColor=000000)](https://buymeacoffee.com/sanskarIN)
