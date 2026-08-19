# Architecture

TempoTrack is a modular monolith with platform entry points around a shared Kotlin module.

## Modules

### `shared`

Contains:

- stopwatch domain model and deterministic timing engine;
- storage/repository contracts;
- JSON persistence and CSV/JSON export encoding;
- shared Compose Multiplatform UI;
- unit tests.

The shared module is a KMP library with Android-KMP and Desktop JVM targets.

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
- optional floating mini-window integration.

## Dependency direction

Platform app → shared UI/data/domain.

The domain package does not depend on UI or platform APIs.

## Timing invariant

Wall time is never used to calculate elapsed duration. A monotonic clock is injected into `StopwatchEngine`. Wall time is used only for session metadata/IDs.

## Persistence

JSON files are deliberately simple for a local stopwatch. Writes use temporary files where platform adapters can provide them. Serialization ignores unknown fields so additive schema evolution remains possible.

## State recovery

The engine checkpoint stores status, accumulated time, the current monotonic start reading, and laps. On a same-boot process restart it can resume from the monotonic reference. A stale monotonic reference after reboot is restored as paused so it cannot produce a negative or permanently stalled running duration.

## iOS readiness

The shared domain/data/UI code avoids Android-specific APIs. Adding iOS requires an iOS application entry point plus platform implementations for the storage/export/clock interfaces. That entry point is intentionally not claimed as shipped in v1.0.
