# Architecture

TempoTrack is a modular monolith with platform entry points around a shared Kotlin module.

## Modules

### `shared`

Contains:

- stopwatch domain model and deterministic timing engine;
- storage/repository contracts;
- JSON persistence and CSV/JSON export encoding;
- shared Compose Multiplatform UI;
- design tokens and an injectable English UI string catalog;
- unit and repository-integration tests.

The shared module is a KMP library with an Android-KMP target and Desktop JVM target.

### `androidApp`

Contains:

- `ComponentActivity` entry point;
- `SystemClock.elapsedRealtimeNanos()` monotonic clock;
- application-private file storage;
- local export implementation;
- Android resources and backup rules.

### `desktopApp`

Contains:

- Compose Desktop entry point;
- `System.nanoTime()` monotonic clock;
- atomic local file storage;
- local file export;
- keyboard shortcuts;
- optional floating mini-window integration.

## Dependency direction

Platform app → shared UI/data/domain.

The domain package does not depend on UI or platform APIs.

## Timing invariant

Wall time is never used to calculate elapsed duration. A `MonotonicClock` is injected into `StopwatchEngine`. Wall time is used only for saved-session metadata/IDs.

Repeated display snapshots reuse the same immutable lap list until a lap mutation occurs. This avoids copying the complete lap history on every 16 ms timer refresh while keeping snapshots immutable to callers.

## Persistence

JSON files are deliberately simple for a local stopwatch. Writes use temporary files where platform adapters can provide them. Serialization ignores unknown fields so additive schema evolution remains possible.

## State recovery

The engine checkpoint stores status, accumulated time, the current monotonic start reading, and laps. On a same-boot process restart it can resume from the monotonic reference. A stale monotonic reference after reboot is restored as paused so it cannot produce a negative or permanently stalled running duration.

## UI and adaptive layout

`TempoTrackTheme` owns the product color system, component shapes, spacing tokens, and refresh cadence tokens. The navigation shell changes from bottom navigation to a navigation rail at a wide breakpoint, so Android tablets and Desktop layouts can use the same adaptive shared UI.

## Internationalization readiness

Visible shared UI copy is supplied through `TempoTrackStrings`. `TempoTrackDependencies` injects the catalog, and English ships as `EnglishTempoTrackStrings`. Future locales can supply another catalog without changing stopwatch domain logic, repositories, or screen behavior.

Brand names, file names, MIME types, URLs, and protocol identifiers remain code constants because they are not translatable UI sentences.

## iOS readiness

The shared domain/data/UI code avoids Android-specific APIs. Adding iOS requires an iOS application entry point plus platform implementations for the storage/export/clock interfaces. That entry point is intentionally not claimed as shipped in v1.0.
