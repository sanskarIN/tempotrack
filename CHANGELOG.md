# Changelog

All notable changes to TempoTrack are documented here.

The format follows Keep a Changelog concepts and the project uses semantic versioning.

## [Unreleased]

### Added

- Repository-local `tools/check_gradle_version_alignment.py` guard that verifies one Gradle version across wrapper metadata, Unix/Windows launchers, CI, CodeQL, and release automation while requiring checksum and retry/backoff hardening.

### Changed

- Hardened the supported Gradle 9.5.0 baseline with the official binary-distribution SHA-256, bounded wrapper download retries/backoff, and distribution URL validation.
- Added CI enforcement so wrapper metadata, launchers, CodeQL, and release automation cannot silently drift to different Gradle versions.
- Reverted an experimental Gradle 9.7.0 bump after compatibility review showed Kotlin 2.4.10 is fully supported through Gradle 9.5.0; API 37 support remains unchanged.
- Updated README, setup, testing, release, troubleshooting, contributor, roadmap, and repository-reference guidance for the hardened Gradle 9.5.0 baseline.

## [2.0.12] - 2026-08-19

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
