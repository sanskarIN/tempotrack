# Changelog

All notable changes to TempoTrack are documented here.

The format follows Keep a Changelog concepts and the project uses semantic versioning.

## [Unreleased]

No unreleased changes are recorded after the 2.12.4 release-preparation freeze.

## [2.12.4] - 2026-08-24

### Added

- A deterministic `tools/check_gradle_version_alignment.py` guard now verifies wrapper version/checksum/retry policy, Unix/Windows fallback launchers, and every Gradle-bearing GitHub workflow remain aligned.
- A deterministic `tools/check_release_metadata.py` guard now centralizes canonical application version syntax, Android `versionCode` derivation/range validation, README release marker, dated changelog heading, roadmap release section, and optional semantic release-tag validation.
- Main CI now validates canonical application version metadata and requires `gradle.properties`, README, CHANGELOG, and ROADMAP to identify the same release line.

### Changed

- Canonical application defaults are advanced to version 2.12.4 with Android development `versionCode` 21204.
- Main CI and tagged release validation now share `tools/check_release_metadata.py` instead of maintaining separate semantic-version parsing logic in workflow shell.
- The tag workflow runs both release-metadata and Gradle-alignment guards on the exact tagged commit before Android/Desktop/iOS release jobs start.
- Release-tag validation requires the semantic tag to agree with `gradle.properties`, the derived Android versionCode, the README release marker, a dated changelog section, and the roadmap release section.
- Canonical tag examples and release documentation are aligned to the `v2.12.4` release line.
- Gradle wrapper metadata retains Gradle 9.5.0 and its pinned distribution SHA-256 while adding bounded download retries and retry backoff.
- README, setup, testing, build/CI, GitHub operations, maintainer, release, contribution, pull-request, changelog, and repository-reference guidance now use the five deterministic repository/toolchain/release guards.
- Documentation CI uses `actions/setup-python@v7` while preserving Python 3.13 verification.
- Obsolete Gradle 9.7 and superseded Gradle-hardening pull requests were closed after the useful release-safe hardening was carried into current `main` without the stale version metadata or unrelated AGP downgrade.

### Fixed

- Corrected the maintainer version-change example from the obsolete `1.0.0` / `versionCode=1` pair to the current semantic Android mapping (`2.12.4` / `21204`).
- Removed stale GitHub-operations wording that described only three deterministic guards and regex-only release-tag validation.

### Release status

- This section records source-tree release preparation only; it does not claim that a production `v2.12.4` tag has been created or published.
- Full CI/build/test observation, protected Android production signing, signed artifact inspection/checksum verification, real release screenshots, target-device accessibility/lifecycle testing, Desktop cross-host verification, and native iOS picker/share verification remain release gates.

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
- Versioned preferences and active-stopwatch stores with legacy migration.
- Persistent preferences, active timer checkpoints, floating mini-stopwatch visibility, and desktop shortcut enablement.
- Light/dark/system themes and large-control accessibility option.
- Adaptive navigation for larger mobile, tablet, desktop, and iOS layouts.
- Shared Compose string resources so user-facing copy is localization-ready.
- Shared design tokens for spacing, sizing, shapes, typography, and motion guidance.
- Desktop mini-stopwatch integration, keyboard shortcut help, persistent shortcut enable/disable control, and elapsed-time accessibility semantics.
- Branded Android launch/splash treatment, including Android 12+ splash attributes.
- Unit/integration tests for timing, persistence, migration, reboot/restart recovery, validation, import, export, safe export filename normalization, backup/restore journeys, arithmetic boundaries, and persistence limits.
- Android JVM tests for collision-safe legacy export naming, preservation of existing backups, unique share staging files, extension retention, and short filenames.
- CI, including macOS verification for the iOS shared framework, security scanning, dependency updates, issue templates and release workflow.
- Repository-local Markdown link validation in CI.
- Repository-local Kotlin package-keyword validation that rejects unescaped `package in.sanskar...` / `import in.sanskar...` source.
- Git-backed repository-reference coverage validation that requires every tracked file to be documented.
- A complete documentation index plus exhaustive tracked-file, source/API, state/recovery, data/storage, platform, user, maintainer, build/CI, and security guides.
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
- Android sharing stages each operation in a unique cache file and carries its granted content URI through both `EXTRA_STREAM` and `ClipData`.
- Pre-Android-10 explicit exports reserve collision-safe filenames instead of overwriting an existing backup.
- Android 10+ MediaStore export now requires successful pending-item finalization before reporting success.
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
- Contributor, pull-request, setup, testing, GitHub-operations, and release guidance now use the same three repository integrity checks.
- Privacy documentation explains Android cache sharing, iOS temporary export/share staging, and operating-system destination selection.
- Android Gradle Plugin is updated to 9.3.1 and Compose Multiplatform to 1.11.1.
- GitHub workflows use maintained Node 24-compatible action majors for checkout, Java/Python setup, Android setup, CodeQL, dependency review, artifact upload/download, and Gradle setup.
- `gradle/actions/setup-gradle` stays on the v5 line because v6 introduces a separately licensed proprietary caching component; Dependabot ignores only `gradle/actions` 6.x while allowing later versions to be evaluated.
- Dependabot no longer requests a repository label that does not exist.
- Canonical application defaults are advanced to version 2.0.12 with Android development `versionCode` 20012.

### Fixed

- Escape the Kotlin keyword package segment in every `in.sanskar...` package/import directive so the intended namespace is valid Kotlin source while preserving the compiled package name.
- Align active-stopwatch repository migration assertions with the current schema-v2 envelope instead of the obsolete schema-v1 expectation.
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
- Preserve independent Android share content when another share starts before the first recipient finishes reading.
- Support short sanitized Android share filenames without violating Java temporary-file prefix requirements.
- Preserve a staged Android share file when cancellation races a chooser that may already have received the granted URI.
- Delete incomplete Android MediaStore exports when content write or finalization fails.
- Correct troubleshooting documentation so corrupt durable history is described as a controlled fail-closed read error rather than an empty successful history.
- Correct restart documentation to describe the current uptime/wall recovery policy on Android/iOS and process-local `System.nanoTime()` policy on Desktop.
- Restore the persisted floating mini-stopwatch visibility preference when the app launches on Desktop.
- Persist closing the floating mini-stopwatch so it does not unexpectedly reopen on the next launch.
- Respect the persisted desktop keyboard-shortcut preference before processing Space, L, or R controls.
- Preserve coroutine cancellation in shared UI persistence and platform export/share boundaries.
- Exclude generated Compose/resource Kotlin from ktlint so repository style checks evaluate only source owned by the project instead of failing on generated output.
- Upgrade Android setup in CI, CodeQL, and release jobs so current command-line tools can resolve Android SDK Platform 37 instead of failing during SDK installation.
- Derive tagged Android `versionCode` from semantic version components instead of an unrelated GitHub Actions run number, keeping `v2.0.12` aligned with source versionCode `20012` and preserving upgrade ordering.
