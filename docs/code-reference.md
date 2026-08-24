# Source Code Reference

This guide describes the important types, functions, state ownership, and dependency boundaries in TempoTrack. For a path-by-path inventory of every tracked file, see [`repository-reference.md`](repository-reference.md).

## Kotlin namespace note

The runtime namespace is `in.sanskar.tempotrack`. Because `in` is a Kotlin keyword, Kotlin source must escape the first package segment:

```kotlin
package `in`.sanskar.tempotrack.domain
import `in`.sanskar.tempotrack.data.SessionRepository
```

This source spelling still compiles to the ordinary JVM/Android/Kotlin-Native package `in.sanskar.tempotrack...`.

## Domain layer

The domain layer has no Compose, Android, Swing, UIKit, filesystem, or JSON repository dependency. It contains the correctness-critical stopwatch rules.

### `Clock.kt`

`MonotonicClock`

- Functional interface with `nowNanos(): Long`.
- Injected into `StopwatchEngine`.
- Represents an arbitrary monotonic origin; callers must not assume it starts at zero or maps to wall time.
- Android supplies `SystemClock.elapsedRealtimeNanos()`.
- Desktop supplies `System.nanoTime()`.
- iOS supplies `NSProcessInfo.systemUptime` converted to nanoseconds.

`WallClock`

- Functional interface with `nowEpochMillis(): Long`.
- Used for session creation metadata/IDs and checkpoint save metadata.
- Never used as the live elapsed-duration source.

### `Models.kt`

`StopwatchStatus`

- `IDLE` — no active elapsed time; checkpoint carries no laps/start timestamp.
- `RUNNING` — elapsed time equals accumulated baseline plus monotonic delta since `startedAtNanos`.
- `PAUSED` — elapsed time is fully accumulated; no active start timestamp.

`Lap`

- `index`: one-based sequential lap number.
- `splitNanos`: duration since previous lap/start.
- `totalNanos`: cumulative elapsed time when lap was captured.

`StopwatchSnapshot`

- Read model exposed by the engine/UI.
- Carries status, current elapsed nanoseconds, immutable lap list.
- `elapsedMillis` is derived by integer division.

`StopwatchCheckpoint`

- Durable active-timer representation.
- `accumulatedNanos` is the safe elapsed baseline.
- `startedAtNanos` exists only for `RUNNING`.
- `savedAtEpochMillis` is nullable recovery metadata introduced by active schema v2.
- `laps` preserve known lap history.

`StopwatchSession`

- Durable completed/named history entry.
- Includes generated ID, display name, creation wall timestamp, total duration, and laps.

`LapStatistics`

- Computes fastest/slowest laps with `minByOrNull`/`maxByOrNull`.
- Computes rounded average using quotient/remainder arithmetic.
- Avoids summing all durations into one potentially overflowing `Long` and avoids `Double` precision loss.

`NANOS_PER_MILLISECOND` / `NANOS_PER_SECOND`

- Shared conversion constants used by engine, formatting, recovery, and tests.

### `StopwatchEngine.kt`

`StopwatchEngine(clock, checkpoint, wallClock)` owns mutable stopwatch state inside one instance.

Public operations:

- `start()` — only changes `IDLE → RUNNING`; clears prior accumulated/lap state and captures monotonic start.
- `pause()` — only changes `RUNNING → PAUSED`; snapshots elapsed into `accumulatedNanos` and clears start timestamp.
- `resume()` — only changes `PAUSED → RUNNING`; captures a new monotonic start without altering accumulated elapsed.
- `reset()` — returns to canonical `IDLE` state and clears laps.
- `lap()` — while running and below the shared lap ceiling, records cumulative elapsed and split from previous lap total.
- `snapshot()` — calculates current visible elapsed without mutating durable state.
- `checkpoint()` — returns a persistence-safe checkpoint. A running checkpoint is **rebased**: current elapsed becomes `accumulatedNanos` and the same current monotonic reading becomes the new `startedAtNanos`.

Important internal behavior:

- Constructor protects against a persisted running start timestamp that is later than the current monotonic reading; this normalizes to paused and preserves at least the latest known lap total.
- `elapsedAt` clamps negative deltas to zero.
- Accumulated + active duration uses saturating addition and never wraps negative.
- Lap recording uses `SessionValidation.MAX_LAPS_PER_SESSION`, keeping live state aligned with durable-session limits.

### `StopwatchCheckpointValidation.kt`

`StopwatchCheckpointValidation.validate(checkpoint)` returns distinct human-readable internal validation errors; `isValid` reduces this to a Boolean.

Rules include:

- accumulated duration must be nonnegative;
- wall save timestamp, when present, must be nonnegative;
- lap count is bounded by the session lap limit;
- idle state has zero time, no start timestamp, and no laps;
- running state requires a start timestamp;
- paused state cannot have a start timestamp;
- lap indices are sequential and split/total values are nonnegative;
- cumulative totals equal the sum of splits without overflowing;
- non-running lap total cannot exceed accumulated duration.

A running legacy checkpoint is allowed to have a lap total newer than its old accumulated baseline because older persistence stored only the pre-run baseline.

### `StopwatchCheckpointRecovery.kt`

`pauseRunningAtLastSavedElapsed(checkpoint)`

- Leaves non-running checkpoints unchanged.
- Converts running to paused.
- Preserves the maximum of `accumulatedNanos` and the last lap total.
- Clears `startedAtNanos`.

`recoverSystemUptimeCheckpoint(...)`

- Used by Android/iOS clocks whose monotonic value is system uptime.
- Requires nonnegative recovery tolerance.
- Fails safely to paused when save/start metadata is missing/negative, uptime moved backward, wall time moved backward, or uptime/wall elapsed deltas differ beyond tolerance.
- Leaves a running checkpoint unchanged only when both elapsed references remain mutually plausible.

### `SessionValidation.kt`

`SessionValidation` centralizes durable history constraints:

- max name length: 80;
- max ID length: 160;
- max laps per session: 100,000;
- nonblank bounded ID/name;
- nonnegative creation time and duration;
- sequential lap indices;
- nonnegative split/total values;
- monotonic totals;
- total not beyond session duration;
- split must equal change in cumulative total.

`requireValid` converts validation errors into an `IllegalArgumentException` for internal write boundaries.

### `SessionIdGenerator.kt`

Generates:

`<createdAtEpochMillis>-<durationNanos>-<16-character unsigned random hex>`

The result is bounded to `MAX_SESSION_ID_LENGTH`. The random suffix prevents collisions for sessions saved at the same timestamp/duration.

### `DurationFormatter.kt`

Formats nonnegative duration as `HH:mm:ss.SSS` by default, optionally without milliseconds. Negative input is clamped to zero. Hours are intentionally not capped at 23.

## Data layer

The data package defines persistence/portability contracts and implementations that remain platform-neutral by depending on `StringStorage`, `Exporter`, and `ShareService` abstractions.

### `Storage.kt`

`StringStorage`

- `read(): String?`
- `write(content)`
- `clear()`

Platform implementations provide durability/location semantics.

`Exporter`

- Accepts suggested filename, MIME type, UTF-8 logical content.
- Returns `ExportResult.Success(destination)` or typed `ExportResult.Failure`.

`ExportError`

- `WRITE_FAILED`
- `PLATFORM_EXPORT_UNAVAILABLE`
- `USER_CANCELLED`

`ShareService`

- Stages/shares content through platform UI.
- Returns `ShareResult.Started` or typed failure.

`ShareError`

- `PREPARE_FAILED`
- `PLATFORM_SHARE_UNAVAILABLE`

### `ExportFileName.kt`

`ExportFileName.sanitize`:

1. trims surrounding whitespace;
2. replaces characters outside `[A-Za-z0-9._-]` with `_`;
3. trims leading/trailing `.` and `_`;
4. caps length at 120 characters;
5. falls back to `tempotrack-export` if nothing safe remains.

This is a filename policy, not a filesystem authorization mechanism. Platform adapters still choose an allowed directory/destination.

### `SessionCodec.kt`

`toJson(sessions)`

- Serializes the portable backup format as a plain list of `StopwatchSession` objects.
- Pretty prints and omits explicit nulls.
- Does **not** expose the internal local-store envelope.

`toCsv(sessions)`

Seven columns:

1. `session_id`
2. `session_name`
3. `created_at_epoch_ms`
4. `duration`
5. `lap_number`
6. `split`
7. `total`

Every data row has the same seven-column structure, including sessions without laps.

Text fields are quoted/escaped. Leading spreadsheet formula characters (`=`, `+`, `-`, `@` after trim) are prefixed with an apostrophe before CSV quoting.

### `SessionImport.kt`

`SessionImporter.fromJson(content)` is the restore validation boundary.

It checks, in order:

- blank input;
- character-size limit;
- JSON decoding;
- session-count limit;
- duplicate IDs;
- per-session validation.

On success, sessions are newest-first. Failures return a stable `SessionImportError` rather than raw exception or input content. For an invalid session, only its one-based position is exposed to UI.

### `SessionStoreCodec.kt`

Internal saved-history envelope:

```text
schemaVersion: 1
sessions: [...]
```

Behavior:

- current envelope decodes normally;
- original bare session-list JSON is accepted as legacy and marked for migration;
- unknown/future envelope versions return failure instead of being rewritten.

### `SessionRepository.kt`

`SessionRepository` operations:

- `all()`
- `upsert(session)`
- `rename(id, newName)`
- `delete(id)`
- `replaceAll(sessions)`

`JsonSessionRepository` serializes all operations with a coroutine `Mutex`.

Read path:

1. read string;
2. reject oversized payload;
3. decode current/legacy store;
4. reject too many sessions;
5. reject duplicate IDs;
6. validate every session;
7. normalize newest-first;
8. migrate legacy storage only after successful validation.

Write path:

- validates models before encoding;
- bounds total encoded characters;
- relies on platform `StringStorage` for durable write mechanics.

No-op optimizations:

- identical upsert does not rewrite;
- same normalized rename returns success without rewrite;
- missing-ID delete does not rewrite;
- identical normalized `replaceAll` does not rewrite.

### `PreferencesRepository.kt` and `PreferencesStoreCodec.kt`

`AppPreferences` currently includes:

- `theme`: system/light/dark;
- `largeControls`;
- `reducedMotion`;
- `onboardingCompleted`;
- `miniStopwatchVisible`;
- `keyboardShortcutsEnabled`.

Preferences have an internal schema envelope, bounded payload size, and defaults for newly introduced fields. Corrupt/oversized/future data loads as safe defaults rather than producing UI-visible raw persistence errors. Legacy bare `AppPreferences` JSON is migrated after successful decode.

### `ActiveStopwatchRepository.kt` and `ActiveStopwatchStoreCodec.kt`

`JsonActiveStopwatchRepository`:

- serializes load/save/clear with a `Mutex`;
- rejects oversized stored payloads before decode;
- validates checkpoint before save and after decode;
- rewrites legacy/current migration only after validation.

Current active schema is **v2**. v2 includes `savedAtEpochMillis` through the checkpoint model. The decoder accepts:

- v2 current envelope — no migration;
- v1 envelope — migrate;
- original bare checkpoint — migrate;
- future/unknown versions — fail closed.

## Shared utility

### `SuspendResult.kt`

`suspendResult { ... }` behaves like a cancellation-safe suspend equivalent of `runCatching`:

- successful value → `Result.success`;
- ordinary `Throwable` → `Result.failure`;
- `CancellationException` → rethrow.

This is important around UI persistence/export/share operations so structured-concurrency cancellation is not misreported as a user-visible storage failure.

## Shared Compose application

### `TempoTrackDependencies.kt`

This data class is the platform injection surface. Required dependencies include:

- monotonic/wall clocks;
- session/preferences/active repositories;
- exporter;
- platform name/version.

Optional capability hooks include:

- share service;
- checkpoint recovery function;
- running checkpoint heartbeat interval;
- mini stopwatch supported + visibility setter;
- keyboard shortcuts supported + enabled setter.

The common UI therefore does not import Android/Swing/UIKit APIs.

### `TempoTrackApp.kt`

Initialization sequence:

1. load preferences with safe default fallback;
2. apply Desktop capability side effects for mini-window/shortcuts;
3. load active checkpoint;
4. apply platform recovery function;
5. persist normalized recovered checkpoint when recovery changed it;
6. create one `StopwatchEngine` with platform clocks;
7. expose engine to optional host callback;
8. mark UI loaded.

Heartbeat effect:

- only runs when platform supplied a positive interval;
- periodically persists only while engine is running;
- Desktop supplies five seconds; Android/iOS currently rely on meaningful action checkpoints rather than this process-local heartbeat.

Navigation:

- onboarding owns the entire UI until completion is persisted;
- compact layouts use bottom navigation;
- wide layouts use navigation rail;
- destinations: Stopwatch, History, Settings, About.

### `StopwatchScreen.kt`

State:

- snapshot mirrored from engine;
- optional session name;
- save-feedback text;
- session-save single-flight flag;
- lap sort mode.

Refresh loop:

- ~16 ms while running;
- slower while not running;
- display refresh never changes timing truth because snapshots derive from monotonic clock.

Actions persist active checkpoint after start/pause/resume/lap and clear it after reset.

Saving a history session captures a final snapshot and wall timestamp, generates an ID, and upserts once. The save button is disabled while the write is active.

### `HistoryScreen.kt`

State separates:

- history read/search;
- `dataOperationInProgress` for export/share preparation;
- `importing` for restore parse/replace;
- `historyMutationInProgress` for delete/undo/rename;
- rename/import dialogs and validation feedback.

Concurrency rules:

- only one export/share preparation at a time;
- restore cannot overlap data operations/history mutations;
- delete/undo/rename are single-flight;
- conflicting controls are disabled while an operation owns the boundary;
- restore parsing and export serialization run on `Dispatchers.Default` rather than the UI dispatcher.

User-facing failures are resource strings derived from typed error enums, not raw exception messages.

### `SettingsScreen.kt`

Preference updates are optimistic but transactional from the user's perspective:

1. remember previous preferences;
2. update visible state and platform side effect;
3. persist once;
4. on failure restore previous UI preferences;
5. call rollback hook for platform side effect;
6. show localized failure message.

Controls disable while the preference write is active.

### `OnboardingScreen.kt`

The continue action first persists `onboardingCompleted = true`; it advances only when persistence succeeds. A failed write leaves onboarding visible and displays a localized error.

### `MiniStopwatch.kt`

Compact Desktop window using the same engine and active repository. It provides start/pause/resume/lap/reset and exposes formatted elapsed time through accessibility semantics.

### `AboutScreen.kt`

Shows product/platform/version metadata and opens GitHub, funding, and email links through `openUriSafely`. Link launch failure becomes UI feedback rather than an exception crash.

### Theme/tokens/URI helpers

- `AppTheme.kt` maps preference to Material light/dark scheme.
- `DesignTokens.kt` centralizes spacing, control dimensions, shapes, typography, and motion constants.
- `ExternalUri.kt` wraps `UriHandler.openUri` in a Boolean success result.

## Android platform code

### `MainActivity.kt`

Constructs:

- three private file-backed stores under `filesDir`;
- Android monotonic/wall clocks;
- JSON repositories;
- `AndroidExporter` and `AndroidShareService`;
- system-uptime recovery function.

The Compose content receives only `TempoTrackDependencies`.

### `AndroidStringStorage.kt`

Write algorithm:

1. require parent;
2. create parent if needed;
3. verify parent is directory;
4. write UTF-8 sibling `.tmp` file;
5. move with `REPLACE_EXISTING + ATOMIC_MOVE`;
6. fall back only on `AtomicMoveNotSupportedException`.

`clear()` deletes both primary and stale `.tmp` sidecar.

### `AndroidExporter.kt`

Android 10+:

1. insert Downloads MediaStore row with `IS_PENDING=1` and `Downloads/TempoTrack` relative path;
2. write UTF-8 content;
3. set `IS_PENDING=0`;
4. require exactly one updated row;
5. delete the MediaStore item if writing/finalization fails.

Pre-Android-10:

- uses app-specific Documents/TempoTrack;
- verifies directory;
- atomically reserves a collision-safe filename with `AndroidStagingFiles`;
- writes content and deletes reservation if write fails.

### `AndroidStagingFiles.kt`

`createUniqueShareFile` uses `File.createTempFile`, preserves an extension when possible, bounds the human-readable prefix, and pads very short prefixes so Java's minimum-prefix rule is satisfied.

`reserveUniqueExportTarget` tries the requested name first, then `name (1).ext`, `name (2).ext`, etc., up to a bounded attempt count. `createNewFile()` makes reservation race-resistant.

### `AndroidShareService.kt`

Preparation:

- validate/create `cache/shared-exports`;
- generate unique staged file;
- write UTF-8 content;
- obtain `content://` URI from non-exported `FileProvider`;
- attach URI to both `ClipData` and `EXTRA_STREAM`;
- grant read permission;
- create chooser.

Preparation/platform-launch failure deletes the staged file when safe. If coroutine cancellation races a possibly launched chooser, the staged cache file is retained because a recipient may already hold the granted URI.

## Desktop platform code

### `Main.kt`

Storage directory: `~/.tempotrack`.

Desktop uses process-local `System.nanoTime()`. A persisted running timer is therefore recovered as paused at its last safely persisted elapsed value on application restart.

While running, shared app heartbeat persistence runs every five seconds. This bounds elapsed loss after forced JVM termination without comparing `System.nanoTime()` origins across processes.

Keyboard bindings when enabled:

- Space — start/pause/resume;
- L — lap;
- R — reset.

The optional mini window shares the same engine. Closing it persists the hidden preference.

### `JvmStringStorage.kt`

Same private temp-file/atomic-replace concept as Android, implemented with `java.nio.file.Path`.

### `DesktopExporter.kt`

- Creates Swing `JFileChooser` on EDT (`invokeAndWait` when necessary).
- Suggests sanitized filename and type-specific extension filter.
- `null` selected target means user cancellation.
- Writes UTF-8 on IO dispatcher.
- Distinguishes chooser unavailability from write failure.

## iOS Kotlin/Native code

### `IosPlatformAdapters.kt`

- `IosStringStorage` uses `NSUserDefaults` keyed strings.
- `iosMonotonicClock` converts `NSProcessInfo.processInfo.systemUptime` seconds to nanoseconds.
- `iosWallClock` converts `NSDate().timeIntervalSince1970` to epoch milliseconds.

### `IosTemporaryExportFile.kt`

Creates a unique directory under `NSTemporaryDirectory()`, sanitizes filename, writes NSString UTF-8 atomically, and returns both URL and operation directory path for deterministic cleanup.

### `IosShareService.kt`

- Allows one active activity controller.
- Presents `UIActivityViewController` with staged file URL.
- Sets popover source view/rect for regular-width presentation.
- Cleans staged directory on completion/dismissal/failure.
- Retains active controller reference while operation is alive.

### `IosDocumentExporter.kt`

- Serializes export picker operations with a coroutine `Mutex`.
- Creates staging off main dispatcher.
- Presents `UIDocumentPickerViewController(forExportingURLs, asCopy=true)` on main dispatcher.
- Retains delegate strongly for the operation.
- Maps picker selection/cancel/presentation failure to typed `ExportResult`.
- Dismisses picker on coroutine cancellation.
- Cleans staging in `NonCancellable + Dispatchers.Default` finally path.

### `MainViewController.kt`

Builds iOS dependencies and returns the Compose `UIViewController`. The platform version shown in About comes from `CFBundleShortVersionString` with `1.0.0` fallback.

## Test architecture

Common tests deliberately use fake/in-memory storage and injected clocks so most correctness rules run without platform APIs.

Key patterns:

- `ConcurrentWriteDetectingStorage` inserts a suspension point and throws on overlapping mutation, proving repository mutex serialization.
- journey tests combine engine, repository, codec, importer, and restored repository rather than only isolated functions.
- boundary tests use `Long.MAX_VALUE`, negative totals/origins, malformed JSON, future schema values, duplicate IDs, and storage-size limits.
- Android staging helpers are extracted away from framework APIs so collision/temporary-file behavior can run as JVM unit tests.
- iOS staging tests live in `iosTest` because Foundation filesystem behavior requires the Kotlin/Native simulator target.

See [`testing.md`](testing.md) for exact commands and the manual platform matrix.

## Repository Python tools

### `tools/check_release_metadata.py`

Validates the release contract without invoking Gradle:

- parses `appVersion` and `appVersionCode` from `gradle.properties`;
- requires canonical `MAJOR.MINOR.PATCH` source syntax;
- derives and range-checks Android `versionCode` as `MAJOR * 10000 + MINOR * 100 + PATCH`;
- requires source versionCode to equal the derived value;
- checks README current-release marker;
- checks for a dated `CHANGELOG.md` heading for the same version;
- checks for the same release section in `ROADMAP.md`;
- with `--tag`, requires canonical `vMAJOR.MINOR.PATCH` and exact source/tag equality.

Main CI runs the source form; release validation runs the tagged form on the checked-out tag.

### `tools/check_gradle_version_alignment.py`

Treats the Gradle version in `gradle/wrapper/gradle-wrapper.properties` as authoritative and validates:

- distribution URL version;
- pinned 64-character SHA-256;
- positive wrapper retry/backoff settings;
- Unix and Windows fallback launcher versions;
- every `gradle-version` entry in workflows using `gradle/actions/setup-gradle`.

This detects partial Gradle upgrades but does not replace Kotlin/AGP/Compose compatibility testing.

### `tools/check_kotlin_package_keywords.py`

Walks Kotlin source under `shared/src`, `androidApp/src`, and `desktopApp/src`, failing if a left-trimmed line begins with unescaped:

- `package in.sanskar.`
- `import in.sanskar.`

This catches a syntax error before a long Gradle platform build.

### `tools/check_repository_reference.py`

Uses `git ls-files` as the source of truth for tracked paths and fails when any tracked file is absent from `docs/repository-reference.md` as an exact backticked path. It must run from a real Git checkout.

### `tools/check_markdown_links.py`

Parses Markdown links, skips external URLs, normalizes local destinations, and fails when a repository-local path does not exist. This keeps documentation navigation deterministic from a clean checkout.
