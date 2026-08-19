# Platform Behavior

TempoTrack shares domain logic, repositories, serialization, resources, and Compose UI across supported targets. Platform modules/adapters provide clocks, durable storage, export/share integration, lifecycle recovery policy, packaging, and host-only capabilities.

## Platform matrix

| Capability | Android | Desktop | iOS |
|---|---|---|---|
| Shared Compose UI | Yes | Yes | Yes, through Compose `UIViewController` |
| Live monotonic clock | `SystemClock.elapsedRealtimeNanos()` | `System.nanoTime()` | `NSProcessInfo.systemUptime` |
| Wall metadata clock | `System.currentTimeMillis()` | `System.currentTimeMillis()` | `NSDate` epoch time |
| Saved sessions | private file JSON | `~/.tempotrack` file JSON | `NSUserDefaults` string |
| Preferences | private file JSON | `~/.tempotrack` file JSON | `NSUserDefaults` string |
| Active checkpoint | private file JSON | `~/.tempotrack` file JSON | `NSUserDefaults` string |
| Running restart policy | uptime/wall consistency check | restore paused | uptime/wall consistency check |
| Running heartbeat | No periodic shared heartbeat configured | 5 seconds | No periodic shared heartbeat configured |
| Direct export | MediaStore/app Documents | Swing save chooser | document picker |
| Share service | Android chooser + `FileProvider` | Not currently exposed | activity sheet |
| Mini stopwatch | No | Yes | No |
| Keyboard shortcuts | No | Yes | No |
| Release signing owner | Android Gradle/GitHub protected secrets | OS package/signing process | containing Xcode app |

## Shared contract vs platform implementation

The common layer asks each platform for:

- `MonotonicClock`;
- `WallClock`;
- `StringStorage` for three logical stores;
- `Exporter`;
- optional `ShareService`;
- checkpoint recovery function;
- optional heartbeat interval;
- optional Desktop capability hooks.

This keeps platform APIs out of shared domain/data/UI source.

## Android

### Supported SDK contract

From the version catalog/build configuration:

- minimum SDK: 26 (Android 8.0);
- target SDK: 37;
- compile SDK: 37.

### Application entry point

`MainActivity` builds dependencies once during `onCreate`:

```text
filesDir/sessions.json
filesDir/preferences.json
filesDir/active-stopwatch.json
```

It injects:

- elapsed realtime monotonic clock;
- epoch wall clock;
- JSON repositories;
- Android exporter/share service;
- system-uptime checkpoint recovery.

### Timing/recovery

Android's `elapsedRealtimeNanos()` is appropriate for stopwatch timing because it is monotonic and includes time spent in device sleep.

It can survive an application process restart while the device remains booted. It resets across reboot. TempoTrack therefore compares elapsed uptime with elapsed wall time stored at checkpoint save.

Same-boot plausible references can continue running. Reboot/reset/implausible references restore paused at the last safe elapsed duration.

### Private persistence

`AndroidStringStorage` writes a sibling temporary file and attempts atomic replacement. It validates parent directory state and only falls back when the filesystem specifically reports atomic move unsupported.

The store is application-private; no storage permission is required for these logical stores.

### Android 10+ export

`AndroidExporter` inserts into `MediaStore.Downloads`:

```text
RELATIVE_PATH = Downloads/TempoTrack
IS_PENDING = 1
```

It writes UTF-8 data, then clears the pending flag. The adapter requires exactly one row to be updated during finalization. On write/finalization failure it deletes the inserted item rather than returning a false success.

### Pre-Android-10 export

Older supported Android versions use the app-specific external Documents directory under a `TempoTrack` child directory.

Export collision behavior:

```text
sessions.json
sessions (1).json
sessions (2).json
...
```

Filename reservation uses `createNewFile()` and therefore does not overwrite an existing backup.

### Android sharing

Sharing is explicit user action only.

Preparation:

1. create/validate `cache/shared-exports`;
2. create unique per-operation file;
3. write UTF-8 payload;
4. obtain `content://` URI through `FileProvider`;
5. attach URI as `EXTRA_STREAM` and `ClipData`;
6. grant temporary read permission;
7. present `ACTION_SEND` chooser.

The manifest provider is:

- `android:exported="false"`;
- `android:grantUriPermissions="true"`.

`file_paths.xml` restricts provider exposure to the intended cache subtree rather than all files/cache.

A later share operation receives a different staged file, so it cannot rewrite the bytes behind an earlier granted URI.

### Android backup

The manifest opts into Android backup/device transfer and points to explicit legacy/new extraction-rule XML files.

Transient active checkpoint and share/export staging behavior must remain consistent with `PRIVACY.md` and the backup XML rules. Do not broaden backup roots as a shortcut.

### Android branding

- launcher/round icon use `ic_launcher.xml`;
- base style defines TempoTrack launch theme;
- Android 12+ style uses platform splash attributes;
- splash drawable provides the branded background.

### Android tests that do not require a device

`androidApp/src/test` covers pure filesystem staging behavior. Android framework/MediaStore/FileProvider behavior still needs lint/build and manual/device verification.

## Desktop

### Runtime

Desktop is a JVM 17 Compose Desktop application.

Private application directory:

```text
${user.home}/.tempotrack
```

Files:

```text
sessions.json
preferences.json
active-stopwatch.json
```

### Timing/recovery

`System.nanoTime()` is valid for interval measurement inside the current JVM but its origin cannot be treated as durable across process launches.

Therefore:

- live running duration uses `System.nanoTime()`;
- persisted RUNNING checkpoints restore PAUSED after a new process launch;
- while running, TempoTrack persists a rebased checkpoint every 5,000 ms.

The heartbeat limits recent elapsed loss after an abrupt crash/kill while avoiding invalid cross-process monotonic comparison.

### Keyboard shortcuts

When the persisted shortcut preference is enabled:

| Key | Action |
|---|---|
| Space | Start / pause / resume depending on current state |
| L | Lap |
| R | Reset |

Shortcut actions persist/clear active checkpoint just like their shared-screen equivalents.

### Mini stopwatch

The Desktop host can open an always-on-top compact window that receives the same `StopwatchEngine` instance as the main app.

Closing the mini window:

- updates host visibility immediately;
- loads current preferences;
- persists `miniStopwatchVisible = false`.

This prevents an explicitly closed mini window from unexpectedly reopening after restart.

### Desktop export

`DesktopExporter` opens Swing `JFileChooser` on the Event Dispatch Thread. When called off the EDT it uses `SwingUtilities.invokeAndWait`.

Result distinctions:

- approved destination → UTF-8 write;
- chooser cancel → `USER_CANCELLED`;
- chooser creation/platform failure → `PLATFORM_EXPORT_UNAVAILABLE`;
- filesystem write failure → `WRITE_FAILED`.

### Desktop packaging

Compose Desktop targets:

- DMG;
- MSI;
- DEB.

`packageVersion` comes from `appVersion` in Gradle properties. The same version is passed to the JVM as `tempotrack.version` so the About screen reflects package metadata.

Package generation is host-specific; one OS cannot be assumed to validate every target package format.

## iOS

### Architecture status

TempoTrack provides Kotlin/Native framework targets:

- iOS x64;
- iOS arm64;
- iOS Simulator arm64.

Framework base name: `TempoTrackShared`.

`MainViewController()` returns the Compose UIKit host controller. A containing Xcode application is still required for application target settings, bundle identity, signing, device deployment, and App Store packaging.

### Storage

`IosStringStorage` stores JSON strings in `NSUserDefaults` keys:

```text
tempotrack.sessions
tempotrack.preferences
tempotrack.active-stopwatch
```

Shared repositories still own schema envelopes, limits, validation, sorting, and migration.

### Timing/recovery

`NSProcessInfo.processInfo.systemUptime` is converted from seconds to nanoseconds and used as the monotonic source.

`NSDate().timeIntervalSince1970` supplies wall metadata.

As on Android, same-boot plausible uptime/wall deltas can continue a running timer; reboot/reset/legacy ambiguity restores paused at the last safe elapsed value.

### Temporary staging

Both native export and share create a unique operation directory below `NSTemporaryDirectory()`:

```text
tempotrack-<UUID>/<sanitized filename>
```

Content is written UTF-8/atomically through Foundation APIs.

The operation object keeps both file URL and directory path so cleanup removes the whole isolated staging directory rather than only one filename.

### iOS document export

`IosDocumentExporter`:

- serializes picker operations with a `Mutex`;
- creates staging off main dispatcher;
- presents `UIDocumentPickerViewController(forExportingURLs:asCopy:)` on main dispatcher;
- enables file extensions;
- strongly retains the picker delegate;
- maps selection to success;
- maps picker cancellation to `USER_CANCELLED`;
- maps presentation failure to platform-unavailable;
- dismisses on coroutine cancellation;
- removes staging in a non-cancellable cleanup block.

### iOS sharing

`IosShareService` presents `UIActivityViewController` with the staged file URL.

It:

- prevents overlapping active activity controllers;
- retains the active controller;
- anchors the popover to host view bounds when a popover controller is present;
- removes temporary staging from the completion handler;
- removes staging on preparation/presentation failure.

Regular-width/iPad behavior must still be validated on actual simulator/device UIKit environments.

### iOS version display

About version reads `CFBundleShortVersionString` from the containing app's main bundle. `1.0.0` is only a fallback when the host bundle does not supply the value.

## Cross-platform behavior that must remain identical

The following are shared contracts and should not diverge by platform:

- stopwatch state transitions;
- lap split/total semantics;
- duration formatting;
- session validation;
- session sorting;
- history search/rename/delete/undo behavior;
- JSON portable backup semantics;
- CSV schema/safety;
- import validation/error codes;
- theme/accessibility preference semantics;
- navigation destinations;
- About identity/contact content;
- no telemetry/account/network requirement in product code.

## Platform behavior that may intentionally differ

Differences are acceptable when dictated by platform correctness/security conventions:

- monotonic clock API;
- restart recovery policy;
- local storage adapter;
- direct export destination UI/location;
- share mechanism;
- mini-window/keyboard support;
- package/signing process;
- platform backup behavior;
- version metadata source.

Avoid forcing a lowest-common-denominator abstraction when it would make one platform less correct or less native.

## Adding a platform capability

For a new platform-specific feature:

1. Decide whether any rule belongs in shared domain/data instead of the platform.
2. Define a small shared interface/capability hook if common UI needs it.
3. Keep platform APIs inside the platform source set/module.
4. Map platform failures to stable typed results.
5. Preserve coroutine cancellation.
6. Add platform-independent tests where logic can be extracted.
7. Add platform-specific tests where APIs require the target runtime.
8. Update privacy/security docs if data leaves app-private storage or a new system service is invoked.
9. Add manual verification cases to `testing.md`.
10. Update `repository-reference.md` for new files.

## Related documentation

- [`architecture.md`](architecture.md)
- [`state-and-recovery.md`](state-and-recovery.md)
- [`data-model-and-storage.md`](data-model-and-storage.md)
- [`ios.md`](ios.md)
- [`testing.md`](testing.md)
- [`release.md`](release.md)
- [`../PRIVACY.md`](../PRIVACY.md)
