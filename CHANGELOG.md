# Changelog

All notable changes to TempoTrack are documented here.

The format follows Keep a Changelog concepts and the project uses semantic versioning.

## [Unreleased]

### Added

- Production-oriented Kotlin/Compose Multiplatform project structure.
- Android and Desktop application entry points.
- iOS Kotlin/Native framework targets and Compose host entry point.
- Native iOS JSON/CSV sharing through `UIActivityViewController` using sanitized temporary files.
- Native iOS JSON/CSV document export through `UIDocumentPickerViewController`, including explicit user-cancellation results.
- iOS simulator tests for temporary export-file isolation, sanitization and cleanup.
- Monotonic stopwatch engine with pause/resume/reset/laps.
- Active-stopwatch schema v2 with elapsed-at-save and wall-save recovery metadata.
- Platform-specific active-checkpoint recovery policies plus Desktop five-second running-checkpoint heartbeats.
- ADR 0005 documenting durable recovery rules for system-uptime clocks and JVM `System.nanoTime()`.
- Fastest, slowest and average lap statistics.
- Named local session history and search.
- Validated session rename workflow.
- JSON/CSV export.
- Android JSON/CSV sharing through the operating-system share sheet using a restricted `FileProvider` cache path.
- Desktop native save-file chooser for selecting export destinations.
- Validated JSON history restore with explicit replacement confirmation.
- Versioned local session storage with automatic migration from the original list format.
- Versioned preferences and active-stopwatch stores with legacy migration.
- Persistent preferences, active timer checkpoints, floating mini-stopwatch visibility, and desktop shortcut enablement.
- Light/dark/system themes and large-control accessibility option.
- Adaptive navigation for larger mobile, tablet, desktop, and iOS layouts.
- Shared Compose string resources so user-facing copy is localization-ready.
- Shared design tokens for spacing, sizing, shapes, typography, and motion guidance.
- Desktop mini-stopwatch integration, keyboard shortcut help, persistent shortcut enable/disable control, and elapsed-time accessibility semantics.
- Branded Android launch/splash treatment, including Android 12+ splash attributes.
- Unit/integration tests for timing, persistence, migration, reboot/restart recovery, validation, import, export, safe export filename normalization, backup/restore journeys, arithmetic boundaries, and persistence limits.
- CI, including macOS verification for the iOS shared framework, security scanning, dependency updates, issue templates and release workflow.
- Repository-local Markdown link validation in CI.
- Tagged release packaging for Android, Desktop, and iOS framework artifacts, with SHA-256 checksums and GitHub Release publishing.
- Environment-backed Android production signing configuration.
- Signed Android tag builds for both APK and Android App Bundle outputs when protected release secrets are configured.
- Strict semantic release-tag validation and workflow concurrency guards.
- Complete project documentation baseline and iOS host integration guide.

### Changed

- Running checkpoints are rebased at every save so persisted accumulated duration is a safe elapsed lower bound independent of the old monotonic origin.
- Android/iOS launch recovery compares elapsed wall time with elapsed system uptime and pauses checkpoints when the references indicate reboot/reset/legacy ambiguity.
- Desktop never compares persisted `System.nanoTime()` references across JVM processes; it restores RUNNING checkpoints as PAUSED at the latest heartbeat value.
- Active-stopwatch schema v1 and original unversioned checkpoints migrate to schema v2 after validation.
- Session import and export failures use stable internal error codes; shared UI maps them to localized user-safe messages.
- Restore size/session limits share the same constants as local persistence so valid self-backups are not rejected by a smaller importer limit.
- Active-stopwatch checkpoint lap limits match the saved-session/live-engine lap limit.
- Active-stopwatch and preference stores reject oversized payloads before decoding and bound encoded writes.
- Live stopwatch lap recording stops at the same maximum accepted by persistence.
- Lap averages use integer quotient/remainder arithmetic instead of floating-point averaging.
- Large JSON/CSV serialization and JSON restore parsing run off the UI dispatcher.
- History export/share launches, restore confirmation, Settings writes, and stopwatch session saves use single-flight state to avoid duplicate operations.
- History delete, undo, and rename actions are serialized with data-portability work, and conflicting controls disable while a history mutation is active.
- Session-history persistence skips rewrites for identical upserts, same-name renames, missing-id deletes, and unchanged full-history replacements.
- Settings controls temporarily disable during persistence and failed writes revert both visible state and platform side effects.
- Stopwatch saved feedback clears when the timer/name changes and session-name input uses the shared validation limit.
- Export cancellation is distinguished from write failures so Desktop and iOS users can cancel native destination pickers without receiving a false error.
- Desktop native export reports a platform-unavailable error if a chooser cannot be created on the current host.
- Android, Desktop and iOS export/share boundaries preserve coroutine cancellation instead of converting cancellation to ordinary write failures.
- Platform export filenames use one bounded shared sanitization policy.
- Android sharing validates its cache directory, preserves coroutine cancellation, and distinguishes file-preparation failure from an unavailable system share activity.
- Android/Desktop private string storage validates parent directories and only falls back from atomic replacement when atomic moves are specifically unsupported.
- iOS export/share staging uses unique temporary operation directories and removes them after completion/cancellation/failure paths.
- Android and Desktop package versions are sourced from Gradle properties and release tags.
- Desktop About version metadata uses the Gradle/tag version passed to the packaged JVM runtime.
- iOS About version metadata uses the containing app's `CFBundleShortVersionString`.
- Android tag releases fail before publishing when production signing secrets are absent instead of uploading unsigned release artifacts.
- Android signing configuration rejects partial secret/environment configuration and validates the keystore path.
- Android signing secrets are scoped to only the workflow steps that need them.
- The release publisher includes AAB files alongside APK/Desktop/iOS artifacts and generated SHA-256 checksums.
- CI and secret scans cancel superseded branch/PR runs while release runs are serialized per tag.
- Unix/Windows bootstrap scripts require exactly Gradle 9.5.0 when the wrapper JAR is absent; wrapper properties pin the official Gradle 9.5.0 distribution checksum.
- Privacy documentation explains Android cache sharing, iOS temporary export/share staging, and operating-system destination selection.

### Fixed

- Recover unsafe running checkpoints as paused after monotonic origin reset/reboot/process replacement instead of calculating from an incompatible clock reference.
- Preserve known lap elapsed time when recovering older running checkpoints.
- Saturate elapsed-time accumulation at `Long.MAX_VALUE` rather than wrapping negative.
- Validate negative/malformed lap totals without unsafe subtraction during session validation.
- Neutralize spreadsheet-formula prefixes in CSV-exported text fields.
- Keep no-lap CSV rows aligned to the same seven-column schema as the header.
- Reject malformed or internally inconsistent session records before persistence or restore.
- Validate active stopwatch checkpoints before persisting or restoring them.
- Fail closed on unsupported future local session, preference, and active-stopwatch schema versions.
- Keep serialization configuration on stable APIs to avoid unnecessary experimental opt-ins.
- Restore the persisted floating mini-stopwatch visibility preference when the app launches on Desktop.
- Persist closing the floating mini-stopwatch so it does not unexpectedly reopen on the next launch.
- Respect the persisted desktop keyboard-shortcut preference before processing Space, L, or R controls.
- Preserve coroutine cancellation in shared UI persistence and platform export/share boundaries.
