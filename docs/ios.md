# iOS integration

TempoTrack's shared module publishes static Kotlin/Native frameworks for:

- `iosArm64`
- `iosSimulatorArm64`
- `iosX64`

The framework base name is `TempoTrackShared`.

## Entry point

`shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/MainViewController.kt` exposes `MainViewController()` for a Swift/SwiftUI host.

The shared iOS composition root provides:

- monotonic timing through `NSProcessInfo.systemUptime`;
- wall-clock timestamps through `NSDate`;
- local JSON state backed by `NSUserDefaults`;
- host bundle version display from `CFBundleShortVersionString`;
- native JSON/CSV sharing through `UIActivityViewController`;
- the same stopwatch, history, settings, onboarding and About UI used by Android/Desktop.

## Native share bridge

`IosShareService` implements the shared `ShareService` contract. When the user chooses a History share action it:

1. sanitizes the suggested filename using the shared export filename policy;
2. writes the UTF-8 JSON/CSV payload atomically to the app's temporary directory;
3. creates a file URL for that temporary file;
4. presents `UIActivityViewController` from the Compose host controller;
5. configures a source view/source rectangle when UIKit provides a popover presentation controller, so the activity sheet has an anchor on iPad-class presentations.

The destination remains under operating-system/user control. Temporary files are not a cloud-sync mechanism and are not created unless the user explicitly starts sharing.

## Direct document export

The shared `Exporter` boundary still returns `PLATFORM_EXPORT_UNAVAILABLE` on iOS. The History share actions are functional through the native share service, but a separate direct "save/export to a chosen document location" path remains host work.

A future direct export implementation should use a native document picker/export flow and preserve the shared `Exporter` result contract, including explicit user cancellation. Do not silently substitute an app-private file path and report it as a user-selected export.

## Build examples

On macOS with Xcode installed:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:linkReleaseFrameworkIosArm64
```

Kotlin/Native iOS compilation requires macOS/Xcode. Linux and Windows validation should not claim iOS framework verification.

## Manual iOS verification

Run the containing iOS host on both compact and regular-width presentations where practical and verify:

- start/pause/resume/lap/reset and active-checkpoint recovery;
- History JSON and CSV share actions present the system activity sheet;
- the activity sheet is anchored correctly on iPad/regular-width presentation;
- canceling/dismissing the system activity sheet leaves app history unchanged;
- the About screen displays the host app's bundle version;
- no share file is created before an explicit user share action.

## Privacy

TempoTrack does not require an account or network service for stopwatch data. The iOS adapter keeps app state local to the application container. Native sharing only prepares a temporary file after an explicit user action; the service selected in the system activity sheet has its own privacy behavior and terms.
