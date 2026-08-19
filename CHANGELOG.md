# Changelog

All notable changes to TempoTrack are documented here.

The format follows Keep a Changelog concepts and the project uses semantic versioning.

## [Unreleased]

### Added

- Production-oriented Kotlin/Compose Multiplatform project structure.
- Android and Desktop application entry points.
- iOS Kotlin/Native framework targets and Compose host entry point.
- Monotonic stopwatch engine with pause/resume/reset/laps.
- Fastest, slowest and average lap statistics.
- Named local session history and search.
- Validated session rename workflow.
- JSON/CSV export.
- Validated JSON history restore with explicit replacement confirmation.
- Versioned local session storage with automatic migration from the original list format.
- Versioned preferences and active-stopwatch stores with legacy migration.
- Persistent preferences, active timer checkpoints, and floating mini-stopwatch visibility.
- Light/dark/system themes and large-control accessibility option.
- Adaptive navigation for larger mobile, tablet, desktop, and iOS layouts.
- Shared Compose string resources so user-facing copy is localization-ready.
- Shared design tokens for spacing, sizing, shapes, typography, and motion guidance.
- Desktop mini-stopwatch integration and keyboard shortcut help.
- Branded Android launch/splash treatment, including Android 12+ splash attributes.
- Unit tests for timing, persistence, migration, validation, import and export edge cases.
- CI, including macOS verification for the iOS shared framework, security scanning, dependency updates, issue templates and release workflow.
- Repository-local Markdown link validation in CI.
- Tagged release packaging for Android, Desktop, and iOS framework artifacts, with SHA-256 checksums and GitHub Release publishing.
- Complete project documentation baseline and iOS host integration guide.

### Changed

- Session import and export failures now use stable internal error codes; shared UI maps them to localized user-safe messages.
- Android and Desktop package versions are sourced from Gradle properties and release tags.
- Android private file replacement now uses atomic move when supported with safe replacement fallback.

### Fixed

- Recover stale running checkpoints as paused after a monotonic clock reset/reboot.
- Neutralize spreadsheet-formula prefixes in CSV-exported text fields.
- Reject malformed or internally inconsistent session records before persistence or restore.
- Validate active stopwatch checkpoints before persisting or restoring them.
- Fail closed on unsupported future local session, preference, and active-stopwatch schema versions.
- Keep serialization configuration on stable APIs to avoid unnecessary experimental opt-ins.
- Restore the persisted floating mini-stopwatch visibility preference when the app launches on Desktop.
