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
- JSON/CSV export.
- Validated JSON history restore with explicit replacement confirmation.
- Versioned local session storage with automatic migration from the original list format.
- Persistent preferences and active timer checkpoint.
- Light/dark/system themes and large-control accessibility option.
- Desktop mini-stopwatch integration.
- Unit tests for timing, persistence, migration, validation, import and export edge cases.
- CI, including macOS verification for the iOS shared framework, security scanning, dependency updates, issue templates and release workflow.
- Complete project documentation baseline and iOS host integration guide.

### Fixed

- Recover stale running checkpoints as paused after a monotonic clock reset/reboot.
- Neutralize spreadsheet-formula prefixes in CSV-exported text fields.
- Reject malformed or internally inconsistent session records before persistence or restore.
- Fail closed on unsupported future local session schema versions.
- Keep serialization configuration on stable APIs to avoid unnecessary experimental opt-ins.
