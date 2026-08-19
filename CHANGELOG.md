# Changelog

All notable changes to TempoTrack are documented here.

The format follows Keep a Changelog concepts and the project uses semantic versioning.

## [Unreleased]

### Added

- Production-oriented Kotlin/Compose Multiplatform project structure.
- Android and Desktop application entry points.
- iOS Kotlin/Native framework targets and Compose host entry point.
- Native iOS JSON/CSV sharing through `UIActivityViewController` using sanitized temporary files.
- Monotonic stopwatch engine with pause/resume/reset/laps.
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
- Desktop mini-stopwatch integration, keyboard shortcut help, and persistent shortcut enable/disable control.
- Branded Android launch/splash treatment, including Android 12+ splash attributes.
- Unit/integration tests for timing, persistence, migration, validation, import, export, safe export filename normalization, backup/restore journeys, and persistence limits.
- CI, including macOS verification for the iOS shared framework, security scanning, dependency updates, issue templates and release workflow.
- Repository-local Markdown link validation in CI.
- Tagged release packaging for Android, Desktop, and iOS framework artifacts, with SHA-256 checksums and GitHub Release publishing.
- Environment-backed Android production signing configuration.
- Signed Android tag builds for both APK and Android App Bundle outputs when protected release secrets are configured.
- Strict semantic release-tag validation and workflow concurrency guards.
- Complete project documentation baseline and iOS host integration guide.

### Changed

- Session import and export failures use stable internal error codes; shared UI maps them to localized user-safe messages.
- Restore size/session limits now share the same constants as local persistence so valid self-backups are not rejected by a smaller importer limit.
- Active-stopwatch checkpoint lap limits now match the saved-session/live-engine lap limit.
- Active-stopwatch and preference stores now reject oversized payloads before decoding and bound encoded writes.
- Live stopwatch lap recording stops at the same maximum accepted by persistence.
- Large JSON/CSV serialization and JSON restore parsing run off the UI dispatcher; duplicate restore submissions are blocked while replacement is in progress.
- Export cancellation is distinguished from write failures so Desktop users can cancel the native save dialog without receiving a false error.
- Desktop native export reports a platform-unavailable error if a chooser cannot be created on the current host.
- Android and Desktop export paths preserve coroutine cancellation instead of converting cancellation to ordinary write failures.
- Android and Desktop export filenames use one bounded shared sanitization policy.
- Android sharing preserves coroutine cancellation and distinguishes file-preparation failure from an unavailable system share activity.
- Android and Desktop package versions are sourced from Gradle properties and release tags.
- Desktop About version metadata uses the Gradle/tag version passed to the packaged JVM runtime.
- iOS About version metadata uses the containing app's `CFBundleShortVersionString`.
- Android private file replacement uses atomic move when supported with safe replacement fallback.
- Android tag releases fail before publishing when production signing secrets are absent instead of uploading unsigned release artifacts.
- Android signing configuration rejects partial secret/environment configuration and validates the keystore path.
- Android signing secrets are scoped to only the workflow steps that need them.
- The release publisher includes AAB files alongside APK/Desktop/iOS artifacts and generated SHA-256 checksums.
- CI cancels superseded branch/PR verification while release runs are serialized per tag.
- Privacy documentation explains Android cache sharing, iOS temporary share files, and operating-system destination selection.

### Fixed

- Recover stale running checkpoints as paused after a monotonic clock reset/reboot.
- Neutralize spreadsheet-formula prefixes in CSV-exported text fields.
- Keep no-lap CSV rows aligned to the same seven-column schema as the header.
- Reject malformed or internally inconsistent session records before persistence or restore.
- Validate active stopwatch checkpoints before persisting or restoring them.
- Fail closed on unsupported future local session, preference, and active-stopwatch schema versions.
- Keep serialization configuration on stable APIs to avoid unnecessary experimental opt-ins.
- Restore the persisted floating mini-stopwatch visibility preference when the app launches on Desktop.
- Persist closing the floating mini-stopwatch so it does not unexpectedly reopen on the next launch.
- Respect the persisted desktop keyboard-shortcut preference before processing Space, L, or R controls.
- Roll failed Settings writes back to the last successfully persisted preference state instead of leaving runtime/UI state divergent from disk.
- Preserve coroutine cancellation in shared UI persistence and platform export/share boundaries.
