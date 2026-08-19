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
- direct JSON/CSV document export through `UIDocumentPickerViewController`;
- the same stopwatch, history, settings, onboarding and About UI used by Android/Desktop.

## Temporary export-file policy

`IosTemporaryExportFile.kt` is the common staging boundary used by native share and direct export flows. Each operation:

1. sanitizes the user-visible filename using the shared export filename policy;
2. creates a unique operation directory below `NSTemporaryDirectory()`;
3. writes the UTF-8 JSON/CSV payload atomically using the sanitized filename;
4. removes the entire operation directory when the platform flow completes, is dismissed, is cancelled, or fails to present.

Using a unique directory preserves the requested filename while preventing simultaneous operations from overwriting one another.

## Native share bridge

`IosShareService` implements the shared `ShareService` contract. When the user chooses a History share action it:

1. prepares a temporary export file;
2. presents `UIActivityViewController` from the Compose host controller;
3. configures a source view/source rectangle when UIKit provides a popover presentation controller;
4. removes the temporary operation directory from the activity controller completion handler.

Only one activity sheet is presented by the service at a time. The destination remains under operating-system/user control.

## Direct document export

`IosDocumentExporter` implements the shared `Exporter` contract. It prepares a temporary source file and presents `UIDocumentPickerViewController(forExportingURLs:)` so the user chooses a document destination outside the app sandbox.

The exporter keeps the picker delegate strongly referenced for the lifetime of the operation and returns:

- `ExportResult.Success` after the picker reports a selected destination;
- `ExportError.USER_CANCELLED` when the picker delegate reports cancellation;
- `ExportError.WRITE_FAILED` if the temporary source file cannot be prepared or the picker reports no destination;
- `ExportError.PLATFORM_EXPORT_UNAVAILABLE` if the native picker cannot be presented.

The exporter serializes picker operations with a mutex and removes its temporary source directory in a non-cancellable cleanup section.

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
- History Export JSON and Export CSV present the system document picker;
- choosing a destination produces a readable file with the sanitized suggested filename;
- cancelling the document picker returns to History without a false write-failure message;
- History Share JSON and Share CSV present the system activity sheet;
- the activity sheet is anchored correctly on iPad/regular-width presentation;
- canceling/dismissing the system activity sheet leaves app history unchanged;
- temporary operation directories are cleaned after picker/share completion;
- the About screen displays the host app's bundle version;
- no export/share file is created before an explicit user action.

## Privacy

TempoTrack does not require an account or network service for stopwatch data. The iOS adapter keeps app state local to the application container. Native export/share operations stage temporary files only after explicit user actions. The user-selected document destination or system-share service has its own privacy behavior and terms.
