# Repository File Reference

This document inventories every tracked TempoTrack file and explains why it exists. Directory entries are omitted because Git tracks files, not empty directories; every tracked file in the current repository layout is covered below.

When a file is added, renamed, or removed, update this reference in the same change series. `tools/check_repository_reference.py` enforces this contract in CI by comparing `git ls-files` with exact backticked paths in this document.

## Root repository files

| File | Responsibility | Maintenance notes |
|---|---|---|
| `.editorconfig` | Shared editor formatting defaults. | Keep compatible with ktlint and avoid editor-only formatting rules that fight CI. |
| `.env.example` | Documents optional local Android release-signing environment variables. | Never add real secrets, passwords, keystore bytes, or tokens. Runtime TempoTrack requires no credentials. |
| `.gitattributes` | Repository text/line-ending behavior. | Keep scripts and source consistently normalized across Windows/macOS/Linux. |
| `.gitignore` | Excludes build output, IDE state, local secrets, generated artifacts, and platform noise. | Any new secret/generated path should be ignored before developers commonly create it. |
| `CHANGELOG.md` | Unreleased and release change history. | Record meaningful user-facing/reliability/build changes; do not claim unverified checks. |
| `CODE_OF_CONDUCT.md` | Contributor/community conduct expectations. | Keep project contact/escalation information current. |
| `CONTRIBUTING.md` | Contribution workflow, style, tests, and pull-request expectations. | Update when quality commands, branch/release rules, or required tooling changes. |
| `LICENSE` | MIT license. | Do not alter without an intentional licensing decision. |
| `PRIVACY.md` | Documents local storage, backup, export/share, and temporary staging privacy behavior. | Must change with any new telemetry/network/cloud/storage or sharing behavior. |
| `README.md` | Product-facing repository overview and quick start. | Keep versions, supported platforms, commands, feature list, and documentation links synchronized. |
| `ROADMAP.md` | Tracks completed, planned, optional, and environment-gated work. | Do not mark device/signing/screenshot work complete before it is actually observed. |
| `SECURITY.md` | Vulnerability reporting and security policy. | Keep supported scope and contact route current. |
| `SUPPORT.md` | User support and project contact routes. | Keep addresses/links current. |
| `build.gradle.kts` | Root plugin declarations and aggregate `quality` verification task. | Add a new module's primary verification tasks when it becomes part of the supported product. |
| `gradle.properties` | Gradle performance flags plus application version/versionCode. | Version values feed Android/Desktop packaging; change deliberately for releases. |
| `gradlew` | Unix bootstrap launcher; uses wrapper JAR when present or requires exact installed Gradle fallback. | Preserve executable bit and exact-version enforcement. |
| `gradlew.bat` | Windows bootstrap launcher with the same exact-version fallback contract. | Be careful with batch delayed-expansion semantics. |
| `settings.gradle.kts` | Repository/plugin/dependency repositories and module inclusion. | Current modules: `shared`, `androidApp`, `desktopApp`. |
| `what_changed.md` | Durable engineering handoff/checkpoint requested for continuation work. | Append concrete work, commits, verification, blockers, and next exact tasks; do not replace history with a short summary. |

## GitHub project automation

| File | Responsibility | Maintenance notes |
|---|---|---|
| `.github/FUNDING.yml` | GitHub Sponsors/funding surface pointing to project support. | Keep funding account identifiers current. |
| `.github/ISSUE_TEMPLATE/bug_report.yml` | Structured bug reports. | Ask for reproducible behavior without requesting secrets/sensitive exports. |
| `.github/ISSUE_TEMPLATE/config.yml` | Issue-template chooser configuration and support/security links. | Keep links aligned with `SECURITY.md`/`SUPPORT.md`. |
| `.github/ISSUE_TEMPLATE/feature_request.yml` | Structured feature proposals. | Encourage problem/use-case descriptions, not only implementation requests. |
| `.github/dependabot.yml` | Automated dependency update configuration. | Review cadence/ecosystems when build tooling changes. |
| `.github/pull_request_template.md` | Pull-request quality/security/documentation checklist. | Keep required checks synchronized with `docs/testing.md`. |
| `.github/workflows/ci.yml` | Main Android/Desktop/shared/iOS/documentation CI matrix. | Includes release-metadata, Gradle-alignment, Kotlin package-keyword, repository-reference, and Markdown guards; keep tool versions aligned with Gradle/version catalog. |
| `.github/workflows/codeql.yml` | CodeQL static analysis. | Maintain least-privilege permissions and supported build behavior. |
| `.github/workflows/dependency-review.yml` | Pull-request dependency risk review. | Runs only where dependency diffs are available. |
| `.github/workflows/release.yml` | Semantic-tag build/package/checksum/publish workflow. | Android release requires protected signing secrets; release tags must remain strict semantic versions. |
| `.github/workflows/secret-scan.yml` | Secret scanning for pushes/PRs. | Never weaken patterns/permissions merely to silence a real finding. |

## Gradle dependency and wrapper metadata

| File | Responsibility | Maintenance notes |
|---|---|---|
| `gradle/libs.versions.toml` | Central dependency/plugin/SDK version catalog. | Update versions here first; verify compatibility across Android, Desktop, and Kotlin/Native. |
| `gradle/wrapper/gradle-wrapper.properties` | Pins the Gradle distribution URL/SHA-256 plus bounded download retry/backoff policy. | Current project expects Gradle 9.5.0; regenerate wrapper artifacts only from a trusted installation. |

`gradle/wrapper/gradle-wrapper.jar` is intentionally absent in the current repository state and therefore is not a tracked file. The launchers require an installed Gradle 9.5.0 when the standard wrapper JAR is unavailable.

## Android application module

### Module/build and manifest

| File | Responsibility | Maintenance notes |
|---|---|---|
| `androidApp/build.gradle.kts` | Android application plugin, SDK levels, versioning, Compose, dependencies, lint, unit tests, and optional environment-backed release signing. | Signing must be all-or-none; never hard-code credentials. |
| `androidApp/src/main/AndroidManifest.xml` | App component metadata, launch activity, backup rules, and restricted `FileProvider`. | Review exported flags and provider paths for every new component/share path. |

### Android Kotlin

| File | Responsibility | Maintenance notes |
|---|---|---|
| `androidApp/src/main/kotlin/in/sanskar/tempotrack/MainActivity.kt` | Android entry point; constructs storage, clocks, repositories, exporter/share service, and system-uptime recovery policy. | Live elapsed time uses `SystemClock.elapsedRealtimeNanos`; wall clock is only metadata. |
| `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidExporter.kt` | JSON/CSV export through MediaStore on Android 10+ and collision-safe app Documents storage on older Android. | MediaStore items remain pending until successfully finalized; failed items are deleted. |
| `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidShareService.kt` | Stages a unique cache file and opens the Android share chooser via `FileProvider`. | URI is supplied in `EXTRA_STREAM` and `ClipData` with temporary read permission. |
| `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidStagingFiles.kt` | Pure JVM filesystem helpers for unique export/share staging names. | Kept Android-framework-free so local JVM tests can exercise collision behavior. |
| `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidStringStorage.kt` | Application-private UTF-8 string storage with temp-file + atomic-replace behavior. | Fall back only when atomic moves are specifically unsupported. |

Kotlin source uses ``package `in`.sanskar...`` because `in` is a Kotlin keyword. The compiled package remains `in.sanskar...`.

### Android resources

| File | Responsibility | Maintenance notes |
|---|---|---|
| `androidApp/src/main/res/drawable/ic_launcher.xml` | Vector launcher/brand artwork used by Android resources. | Keep compatible with the manifest/theme branding. |
| `androidApp/src/main/res/drawable/splash_background.xml` | Splash-screen background drawable. | Coordinate visual changes with both style resource variants. |
| `androidApp/src/main/res/values/styles.xml` | Base launch/application theme behavior for pre-Android-12 and general Android. | Keep splash transition consistent with Compose content. |
| `androidApp/src/main/res/values-v31/styles.xml` | Android 12+ splash attributes. | Uses platform splash APIs unavailable to older resource qualifiers. |
| `androidApp/src/main/res/xml/backup_rules.xml` | Legacy Android backup inclusion/exclusion policy. | Exclude transient active/share/export cache data; review privacy impact before broadening. |
| `androidApp/src/main/res/xml/data_extraction_rules.xml` | Android 12+ cloud-backup/device-transfer rules. | Keep semantically aligned with `backup_rules.xml` and `PRIVACY.md`. |
| `androidApp/src/main/res/xml/file_paths.xml` | Restricts `FileProvider` to the share cache subtree. | Do not replace with broad cache/files-root exposure. |

### Android JVM tests

| File | Responsibility | Maintenance notes |
|---|---|---|
| `androidApp/src/test/kotlin/in/sanskar/tempotrack/AndroidExportStagingTest.kt` | Tests collision-safe filename reservation and preservation of existing exports. | Add cases for any future naming policy changes. |
| `androidApp/src/test/kotlin/in/sanskar/tempotrack/AndroidShareStagingTest.kt` | Tests per-operation unique share files, extension retention, and short names. | These are filesystem tests; device behavior still needs instrumentation/manual verification. |

## Desktop application module

| File | Responsibility | Maintenance notes |
|---|---|---|
| `desktopApp/build.gradle.kts` | JVM 17/Compose Desktop application configuration and DMG/MSI/DEB packaging metadata. | `mainClass` remains the compiled package `in.sanskar.tempotrack.desktop.MainKt`. |
| `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/Main.kt` | Desktop entry point, dependency wiring, keyboard shortcuts, mini-window, heartbeat persistence, and safe restart policy. | Persisted `System.nanoTime()` values are never compared across JVM processes. |
| `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/DesktopExporter.kt` | Swing `JFileChooser` destination selection and UTF-8 file export. | User cancellation is a distinct result; chooser unavailability is not a write failure. |
| `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/JvmStringStorage.kt` | Desktop private UTF-8 storage with atomic replacement fallback. | Keep data path private under the user's `.tempotrack` directory. |

## Shared module build/resources

| File | Responsibility | Maintenance notes |
|---|---|---|
| `shared/build.gradle.kts` | Kotlin Multiplatform targets, Compose resources, Android library settings, iOS frameworks, and common dependencies/tests. | Current targets: Android library, Desktop JVM, iOS x64/arm64/simulatorArm64. |
| `shared/src/commonMain/composeResources/values/strings.xml` | Main localization-ready user-facing string catalog. | Add UI copy here instead of hard-coding when practical; preserve placeholders. |
| `shared/src/commonMain/composeResources/values/shortcuts.xml` | Desktop shortcut/help strings. | Keep actual key bindings and help text synchronized. |
| `shared/src/commonMain/composeResources/values/reliability.xml` | Reliability/persistence failure copy. | Messages should be user-safe and avoid raw exception/storage content. |

## Shared domain source

| File | Responsibility | Key contract |
|---|---|---|
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/Clock.kt` | Injectable `MonotonicClock` and `WallClock` interfaces. | Live elapsed duration depends only on monotonic time. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/DurationFormatter.kt` | Converts nanoseconds to stable display strings. | Negative input is clamped to zero. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/Models.kt` | Serializable stopwatch statuses, laps, snapshots, checkpoints, sessions, and lap statistics. | Lap averages use overflow-safe integer arithmetic. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/SessionIdGenerator.kt` | Generates bounded session IDs from save metadata plus random suffix. | IDs stay within shared validation length. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/SessionValidation.kt` | Validates session identity, names, durations, lap order/totals/splits, and limits. | Validation arithmetic is overflow-safe. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/StopwatchCheckpointRecovery.kt` | Safe recovery policies for persisted running checkpoints. | System-uptime recovery compares uptime/wall deltas; unsafe references pause at known elapsed. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/StopwatchCheckpointValidation.kt` | Structural validation for active timer checkpoints. | Running requires start timestamp; idle/paused invariants differ; lap count matches session limit. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/StopwatchEngine.kt` | Deterministic stopwatch state machine: start/pause/resume/reset/lap/snapshot/checkpoint. | Elapsed addition saturates at `Long.MAX_VALUE`; running checkpoints are rebased at save time. |

## Shared data source

| File | Responsibility | Key contract |
|---|---|---|
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/Storage.kt` | Platform-neutral `StringStorage`, `Exporter`, and `ShareService` contracts plus typed results/errors. | Platform adapters translate exceptions into small stable result enums. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ExportFileName.kt` | Sanitizes bounded platform export/share filenames. | Prevent traversal-like/unsafe characters and provide nonblank fallback. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionCodec.kt` | Portable JSON and seven-column CSV encoding. | CSV escapes fields and neutralizes spreadsheet-formula prefixes. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionImport.kt` | Validates portable JSON restore input and returns typed import failures. | Import limits are aligned with persistence limits; duplicate IDs/invalid sessions fail before replacement. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionRepository.kt` | Mutex-serialized saved-session CRUD/replace persistence. | Fails closed on corrupt/future/invalid history; skips no-op rewrites. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionStoreCodec.kt` | Internal versioned saved-session envelope and legacy-list migration. | Portable export format is intentionally separate from internal envelope. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/PreferencesRepository.kt` | Versioned app-preference persistence with bounded payload. | Corrupt/oversized preferences fall back to defaults rather than exposing raw errors. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/PreferencesStoreCodec.kt` | Preference envelope serialization and legacy migration. | Newly added preference fields rely on safe serializer defaults. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ActiveStopwatchRepository.kt` | Mutex-serialized active-checkpoint load/save/clear with validation and bounds. | Invalid/oversized stored checkpoints fail closed. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ActiveStopwatchStoreCodec.kt` | Active checkpoint schema-v2 envelope, schema-v1/bare migration, future-version rejection. | v2 carries `savedAtEpochMillis` recovery metadata. |

## Shared UI and utility source

| File | Responsibility | Maintenance notes |
|---|---|---|
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/AppTheme.kt` | Maps system/light/dark preference to Material color scheme. | Keep theme choice platform-neutral. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/DesignTokens.kt` | Shared spacing, sizing, shapes, typography, and motion constants. | Prefer these tokens over duplicated UI literals. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/ExternalUri.kt` | Safe wrapper around Compose `UriHandler`. | About/settings links should not crash if a platform cannot open a URI. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/MiniStopwatch.kt` | Compact stopwatch UI used by Desktop floating window. | Shares engine/checkpoint persistence and elapsed-time semantics with main screen. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/TempoTrackDependencies.kt` | Dependency/capability bundle supplied by platform entry points. | New platform-only behavior should be exposed through small capability hooks rather than platform APIs in common UI. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/TempoTrackApp.kt` | Shared app root, initialization, onboarding, navigation, adaptive layout, checkpoint normalization, heartbeat loop. | Startup is the single place that converts stored checkpoint into platform-safe recovered state. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/AboutScreen.kt` | Product identity, version/platform, GitHub/funding/contact actions. | Keep metadata/links synchronized with repository policy docs. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/HistoryScreen.kt` | Search, export/share, restore, rename, delete/undo, and operation concurrency controls. | Data portability and mutations are deliberately single-flight/conflict-aware. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/OnboardingScreen.kt` | First-run privacy/value message and persisted onboarding completion. | Never advance permanently if preference persistence fails. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/SettingsScreen.kt` | Theme/accessibility/Desktop preference UI with optimistic state + rollback on failed persistence. | Platform side effects must also roll back when persistence fails. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/StopwatchScreen.kt` | Main timer/laps/session-save UI. | Session saves are single-flight; stale saved feedback clears after timer/name changes. |
| `shared/src/commonMain/kotlin/in/sanskar/tempotrack/util/SuspendResult.kt` | Converts ordinary suspend failures to `Result` while rethrowing coroutine cancellation. | Never swallow `CancellationException`. |

## Shared common tests

| File | Coverage focus |
|---|---|
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/StopwatchJourneyTest.kt` | End-to-end stopwatch → session → JSON backup → restore and active-checkpoint restart journeys. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/ActiveStopwatchRepositoryTest.kt` | Checkpoint persistence, validation, migration to schema v2, concurrency. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/ActiveStopwatchStoreCodecTest.kt` | Current round-trip, v1 migration, bare legacy migration, future-version rejection. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/ConcurrentWriteDetectingStorage.kt` | Test helper that deliberately detects overlapping storage mutation. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/ExportFileNameTest.kt` | Filename sanitization, traversal-like punctuation, fallback, length bound. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/PreferencesRepositoryTest.kt` | Preference round-trip/defaults/migration/oversized input/concurrent saves. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/PreferencesStoreCodecTest.kt` | Versioned/legacy preference codec behavior and new-field defaults. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/SessionCodecTest.kt` | CSV escaping/formula neutralization/column consistency and JSON identity. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/SessionImportTest.kt` | Restore limit alignment, sorting, malformed JSON, duplicates, invalid-session error mapping. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/SessionRepositoryTest.kt` | Sorting, CRUD, concurrency, corruption handling, duplicate rejection, and no-op write avoidance. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/SessionStoreCodecTest.kt` | Internal session envelope round-trip, legacy migration, future-version rejection. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/DurationFormatterTest.kt` | Duration display and negative clamping. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/LapStatisticsTest.kt` | Rounded/overflow-safe average computation. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/SessionIdGeneratorTest.kt` | Stable metadata format, uniqueness suffix, bounds, unsigned hex behavior. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/SessionValidationTest.kt` | Valid sessions and malformed/negative/overflow-sensitive lap relationships. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/StopwatchCheckpointRecoveryTest.kt` | Safe pause fallback and uptime/wall consistency behavior. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/StopwatchCheckpointValidationTest.kt` | Status/timestamp/lap invariants and shared lap ceiling. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/StopwatchEngineTest.kt` | Timing, sleep-like jumps, overflow saturation, rebased checkpoints, laps, reset, stale origins. |
| `shared/src/commonTest/kotlin/in/sanskar/tempotrack/util/SuspendResultTest.kt` | Cancellation propagation and ordinary success/failure conversion. |

## iOS Kotlin/Native source and tests

| File | Responsibility | Maintenance notes |
|---|---|---|
| `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosPlatformAdapters.kt` | `NSUserDefaults` storage plus uptime/wall clock adapters. | Uptime is converted to nanoseconds; live timing remains monotonic. |
| `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosTemporaryExportFile.kt` | Creates sanitized UTF-8 temporary export/share file in a unique operation directory and removes it. | Every native data-portability flow should clean its directory. |
| `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosShareService.kt` | Presents `UIActivityViewController`, handles popover anchoring, retains active controller, cleans staging. | Must be simulator/device tested on iPhone and regular-width/iPad presentation. |
| `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosDocumentExporter.kt` | Presents `UIDocumentPickerViewController`, retains delegate, serializes export operations, handles cancellation/cleanup. | UIKit/Kotlin-Native interop requires macOS/Xcode verification. |
| `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/MainViewController.kt` | Compose iOS host controller and dependency wiring. | Containing Xcode app still owns signing and App Store packaging. |
| `shared/src/iosTest/kotlin/in/sanskar/tempotrack/ios/IosTemporaryExportFileTest.kt` | Simulator-target tests for sanitization, isolation, file creation, and cleanup. | Run only on macOS/iOS simulator toolchains. |

## Repository-local tools

| File | Responsibility | Maintenance notes |
|---|---|---|
| `tools/check_gradle_version_alignment.py` | Verifies the wrapper version/checksum/retry policy, Unix/Windows launcher fallback version, and every Gradle-bearing GitHub workflow stay aligned. | Run after any Gradle/toolchain workflow change; CI enforces it. |
| `tools/check_kotlin_package_keywords.py` | Fails on unescaped `package in.sanskar...` / `import in.sanskar...` Kotlin source. | Required because `in` is a Kotlin keyword; CI runs it. |
| `tools/check_markdown_links.py` | Checks deterministic repository-local Markdown link destinations. | External URLs are intentionally not treated as deterministic local checks. |
| `tools/check_release_metadata.py` | Validates canonical `appVersion`/`appVersionCode`, README/CHANGELOG/ROADMAP release markers, and an optional semantic release tag. | Run on normal CI without `--tag`; release automation should pass the actual tag. |
| `tools/check_repository_reference.py` | Compares `git ls-files` against exact backticked paths in this document and fails when any tracked file is undocumented. | Run whenever tracked files change; CI enforces complete file-documentation coverage. |

## Documentation files

| File | Purpose |
|---|---|
| `docs/README.md` | Documentation index, reading paths, and documentation maintenance rule. |
| `docs/repository-reference.md` | This complete tracked-file inventory. |
| `docs/code-reference.md` | Deeper source/API responsibility reference. |
| `docs/state-and-recovery.md` | Stopwatch state machine, checkpoint semantics, platform recovery. |
| `docs/data-model-and-storage.md` | Models, schemas, limits, backup/restore, data lifecycle. |
| `docs/platforms.md` | Android/Desktop/iOS behavior and platform adapter contracts. |
| `docs/user-guide.md` | End-user workflow for stopwatch, history, export/share/restore, settings, and recovery. |
| `docs/maintainer-guide.md` | Common change recipes and repository maintenance procedures. |
| `docs/build-and-ci.md` | Gradle modules/toolchain, deterministic guards, CI/security workflows, release pipeline. |
| `docs/security-model.md` | Engineering trust boundaries, malformed-input controls, sharing/storage security, signing/supply chain. |
| `docs/accessibility.md` | Accessibility implementation and manual verification guidance. |
| `docs/architecture.md` | Module/dependency architecture and core invariants. |
| `docs/development.md` | Day-to-day development workflow reference. |
| `docs/github.md` | GitHub Actions/Dependabot/templates/repository automation. |
| `docs/ios.md` | iOS framework integration and native bridge verification. |
| `docs/localization.md` | Compose-resource localization conventions/review. |
| `docs/performance.md` | Timing, persistence, history responsiveness, and performance budgets. |
| `docs/release-notes-template.md` | Repeatable release-note skeleton. |
| `docs/release.md` | Signing, semantic tags, packaging, checksums, release procedure. |
| `docs/setup.md` | Toolchain installation/setup and first build commands. |
| `docs/testing.md` | Automated/manual test matrix and verification-integrity policy. |
| `docs/troubleshooting.md` | Setup/build/runtime/persistence/platform/release failure diagnosis. |
| `docs/screenshots/README.md` | Screenshot capture policy/placeholders; real release captures remain environment-gated. |
| `docs/assets/logo.svg` | Repository/product documentation logo artwork. |
| `docs/adr/0001-monotonic-time.md` | ADR: monotonic elapsed-time source. |
| `docs/adr/0002-local-json-storage.md` | ADR: bounded local JSON storage. |
| `docs/adr/0003-agp9-module-layout.md` | ADR: AGP 9 module structure. |
| `docs/adr/0004-versioned-session-storage.md` | ADR: versioned local session persistence. |
| `docs/adr/0005-platform-checkpoint-recovery.md` | ADR: reboot/process-safe recovery policy. |

## Cross-file change map

Use this map to avoid partial changes:

- **Change a persisted session field** → `Models.kt`, `SessionValidation.kt`, `SessionStoreCodec.kt`, `SessionCodec.kt`/`SessionImport.kt` if portable, tests, `data-model-and-storage.md`, changelog.
- **Change active timer persistence/recovery** → `Models.kt`, `StopwatchEngine.kt`, checkpoint validation/recovery, active codec/repository, each platform entry point, tests, ADR 0005, architecture/testing docs.
- **Change a preference** → `PreferencesRepository.kt`, preference codec defaults/migration, `SettingsScreen.kt`, any platform side-effect hook, tests, localization strings/docs.
- **Change Android share/export paths** → Android adapter, manifest/provider XML/backup rules as applicable, Android staging tests, privacy/testing/platform docs.
- **Change iOS export/share** → iOS staging + bridge, iOS tests, `ios.md`, privacy/testing/platform docs.
- **Change Desktop shortcut/mini-window behavior** → Desktop `Main.kt`, shared preference/dependency/UI files, resource strings, tests/docs where applicable.
- **Change build dependency/tool version** → `libs.versions.toml`, affected module build scripts, setup/testing/release docs, CI/release workflow if the runner/tool installation must change.
- **Add a tracked file** → update this document, run `tools/check_repository_reference.py`, and ensure local Markdown checks can resolve any new links.
