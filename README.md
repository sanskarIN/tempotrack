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
>
> Current release line: **2.0.12**

## Highlights

- Start, pause, resume, reset, lap and split timing.
- Millisecond display precision.
- Monotonic live timing with platform-aware crash/restart recovery.
- Android/iOS uptime-vs-wall recovery validation so reboot/reset checkpoints fail safely to paused.
- Desktop process-restart recovery backed by a five-second running-checkpoint heartbeat.
- Fastest, slowest and average lap statistics with recorded/fastest/slowest sorting and overflow-safe integer averaging.
- Named, searchable sessions stored locally, with rename and undo-delete flows.
- CSV and JSON export plus validated JSON history restore.
- Android JSON/CSV system sharing through a restricted `FileProvider` cache path.
- iOS JSON/CSV system sharing through a native `UIActivityViewController` bridge.
- iOS direct JSON/CSV destination selection through `UIDocumentPickerViewController`.
- Desktop native save-file chooser with explicit cancellation handling.
- Bounded shared export-filename sanitization for platform file operations.
- Versioned local session, preference, and active-stopwatch storage with legacy migration.
- Active-stopwatch schema v2 with elapsed-at-save and wall-save recovery metadata.
- Persistent active-stopwatch checkpoints and Desktop mini-stopwatch visibility.
- Light, dark and system themes.
- Large-control accessibility mode and reduced-motion preference.
- Adaptive navigation for compact and large-screen layouts.
- Localization-ready shared Compose string resources.
- Android and Desktop applications with shared Kotlin/Compose UI and domain logic.
- Kotlin/Native iOS framework targets and a Compose `UIViewController` entry point for host integration.
- Desktop floating mini-stopwatch support.
- Desktop keyboard shortcuts: Space start/pause/resume, L lap, R reset, with in-app shortcut help and a persistent enable/disable setting.
- No account, ads, analytics SDK, or required network connection.

## Screenshots

Real release screenshots will replace these placeholders once verified tagged builds are captured on the supported primary platforms.

| Android | Desktop |
|---|---|
| `docs/screenshots/android-stopwatch.png` | `docs/screenshots/desktop-history.png` |

## Supported platforms

| Platform | Status |
|---|---|
| Android 8.0+ (API 26+) | Primary |
| Windows/macOS/Linux Desktop | Primary |
| iOS | Shared framework + Compose host entry point + native document export + native system sharing |

## Tech stack

- Kotlin 2.4.10
- Compose Multiplatform 1.11.1
- Android Gradle Plugin 9.3.1
- AndroidX Core 1.19.0
- Gradle 9.5.0
- Kotlinx Coroutines 1.11.0
- Kotlinx Serialization 1.11.0

## Quick start

Requirements:

- JDK 17 or newer
- Gradle 9.5.0
- Android Studio with Android SDK 37 for Android builds
- A desktop OS supported by Compose Desktop
- macOS with Xcode for iOS framework compilation/tests and iOS host execution

The launcher scripts use `gradle/wrapper/gradle-wrapper.jar` when that standard binary is present. In this repository state the wrapper JAR is not committed, so the launchers deliberately require an installed **Gradle 9.5.0** and reject a mismatched fallback version instead of silently building with a different toolchain. The wrapper properties pin the Gradle 9.5.0 binary distribution SHA-256 and bounded retry/backoff settings for a future trusted wrapper generation.

```bash
git clone https://github.com/sanskarIN/tempotrack.git
cd tempotrack

# Verify deterministic repository/toolchain guards
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py

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

Build the iOS Simulator framework on macOS:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

See [docs/setup.md](docs/setup.md) for full setup instructions.

## Architecture

TempoTrack uses a modular-monolith structure:

- `shared/` — domain model, versioned storage contracts/codecs, checkpoint recovery policy, serialization, shared Compose UI/resources, tests, and iOS adapters/export/share bridges.
- `androidApp/` — Android entry point, uptime monotonic clock, reboot-aware checkpoint recovery, atomic private-file storage, MediaStore export, and secure operating-system sharing.
- `desktopApp/` — Desktop entry point, JVM monotonic clock, process-restart-safe checkpoint recovery/heartbeat, atomic JVM storage, native export destination selection, keyboard shortcuts, and mini-window integration.

The stopwatch engine never derives live elapsed duration from the wall clock. Wall time is stored only as recovery metadata so Android/iOS can validate whether a persisted uptime reference still belongs to the same boot. See [ADR 0005](docs/adr/0005-platform-checkpoint-recovery.md).

See [docs/architecture.md](docs/architecture.md) and [docs/adr/0001-monotonic-time.md](docs/adr/0001-monotonic-time.md).

## Data portability

History can be exported as JSON or CSV. JSON exports can be restored after validation and explicit replacement confirmation. Android can send the same JSON/CSV payloads to the system share sheet through a restricted `FileProvider`. iOS can either present a system document picker for a user-selected export destination or present the native activity sheet for sharing. Desktop export opens the platform file chooser so the destination remains under user control.

The iOS export/share bridges stage each operation in a unique temporary directory using the sanitized requested filename and remove that temporary directory when the native flow completes or fails.

The internal persistence format is independently versioned and migrated; portable JSON exports remain plain session lists so backups are not coupled to the internal storage envelope. Restore limits are deliberately aligned with local persistence limits so a valid self-export is not rejected merely by a smaller importer cap.

## Documentation

The complete documentation index is [docs/README.md](docs/README.md). High-value references include:

- [User guide](docs/user-guide.md) — stopwatch, laps, history, export/share/restore, settings, and recovery behavior.
- [Repository file reference](docs/repository-reference.md) — tracked-file-by-file ownership and maintenance notes.
- [Source code reference](docs/code-reference.md) — classes, interfaces, functions, invariants, and platform adapters.
- [State and recovery](docs/state-and-recovery.md) — state machine, rebased checkpoints, reboot/process recovery.
- [Data model and storage](docs/data-model-and-storage.md) — schemas, limits, migrations, JSON/CSV portability, storage locations.
- [Platform behavior](docs/platforms.md) — Android/Desktop/iOS differences and native boundaries.
- [Build system and CI](docs/build-and-ci.md) — Gradle modules/toolchain, CI, CodeQL, signing, and release jobs.
- [Security model](docs/security-model.md) — trust boundaries, malformed input, sharing, storage integrity, and supply chain.
- [Maintainer guide](docs/maintainer-guide.md) — safe change recipes and documentation/test update matrix.

## Testing

```bash
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
./gradlew :shared:allTests
./gradlew :desktopApp:test
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
python tools/check_markdown_links.py
```

CI also performs Gradle-version alignment verification, Android/Desktop builds, iOS simulator framework verification, documentation-link checks, Kotlin namespace syntax checks, exhaustive tracked-file documentation coverage, and security scanning. See [docs/testing.md](docs/testing.md).

Kotlin source uses `` `in`.sanskar... `` rather than unescaped `in.sanskar...` because `in` is a Kotlin keyword; the compiled/runtime package is still `in.sanskar...`.

## Build and release

Desktop packages:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Android release builds require production signing configuration before public distribution. Signing secrets are intentionally not committed. Tag builds derive package versions from strict `vMAJOR.MINOR.PATCH` tags, package platform artifacts, generate SHA-256 checksums, and publish GitHub Release assets after the jobs succeed.

See [docs/release.md](docs/release.md) and [docs/build-and-ci.md](docs/build-and-ci.md).

## Privacy and security

TempoTrack is local-first. The application includes no analytics, ads, authentication service, or app-managed cloud synchronization. Export and sharing happen only after explicit user actions. Android sharing uses an app-cache file exposed through a non-exported `FileProvider` with temporary read permission. iOS export/share flows use isolated app-temporary staging files and native system destination UI. Android platform backup/device-transfer behavior is documented separately because it is controlled by the operating system.

Read [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md), and [docs/security-model.md](docs/security-model.md).

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md), [docs/maintainer-guide.md](docs/maintainer-guide.md), run the quality suite, and keep changes focused.

## License

MIT — see [LICENSE](LICENSE).

## Contact and support

- Business: **sanskarin@outlook.in**
- Business: **sanskarin.business@gmail.com**
- Support: **supportramsandesh@gmail.com**
- GitHub: https://github.com/sanskarIN
- Buy Me a Coffee: https://buymeacoffee.com/sanskarIN

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-sanskarIN-FFDD00?logo=buy-me-a-coffee&logoColor=000000)](https://buymeacoffee.com/sanskarIN)
