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
- Branded light/dark/system themes, design tokens, and adaptive bottom-navigation/navigation-rail layouts.
- Large-control accessibility option and reduced-motion preference.
- Injectable English UI string catalog for localization-ready shared screens.
- Desktop mini-stopwatch integration and keyboard shortcuts.
- Unit and repository-integration tests for timing, persistence, statistics, serialization, and export security edge cases.
- CI, security scanning, dependency updates, issue templates and release workflow.
- Workflow concurrency that cancels superseded branch verification runs.
- Complete project documentation baseline.

### Changed

- Timer snapshots now reuse immutable lap history between lap mutations to avoid copying the full list every display refresh.
- Shared Android target configuration uses the current AGP Android-KMP DSL.

### Fixed

- Recover stale running checkpoints as paused after a monotonic clock reset/reboot.
- Neutralize spreadsheet-formula prefixes in CSV-exported text fields.
