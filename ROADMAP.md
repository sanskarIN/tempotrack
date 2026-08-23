# Roadmap

## 2.0.12 — Release hardening

- [x] Set canonical application defaults to `2.0.12` / Android development versionCode `20012`.
- [x] Freeze a dated 2.0.12 changelog section.
- [x] Synchronize README toolchain metadata with Compose Multiplatform 1.11.1 and the current Android Gradle Plugin baseline.
- [x] Document the exact `v2.0.12` release/tag contract.
- [x] Harden CI for Android SDK 37 and maintained Node 24-compatible action majors.
- [x] Keep generated Compose/resource Kotlin outside repository-owned ktlint checks.
- [x] Harden the Gradle 9.5.0 baseline with the official binary-distribution checksum and wrapper retry/backoff settings.
- [x] Keep Gradle wrapper metadata, Unix/Windows launchers, CI, CodeQL, release automation, and documentation aligned through an executable guard.
- [x] Revert the experimental Gradle 9.7.0 bump after compatibility review showed Kotlin 2.4.10 is fully supported only through Gradle 9.5.0.
- [x] Reduce AGP from 9.3.1 to 9.1.1, the minimum stable AGP for API 37 and the closest API-37-compatible baseline to Kotlin 2.4.10's published 9.1.0 AGP ceiling.
- [ ] Observe the complete 2.0.12 CI/build/test matrix on supported runners before tagging.
- [ ] Provision protected production Android signing secrets before a distributable `v2.0.12` tag.
- [ ] Inspect actual signed release artifacts and generated SHA-256 checksums.
- [ ] Capture real release screenshots from verified builds.
- [ ] Complete target-device accessibility/lifecycle checks and native iOS picker/share verification.

## 1.0 — Reliable local stopwatch

- [x] Monotonic stopwatch engine
- [x] Pause/resume/reset/laps
- [x] Lap statistics
- [x] Named local history
- [x] Search
- [x] CSV/JSON export
- [x] JSON restore with schema validation
- [x] Versioned local session persistence and legacy migration
- [x] Versioned preferences and active-stopwatch persistence
- [x] Android and Desktop entry points
- [x] Theme/accessibility settings
- [x] Desktop mini stopwatch
- [x] CI/security/documentation baseline

## 1.1 — UX and platform polish

- [ ] Add real release screenshots after verified tagged builds are captured.
- [x] Add Android system share sheet for JSON/CSV files using a restricted FileProvider.
- [x] Add desktop native save-file chooser with explicit cancellation handling.
- [x] Add adaptive large-screen navigation.
- [x] Externalize shared UI strings for localization.
- [x] Add lap sorting controls without changing recorded order.
- [x] Add keyboard shortcut help overlay.
- [x] Add persistent enable/disable configuration for desktop keyboard shortcuts.
- [ ] Add per-action desktop key rebinding only if user demand justifies the added complexity.
- [x] Add undo deletion.
- [x] Add validated session rename.
- [x] Persist floating mini-stopwatch visibility.
- [x] Add branded Android splash treatment.

## 1.2 — Broader portability

- [x] Add Kotlin/Native iOS framework targets and a Compose iOS entry point.
- [x] Add iOS framework and simulator verification jobs to macOS CI.
- [x] Package an iOS arm64 framework on release tags.
- [x] Add native iOS JSON/CSV sharing through `UIActivityViewController`.
- [x] Add direct iOS document-picker export destination support through `UIDocumentPickerViewController`.
- [ ] Add optional encrypted local backup using platform facilities only if a concrete threat model requires it.

## Documentation and maintainability

- [x] Add a central documentation index with role-based reading paths.
- [x] Add an exhaustive tracked-file reference covering source, tests, resources, workflows, tools, assets, policies, Gradle metadata, and documentation.
- [x] Add deep source/API, state/recovery, persistence/data lifecycle, platform, user, maintainer, build/CI, and security guides.
- [x] Add a deterministic Kotlin namespace guard for the `in.sanskar...` keyword-package syntax.
- [x] Add a Git-backed repository-reference coverage guard so newly tracked files cannot silently remain undocumented.
- [x] Add an executable Gradle-version alignment guard covering wrapper metadata, launchers, CI, CodeQL, and release automation.
- [x] Run the documentation/source guards in CI and expose them in contributor/PR/release guidance.
- [x] Correct stale troubleshooting guidance for corrupt history and platform checkpoint recovery.
- [ ] Observe a clean-checkout run of the complete documentation guards in an execution environment with GitHub/network access.

## Release engineering

- [x] Derive Android/Desktop package versions from release tags.
- [x] Produce checksummed tag artifacts and publish them to GitHub Releases.
- [x] Add environment-backed Android release signing configuration.
- [x] Require signed APK/AAB outputs before the tag workflow can publish Android release artifacts.
- [x] Scope Android signing secrets to only the workflow steps that require them.
- [x] Pin the current Gradle distribution checksum and enforce build-tool version consistency in automation.
- [ ] Provision the actual production Android signing secrets in protected repository/environment settings before creating a distributable Android tag release.

Roadmap items are not promises or release dates. Items that require private signing credentials, a macOS/Xcode host, physical/emulated devices, observed CI/clean-checkout execution, verified iOS picker/share behavior, or release screenshots remain intentionally open until they can be verified in the correct environment.
