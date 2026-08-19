# TempoTrack — Work Handoff

## Current milestone

Phase 0 → Phase 6 implementation and release-candidate audit are in progress from the master prompt uploaded on 2026-08-19.

The repository is now a substantial Kotlin + Jetpack Compose Multiplatform stopwatch implementation rather than scaffolding. This continuation preserved existing working code, audited the current `main` branch, closed remaining Android/Desktop export UX gaps, added secure Android system sharing, added persistent Desktop shortcut configuration, extended tests/documentation, and kept environment-gated release tasks explicitly open instead of claiming unverified completion.

## Implementation contract

- Public/open-source TempoTrack stopwatch.
- Kotlin + Jetpack Compose Multiplatform.
- Android and Desktop primary targets; Kotlin/Native/Compose iOS integration is present.
- MIT license.
- Visible credit: **Made by the Sanskar**.
- Business email: `sanskarin@outlook.in`.
- Business email: `sanskarin.business@gmail.com`.
- Support email: `supportramsandesh@gmail.com`.
- GitHub: `https://github.com/sanskarIN`.
- Buy Me a Coffee: `https://buymeacoffee.com/sanskarIN`.
- Small, atomic, meaningful commits are preferred.
- Requested maintainer Git email: `sanskarin@outlook.in`.

## Current repository capabilities

### Stopwatch engine

- Start, pause, resume and reset.
- Lap/split recording.
- Millisecond display precision.
- Injected monotonic clock abstraction.
- Android timing uses `SystemClock.elapsedRealtimeNanos()` so elapsed timing is not based on wall-clock changes and includes device sleep.
- Desktop timing uses `System.nanoTime()`.
- iOS timing uses `NSProcessInfo.systemUptime`.
- Active stopwatch checkpoint persistence.
- Safe post-reboot/stale monotonic-clock recovery.
- Checkpoint validation before persistence/restoration.

### Laps and statistics

- Recorded lap ordering.
- Fastest-first and slowest-first views without mutating recorded order.
- Fastest/slowest labels.
- Average split statistics.
- Validation of split and cumulative totals.

### Session history

- Named local sessions.
- Search.
- Validated rename.
- Delete with undo.
- Newest-first persistence.
- Duplicate-ID rejection.
- Corrupt persistence fails closed instead of being silently rewritten.
- Bounded store size.

### Data portability

- JSON export.
- CSV export.
- Spreadsheet-formula neutralization in CSV string fields.
- Validated JSON restore.
- Restore requires explicit replacement confirmation.
- Import bounds for content size/session count.
- Versioned internal storage envelopes with legacy migration.
- Portable exported JSON intentionally stays independent of the internal persistence envelope.
- Shared bounded filename sanitization for filesystem/share operations.
- Desktop native save-file chooser.
- Explicit Desktop export cancellation handling.
- Android system share-sheet actions for JSON and CSV.
- Android sharing uses a restricted non-exported `FileProvider` and temporary read grant rather than raw filesystem paths.

### Preferences and UX

- First-run onboarding.
- Light/dark/system theme.
- Large-control accessibility option.
- Reduced-motion preference.
- Adaptive compact/wide navigation.
- Shared design tokens.
- Compose Multiplatform string resources for localization readiness.
- Settings sections for appearance, accessibility, desktop controls, privacy, data, updates and About.
- About content with project identity, version/platform, MIT license, GitHub, support/business contacts, Buy Me a Coffee and **Made by the Sanskar**.

### Desktop

- Desktop application entry point.
- Floating always-on-top mini stopwatch.
- Persistent mini-stopwatch visibility.
- Default stopwatch shortcuts: Space = start/pause/resume, L = lap, R = reset.
- Shortcut help dialog.
- Persistent enable/disable setting for stopwatch shortcuts.
- Disabled stopwatch shortcuts do not disable ordinary keyboard focus/navigation.
- Native save-file chooser for export destination selection.

### Android

- Android application entry point.
- Application-private storage.
- Atomic replacement where supported with safe fallback.
- MediaStore export on modern Android.
- Restricted FileProvider share cache.
- Android system share sheet for JSON/CSV backups/data.
- Branded launch/splash treatment, including Android 12+ splash resources.
- Android backup/data-extraction rules.

### iOS

- `iosX64`, `iosArm64` and `iosSimulatorArm64` Kotlin targets.
- Static `TempoTrackShared` framework.
- Compose `MainViewController()` entry point.
- iOS clocks and local preferences/session adapters.
- macOS CI tasks for iOS simulator framework/test verification.
- Release workflow support for iOS arm64 framework packaging.
- Native iOS document/share-sheet bridge intentionally remains host-layer work; shared code fails safely instead of pretending export succeeded.

### Repository and release engineering

- MIT `LICENSE`.
- `.gitignore`, `.editorconfig`, `.gitattributes`, `.env.example`.
- README and complete policy/documentation baseline.
- ADRs.
- Issue templates and PR template.
- Dependabot.
- CI for shared/Desktop/Android plus macOS iOS verification.
- CodeQL.
- Dependency review.
- Secret scanning.
- Markdown link checker.
- Tag-driven versioning/package workflow.
- Release artifact checksum generation.
- GitHub Release publishing workflow.

## Work completed in the latest continuation batch

### Native Desktop export UX

- Replaced fixed-folder Desktop export behavior with a native `JFileChooser` save dialog.
- Suggested filenames and JSON/CSV filters are supplied to the chooser.
- The selected destination remains under user control.
- Cancel is represented as `ExportError.USER_CANCELLED` instead of a false write failure.
- Shared UI maps cancellation to a localized user-safe message.

### Secure Android sharing

- Added a platform-neutral `ShareService` contract and stable share result/error types.
- Added AndroidX Core required for `FileProvider`.
- Registered a non-exported `FileProvider` using `${applicationId}.fileprovider`.
- Limited provider exposure to `cache/shared-exports/`.
- Added `AndroidShareService`.
- Share files are created only after explicit user action.
- The system chooser receives a `content://` URI with temporary read permission.
- JSON and CSV share actions only appear when a platform share service is present.
- iOS/Desktop hosts are not falsely advertised as sharing-capable through this contract unless a concrete service is supplied.

### Export filename hardening

- Added one shared `ExportFileName` sanitization policy.
- Unsafe characters are replaced.
- traversal-like leading punctuation is removed.
- output is length bounded.
- empty/unsafe-only names fall back to `tempotrack-export`.
- Android export, Android share and Desktop export reuse the same policy.
- Added common tests for normalization and bounds.

### Desktop shortcut configuration

- Added `keyboardShortcutsEnabled` to persisted preferences with a backwards-compatible default of `true`.
- Added host callback wiring through `TempoTrackDependencies`.
- App startup restores the preference into the Desktop runtime.
- Settings exposes a persistent enable/disable control.
- Desktop `onKeyEvent` ignores stopwatch shortcut handling when disabled.
- Added preference codec coverage for explicit disabled state and legacy default behavior.

### Documentation and privacy

- Updated README for Android secure sharing, Desktop destination chooser, shortcut configuration, filename hardening, and data portability.
- Updated `PRIVACY.md` with temporary share-cache behavior and operating-system destination selection.
- Updated accessibility guidance for shortcut conflicts/assistive technologies.
- Updated testing guidance with Android sharing, Desktop export, filename hardening, and shortcut persistence checks.
- Updated changelog and roadmap.
- Roadmap now marks Android system sharing and Desktop native save chooser complete.
- Environment/credential/device-gated items remain open rather than being represented as complete.

## Files added in the latest continuation batch

- `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidShareService.kt`
- `androidApp/src/main/res/xml/file_paths.xml`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ExportFileName.kt`
- `shared/src/commonMain/composeResources/values/shortcuts.xml`
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/ExportFileNameTest.kt`

## Files changed in the latest continuation batch

- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/Storage.kt`
- `gradle/libs.versions.toml`
- `androidApp/build.gradle.kts`
- `androidApp/src/main/AndroidManifest.xml`
- `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidExporter.kt`
- `androidApp/src/main/kotlin/in/sanskar/tempotrack/MainActivity.kt`
- `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/DesktopExporter.kt`
- `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/Main.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/PreferencesRepository.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/TempoTrackDependencies.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/TempoTrackApp.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/HistoryScreen.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/SettingsScreen.kt`
- `shared/src/commonMain/composeResources/values/strings.xml`
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/PreferencesStoreCodecTest.kt`
- `README.md`
- `PRIVACY.md`
- `CHANGELOG.md`
- `ROADMAP.md`
- `docs/accessibility.md`
- `docs/testing.md`
- `what_changed.md`

## Earlier substantial continuation work retained

The current branch also contains earlier audit/improvement work beyond the original handoff, including:

- externalized Compose string resources;
- explicit generated resource imports;
- localization documentation;
- shared design tokens;
- adaptive layout/navigation;
- versioned preference and active-stopwatch storage;
- validation and migration tests;
- concurrency-safe repository writes;
- corrupted-history fail-closed behavior;
- session-store size limits;
- Android API-compatible storage writes;
- Android splash resources;
- release tag/version/checksum improvements;
- Markdown link checking;
- expanded accessibility/testing/architecture documentation.

These existing changes were preserved and integrated with the newest work instead of rewritten.

## Verification performed in this continuation

- Repository metadata/current tree/source files were inspected through the connected GitHub API.
- Recent commit history was inspected after writes.
- Repository search for `TODO FIXME` returned no results in the current indexed repository state.
- Changed files were re-read where necessary to avoid stale-SHA overwrites.
- A stale-SHA write conflict on `AndroidShareService.kt` was detected, the current file was re-fetched, and the update was safely reapplied rather than force-overwriting concurrent work.
- GitHub combined-status lookup on commit `c6d845910867570c68157bb2ef50fc017751ba8e` returned no status contexts through the connector at the time checked.
- A container-side clean clone was attempted earlier in this project continuation but failed because the execution environment could not resolve `github.com`; therefore Gradle dependencies/repository contents could not be obtained through the container for a local build.

## Verification integrity / limitations

This handoff does **not** claim the complete Android/Desktop/iOS build, lint and test matrix has passed after the newest commits because that result has not been observable in the available environment.

The repository CI is configured to perform the appropriate checks, but the connector currently returns no combined status contexts for the latest checked commit. Build/test status must therefore remain `not observed`, not `passed` or `failed`.

The GitHub contents connector can create/update UTF-8 text files but cannot conveniently generate or safely author the binary `gradle-wrapper.jar`. Existing bootstrap scripts delegate to an installed Gradle if the wrapper JAR is absent. A trusted local Gradle installation should regenerate the standard wrapper JAR if full wrapper self-containment is required.

## Known remaining environment/host-gated items

### Required before a truthful release-candidate declaration

1. Observe a clean CI run after the current commits and repair any compile/lint/test failure.
2. Run or observe Android debug/release build and lint.
3. Run or observe Desktop compilation/tests/package task on supported desktop hosts.
4. Run or observe iOS simulator framework link/tests on macOS/Xcode.
5. Run/observe secret scan, dependency review, CodeQL and documentation-link checks.
6. Capture real Android/Desktop screenshots from verified builds and replace placeholders.

### Host/product decisions intentionally still open

- Native iOS document/share-sheet export bridge must be implemented/verified in the iOS host layer.
- Per-action Desktop key rebinding is intentionally not implemented merely to increase feature count; the complete shortcut layer is currently configurable on/off and documented. Add individual key remapping only if there is a clear user need and a collision-safe binding model.
- Optional encrypted local backup remains deferred until there is a concrete threat model and platform-appropriate key-management design; inventing custom crypto would conflict with the security requirements.
- Production Android signing requires private signing material configured outside source control. Signing secrets must never be committed.

## Definition-of-Done status

### Implemented in source

- [x] Core stopwatch flows.
- [x] Monotonic timing architecture.
- [x] Laps/statistics/sorting.
- [x] Named/searchable history.
- [x] Local persistence with versioning/migration/validation.
- [x] JSON/CSV export.
- [x] JSON restore/import validation.
- [x] Android system sharing.
- [x] Desktop native export destination selection.
- [x] Accessibility preferences and keyboard support.
- [x] Android/Desktop entry points.
- [x] iOS framework/Compose entry-point readiness.
- [x] Documentation/policies/CI/security workflow baseline.
- [x] Contact/funding/license/credit requirements.

### Still requires external verification/assets

- [ ] Clean-checkout quality suite observed passing after newest commits.
- [ ] Supported release packages observed building from the current head.
- [ ] Production Android signing configured securely.
- [ ] Native iOS host export/share bridge.
- [ ] Real release screenshots.

## Commit author identity limitation

The connected GitHub write API does not expose custom author/committer email fields for file writes. Therefore connector-generated commits cannot be forced to use `sanskarin@outlook.in` as their Git author email.

Maintainer/local Git configuration remains:

```bash
git config user.email "sanskarin@outlook.in"
```

No claim is made that connector-generated commit metadata uses that email.

## Latest continuation commits

- `3149b15` — `feat: distinguish cancelled export operations`
- `20b9d27` — `feat: use native desktop save-file chooser`
- `314c713` — `feat: add localized export cancellation message`
- `be7066e` — `fix: handle cancelled export operations`
- `2da2eab` — `feat: define portable share service contract`
- `7c2f919` — `build: add AndroidX Core for secure file sharing`
- `0bc6fdd` — `build: enable Android secure share provider dependency`
- `0f84988` — `feat: register secure Android file provider`
- `22dd16e` — `feat: restrict Android shared files to export cache`
- `6bc81b8` — `feat: add Android system share-sheet service`
- `252f950` — `feat: expose optional platform share service`
- `348145a` — `feat: wire Android share service into app`
- `2dde871` — `feat: pass share service to history UI`
- `dae3ee7` — `feat: add localized history sharing labels`
- `7a4868b` — `feat: add platform share actions to history`
- `9d83f48` — `refactor: centralize safe export filenames`
- `acd58cd` — `refactor: reuse safe export filename policy`
- `25449e0` — `refactor: sanitize Android shared export filenames`
- `95ca1c8` — `refactor: reuse safe desktop export filenames`
- `f5a8823` — `test: cover safe export filename normalization`
- `ab2275d` — `feat: persist desktop shortcut enablement`
- `d5b8e7a` — `feat: expose desktop shortcut preference callback`
- `9dac828` — `feat: apply persisted desktop shortcut setting`
- `e1d6258` — `feat: add localized shortcut configuration strings`
- `3129156` — `feat: add persistent desktop shortcut toggle`
- `1fa8d33` — `feat: honor desktop shortcut preference at runtime`
- `9247a1e` — `test: cover shortcut preference persistence`
- `e8b03e3` — `docs: document temporary share-cache behavior`
- `a43a380` — `docs: mark completed export and shortcut polish`
- `ad03e13` — `docs: record export sharing and shortcut improvements`
- `a648302` — `docs: document native export and sharing workflows`
- `867920f` — `docs: document configurable keyboard accessibility`
- `c6d8459` — `docs: add export and sharing verification checks`

## Earlier continuation commits retained in history

- `0813995` — `build: add iOS targets to shared module`
- `d764e9c` — `feat: add iOS clocks and local storage adapter`
- `225c326` — `feat: add iOS Compose entry point`
- `f659fb5` — `feat: validate persisted stopwatch sessions`
- `267a73f` — `fix: reject invalid persisted session data`
- `77cdc6c` — `test: cover session validation rules`
- `43935c9` — `feat: add validated JSON session import parser`
- `9254a78` — `test: cover safe JSON session imports`
- `e8b7126` — `feat: add JSON history restore workflow`
- `d6fc007` — `test: cover session repository persistence`
- `c4f29a4` — `docs: document iOS host integration`
- `df53f6f` — `ci: verify iOS shared framework on macOS`
- `3f9eb84` — `feat: version the persisted session schema`
- `62ff179` — `feat: migrate legacy session storage automatically`
- `e1d04d8` — `test: cover session schema migration`
- `4bfb334` — `fix: avoid experimental serialization configuration`
- `2c68ee6` — `fix: keep session schema codec on stable serialization API`
- `8d2763f` — `docs: update changelog for reliability and iOS work`
- `3039402` — `docs: refresh roadmap after portability work`
- `ef443f2` — `docs: add iOS setup commands`
- `46788f6` — `docs: record versioned session storage decision`

## Next exact tasks for the next continuation

1. Inspect/observe GitHub Actions for the current `main` head and fix every build/test/lint/security failure before calling the repository release-ready.
2. If a macOS/Xcode host is available, implement the native iOS share/document bridge and verify it with the Compose host.
3. Regenerate the standard Gradle wrapper JAR from a trusted Gradle 9.5.0 installation if wrapper self-containment is required.
4. Capture real release screenshots only after verified Android/Desktop builds run successfully.
5. Configure Android production signing through repository/environment secrets without committing signing material.
6. Perform the final clean-checkout release audit and update this file, `CHANGELOG.md`, `ROADMAP.md`, and release notes with only observed results.

## Release notes draft

TempoTrack's unreleased line is a local-first stopwatch for Android and Desktop with shared Compose Multiplatform domain/UI code, iOS framework readiness, monotonic timing, laps/statistics, persistent searchable named history, validated JSON restore, JSON/CSV export, Android system sharing, Desktop native export selection, versioned/migrating local data, themes/accessibility controls, adaptive navigation, Desktop shortcuts and mini stopwatch, documentation and automated repository quality/security workflows. Release status remains pre-candidate until the newest head is observed passing the full build/test/lint/security matrix and real release assets are captured.
