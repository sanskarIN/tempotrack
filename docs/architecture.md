# Architecture

TempoTrack is a modular monolith with platform entry points around a shared Kotlin Multiplatform module.

## Modules

### `shared`

Contains:

- stopwatch domain model and deterministic monotonic timing engine;
- session and active-checkpoint validation/recovery policies;
- storage/repository contracts;
- versioned JSON persistence codecs and legacy migration;
- CSV/JSON export encoding and validated JSON restore parsing;
- shared Compose Multiplatform UI;
- shared Compose resources and design tokens;
- Android library, Desktop JVM, and Kotlin/Native iOS targets;
- common and iOS-target tests.

### `androidApp`

Contains:

- `ComponentActivity` entry point;
- `SystemClock.elapsedRealtimeNanos()` monotonic clock;
- application-private atomic file replacement;
- MediaStore/file export implementation;
- restricted `FileProvider` system sharing;
- Android resources, splash treatment, and backup rules.

### `desktopApp`

Contains:

- Compose Desktop entry point;
- `System.nanoTime()` monotonic clock;
- atomic local file storage;
- native save-file chooser export;
- keyboard shortcuts and shortcut help capability;
- optional floating mini-window integration;
- process-restart-safe checkpoint recovery with a five-second running checkpoint heartbeat.

### iOS target

`shared/src/iosMain` contains:

- `NSProcessInfo.systemUptime` monotonic and `NSDate` wall-clock adapters;
- local `NSUserDefaults` string storage adapter;
- a Compose `UIViewController` entry point;
- native document-picker export through `UIDocumentPickerViewController`;
- native sharing through `UIActivityViewController`;
- isolated temporary export/share staging and cleanup.

## Dependency direction

Platform app/host → shared UI → shared data/domain.

The domain package does not depend on UI or platform APIs. Persistence and platform side effects are hidden behind small interfaces.

## Timing invariant

Wall time is never used to calculate live elapsed duration. `StopwatchEngine` computes elapsed time only from an injected monotonic clock.

Wall time has two metadata roles:

- saved-session creation metadata/IDs;
- active-checkpoint save timestamps used only to validate whether a persisted system-uptime reference still belongs to the same boot.

Android uses `SystemClock.elapsedRealtimeNanos()`, which includes device sleep. Desktop uses `System.nanoTime()`. iOS uses `NSProcessInfo.systemUptime` converted to nanoseconds.

## Persistence

TempoTrack maintains three logical stores:

1. saved sessions;
2. application preferences;
3. the active stopwatch checkpoint.

Each store has a schema-version envelope and migration behavior. Unknown future schema versions fail closed instead of being silently rewritten.

The active-stopwatch envelope is currently schema version 2. Version 2 adds nullable `savedAtEpochMillis` recovery metadata. Version 1 envelopes and original bare checkpoints remain readable and are rewritten to the current envelope after validation.

Session records and active checkpoints are validated before persistence/restore. Store sizes and lap counts are bounded. Platform adapters use atomic replacement where available so an interrupted write is less likely to destroy the last valid file.

## State recovery

A running checkpoint is rebased every time it is persisted: `accumulatedNanos` becomes elapsed-at-save and `startedAtNanos` becomes the monotonic reading at that same save point. This makes the stored accumulated duration a safe lower bound even if the old monotonic origin later becomes unusable.

Android and iOS use system-uptime clocks. On launch, `StopwatchCheckpointRecovery.recoverSystemUptimeCheckpoint` compares elapsed wall time since the checkpoint with elapsed uptime since the checkpoint. If the two deltas reasonably agree, the checkpoint can continue running. If uptime moved backward, wall time moved backward, the deltas disagree beyond the configured tolerance, or a legacy running checkpoint has no wall timestamp, recovery fails safely to PAUSED at the last known elapsed value.

Desktop deliberately does not compare persisted `System.nanoTime()` readings across JVM processes. Its launch policy converts a persisted RUNNING checkpoint to PAUSED at the last safely saved elapsed value. While a Desktop timer is running, the shared app root persists a rebased checkpoint every five seconds, limiting elapsed-time loss after a forced process termination without using wall time for live timing.

Recovery transformations are persisted once during app initialization so future launches begin from the normalized state.

## Import/export boundary

Serialization and platform export are separate concerns:

- `SessionCodec` produces portable JSON/CSV data;
- `SessionImporter` validates user-provided JSON and returns typed error codes;
- platform `Exporter` implementations decide where bytes are written;
- optional platform `ShareService` implementations delegate sharing to operating-system UI;
- shared UI maps typed import/export/share failures to localized, user-safe messages.

Large JSON/CSV serialization and restore parsing run on a background dispatcher. Raw exception text and imported content are not shown to users.

## UI architecture

The shared UI uses:

- compact bottom navigation and an adaptive navigation rail at large widths;
- externalized strings from `composeResources`;
- reusable spacing/sizing/shape/typography tokens;
- theme preference state loaded from the preferences repository;
- platform capability flags for Desktop-only mini-window and keyboard shortcut UI;
- single-flight state around settings writes, session saves, history import, and history export/share serialization.

See [localization.md](localization.md) and [accessibility.md](accessibility.md).

## iOS integration status

The Kotlin/Native framework targets and Compose iOS controller are present. Native History export uses a document picker, native History sharing uses the activity sheet, and temporary staging files are isolated per operation and cleaned after the platform flow. macOS CI is configured to link the simulator framework and run the iOS simulator test target.

A containing Xcode application still owns application signing, App Store packaging, and final device/simulator lifecycle verification.

See [ios.md](ios.md) for host integration guidance.
