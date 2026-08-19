# iOS integration

TempoTrack's shared module now publishes static Kotlin/Native frameworks for:

- `iosArm64`
- `iosSimulatorArm64`
- `iosX64`

The framework base name is `TempoTrackShared`.

## Entry point

`shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/MainViewController.kt` exposes `MainViewController()` for a Swift/SwiftUI host.

The shared iOS composition root provides:

- monotonic timing through `NSProcessInfo.systemUptime`
- wall-clock timestamps through `NSDate`
- local JSON state backed by `NSUserDefaults`
- the same stopwatch, history, settings, onboarding and About UI used by Android/Desktop

## Export bridge

The primary supported platforms provide native file export implementations. The iOS shared entry point intentionally fails file export with a user-safe message until the host application wires a native document/share-sheet exporter. This avoids requesting filesystem capabilities or silently writing data where the user cannot find it.

A production iOS host should implement the shared `Exporter` contract using `UIDocumentPickerViewController`, `UIActivityViewController`, or another platform-appropriate document flow and then replace the default host exporter at the composition boundary.

## Build examples

On macOS with Xcode installed:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:linkReleaseFrameworkIosArm64
```

Kotlin/Native iOS compilation requires macOS/Xcode. Linux and Windows CI should not claim iOS framework verification.

## Privacy

TempoTrack does not require an account or network service for stopwatch data. The iOS adapter keeps app state local to the application container. A host export bridge should only write/share data after an explicit user action.
