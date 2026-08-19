# Changelog

All notable changes to TempoTrack are documented here.

The format follows Keep a Changelog concepts and the project uses semantic versioning.

## [Unreleased]

### Added

- Production-oriented Kotlin/Compose Multiplatform project structure.
- Android and Desktop application entry points.
- Monotonic stopwatch engine with pause/resume/reset/laps.
- Fastest, slowest and average lap statistics.
- Named local session history and search.
- JSON/CSV export.
- Persistent preferences and active timer checkpoint.
- Light/dark/system themes and large-control accessibility option.
- Desktop mini-stopwatch integration.
- Unit tests for timing edge cases and exports.
- CI, security scanning, dependency updates, issue templates and release workflow.
- Complete project documentation baseline.

### Fixed

- Recover stale running checkpoints as paused after a monotonic clock reset/reboot.
- Neutralize spreadsheet-formula prefixes in CSV-exported text fields.
