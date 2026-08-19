# Architecture

TempoTrack is a modular monolith with platform entry points around a shared Kotlin Multiplatform module.

## Modules

### `shared`

Contains:

- stopwatch domain model and deterministic monotonic timing engine;
- session and active-checkpoint validation;
- storage/repository contracts;
- versioned JSON persistence codecs and legacy migration;
- CSV/JSON export encoding and validated JSON restore parsing;
- shared Compose Multiplatform UI;
- shared Compose resources and design tokens;
- Android library, Desktop JVM, and Kotlin/Native iOS targets;
- common unit tests.

### `androidApp`

Contains:

- `ComponentActivity` entry point;
- `SystemClock.elapsedRealtimeNanos()` monotonic clock;
- application-private atomic file replacement;
- MediaStore/file export implementation;
- Android resources, splash treatment, and backup rules.

### `desktopApp`

Contains:

- Compose Desktop entry point;
- `System.nanoTime()` monotonic clock;
- atomic local file storage;
- local file export;
- keyboard shortcuts and shortcut help capability;
- optional floating mini-window integration.

### iOS target

`shared/src/iosMain` contains:

- monotonic and wall-clock adapters;
- local `NSUserDefaults` string storage adapter;
- a Compose `UIViewController` entry point;
- an explicit host-export capability placeholder until a native document/share-sheet bridge is supplied by the host app.

## Dependency direction

Platform app/host → shared UI → shared data/domain.

The domain package does not depend on UI or platform APIs. Persistence and platform side effects are hidden behind small interfaces.

## Timing invariant

Wall time is never used to calculate elapsed duration. A monotonic clock is injected into `StopwatchEngine`. Wall time is used only for saved-session metadata/IDs.

Android uses `SystemClock.elapsedRealtimeNanos()`, which includes device sleep. Desktop uses `System.nanoTime()`. iOS uses `NSProcessInfo.systemUptime` converted to nanoseconds.

## Persistence

TempoTrack currently maintains three logical stores:

1. saved sessions;
2. application preferences;
3. the active stopwatch checkpoint.

Each store has a schema-version envelope and can migrate the original unversioned JSON format on read. Unknown future schema versions fail closed instead of being silently rewritten.

Session records and active checkpoints are validated before persistence/restore. Platform adapters use atomic replacement where available so an interrupted write is less likely to destroy the last valid file.

## State recovery

The active checkpoint stores status, accumulated time, the current monotonic start reading, and laps. On a same-boot process restart it can resume from the monotonic reference. A stale monotonic reference after reboot is restored as paused so it cannot produce a negative or permanently stalled running duration.

## Import/export boundary

Serialization and platform export are separate concerns:

- `SessionCodec` produces portable JSON/CSV data;
- `SessionImporter` validates user-provided JSON and returns typed error codes;
- platform `Exporter` implementations decide where bytes are written;
- shared UI maps typed import/export failures to localized, user-safe messages.

Raw exception text and imported content are not shown to users.

## UI architecture

The shared UI uses:

- compact bottom navigation and an adaptive navigation rail at large widths;
- externalized strings from `composeResources`;
- reusable spacing/sizing/shape/typography tokens;
- theme preference state loaded from the preferences repository;
- platform capability flags for Desktop-only mini-window and keyboard shortcut UI.

See [localization.md](localization.md) and [accessibility.md](accessibility.md).

## iOS integration status

The Kotlin/Native framework targets and Compose iOS controller are present and macOS CI is configured to link/test the simulator target. A containing Xcode application still owns app-store signing, lifecycle integration, and the native document/share-sheet export bridge.

See [ios.md](ios.md) for host integration guidance.
