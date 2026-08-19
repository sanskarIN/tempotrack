# Architecture

TempoTrack is a modular monolith with platform entry points around a shared Kotlin Multiplatform module. The architecture intentionally keeps stopwatch correctness and persistence semantics in shared code while allowing native platform storage, timing, export/share, recovery, packaging, and host capabilities to differ when the operating systems require it.

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
- Android 10+ MediaStore export and older-Android app Documents export;
- pure JVM staging helpers for unique/collision-safe files;
- restricted `FileProvider` system sharing;
- Android resources, splash treatment, and backup rules;
- local JVM staging tests.

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

Primary dependency direction:

```text
Android/Desktop/iOS host
        |
        v
TempoTrackDependencies / platform contracts
        |
        v
shared Compose UI
        |
        v
shared repositories / codecs
        |
        v
shared domain model + stopwatch engine
```

The domain package does not depend on UI, persistence frameworks, filesystems, Android APIs, Swing, Foundation, or UIKit.

Platform side effects are hidden behind small interfaces/capability hooks:

- `MonotonicClock`;
- `WallClock`;
- `StringStorage`;
- `Exporter`;
- optional `ShareService`;
- checkpoint-recovery callback;
- optional running-heartbeat interval;
- optional Desktop mini-window/shortcut capability setters.

This makes the common UI reusable without importing platform APIs and keeps correctness rules independently testable.

## Kotlin namespace source rule

The compiled/runtime namespace is `in.sanskar.tempotrack...`, but `in` is a Kotlin keyword. Kotlin source must use escaped package/import syntax:

```kotlin
package `in`.sanskar.tempotrack.domain
import `in`.sanskar.tempotrack.data.SessionRepository
```

`tools/check_kotlin_package_keywords.py` and CI enforce this syntax without changing the runtime package or Android application ID.

## Timing invariant

Wall time is never used to calculate live elapsed duration. `StopwatchEngine` computes elapsed time only from an injected monotonic clock.

Wall time has two metadata roles:

- saved-session creation metadata/IDs;
- active-checkpoint save timestamps used only to validate whether a persisted system-uptime reference still belongs to the same boot.

Android uses `SystemClock.elapsedRealtimeNanos()`, which includes device sleep. Desktop uses `System.nanoTime()`. iOS uses `NSProcessInfo.systemUptime` converted to nanoseconds.

For state-machine and recovery arithmetic see [`state-and-recovery.md`](state-and-recovery.md).

## Persistence architecture

TempoTrack maintains three logical stores:

1. saved sessions;
2. application preferences;
3. the active stopwatch checkpoint.

Shared repositories own:

- serialization/deserialization;
- schema interpretation and migration;
- model validation;
- payload/count/lap limits;
- sorting/normalization;
- coroutine mutex serialization;
- no-op write avoidance where semantics are unchanged.

Platform `StringStorage` implementations own only the physical string persistence boundary.

### Session history

Saved history is durable user data. Unsupported/corrupt/invalid history fails closed rather than silently becoming a successful empty/partial store.

Current internal session envelope is schema version 1; original bare session-list JSON remains a recognized legacy form.

### Preferences

Preferences are reconstructable configuration. Invalid/oversized/unsupported preference storage falls back to safe defaults; legacy bare preferences can migrate to the current envelope.

### Active checkpoint

The active-stopwatch envelope is schema version 2. Version 2 adds nullable `savedAtEpochMillis` recovery metadata. Version 1 envelopes and original bare checkpoints remain readable and are rewritten to the current envelope after validation.

Invalid/oversized/unsupported active state is treated as transient and safely discarded rather than used for an unsafe elapsed calculation.

See [`data-model-and-storage.md`](data-model-and-storage.md) for exact schemas, limits, migration policies, locations, and portability formats.

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

Portable JSON is intentionally a plain session list rather than the internal `SessionStoreEnvelope`, allowing internal persistence to evolve independently of user backup semantics.

Large JSON/CSV serialization and restore parsing run on a background dispatcher. Raw exception text and imported content are not shown to users.

## Android file-portability boundary

Android has two distinct data-portability paths:

### Explicit export

- Android 10+ uses a pending MediaStore Downloads item and reports success only after finalization succeeds.
- Older Android writes to app-specific Documents/TempoTrack and atomically reserves a collision-safe filename so an existing backup is not overwritten.

### Share

- creates a unique per-operation cache file;
- exposes only `cache/shared-exports/` through a non-exported `FileProvider`;
- grants temporary read permission;
- supplies the `content://` URI through both `EXTRA_STREAM` and `ClipData`.

The unique file prevents a second share from changing data behind an earlier recipient's already granted URI.

## iOS file-portability boundary

Both iOS export and share stage a sanitized UTF-8 file inside a unique operation directory below `NSTemporaryDirectory()`.

- document export presents `UIDocumentPickerViewController` and serializes picker operations;
- sharing presents `UIActivityViewController` and retains one active controller;
- native completion/cancellation/failure paths clean staging;
- document cleanup uses a non-cancellable finalization path.

UIKit delegate/controller lifetimes and iPad popover presentation remain platform-runtime concerns requiring macOS/Xcode verification.

## UI architecture

The shared UI uses:

- compact bottom navigation and an adaptive navigation rail at large widths;
- externalized strings from `composeResources`;
- reusable spacing/sizing/shape/typography tokens;
- theme preference state loaded from the preferences repository;
- platform capability flags for Desktop-only mini-window and keyboard shortcut UI;
- cancellation-safe suspend result handling;
- single-flight state around settings writes, session saves, history import/export/share, and history mutations.

### History concurrency model

Repository mutexes protect storage correctness, while UI state protects interaction semantics.

History intentionally prevents overlap between:

- export/share preparation;
- restore parsing/replacement;
- delete/undo/rename mutations.

Conflicting controls disable while an operation owns the boundary. This avoids duplicated/destructive queued actions even though repository writes are also mutex-protected.

### Settings transaction model

Settings use optimistic UI followed by one persistence operation. If persistence fails, both visible preferences and any platform side effect (such as Desktop mini-window/shortcut enabled state) roll back to the previous value.

### Stopwatch save model

A saved session captures a final engine snapshot plus wall creation timestamp. Session saves are single-flight and stale “saved” feedback clears when timer state or session name changes.

See [`code-reference.md`](code-reference.md), [`localization.md`](localization.md), and [`accessibility.md`](accessibility.md).

## Build and verification architecture

Build logic is centralized across:

- root Gradle plugin/task configuration;
- version catalog;
- module build scripts;
- exact Gradle 9.5.0 bootstrap contract;
- GitHub CI/security/release workflows.

Repository-local Python guards run independently of Gradle:

```bash
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

The tracked-file reference guard uses `git ls-files`, making exhaustive repository documentation an executable CI contract.

See [`build-and-ci.md`](build-and-ci.md) and [`testing.md`](testing.md).

## iOS integration status

The Kotlin/Native framework targets and Compose iOS controller are present. Native History export uses a document picker, native History sharing uses the activity sheet, and temporary staging files are isolated per operation and cleaned after the platform flow. macOS CI is configured to link the simulator framework and run the iOS simulator test target.

A containing Xcode application still owns application signing, App Store packaging, and final device/simulator lifecycle verification.

See [`ios.md`](ios.md) for host integration guidance.

## Architecture documentation map

- [`code-reference.md`](code-reference.md) — per-type/source responsibilities.
- [`state-and-recovery.md`](state-and-recovery.md) — timing/recovery state machine and failure matrix.
- [`data-model-and-storage.md`](data-model-and-storage.md) — data formats, limits, migrations, locations, portability.
- [`platforms.md`](platforms.md) — platform implementation matrix.
- [`security-model.md`](security-model.md) — trust boundaries and security controls.
- [`maintainer-guide.md`](maintainer-guide.md) — safe change recipes.
- [`repository-reference.md`](repository-reference.md) — exhaustive tracked-file ownership.
- [`adr/0001-monotonic-time.md`](adr/0001-monotonic-time.md) through [`adr/0005-platform-checkpoint-recovery.md`](adr/0005-platform-checkpoint-recovery.md) — durable architecture decisions.
