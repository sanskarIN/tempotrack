# TempoTrack — Work Handoff

## Current milestone

Phase 0 → Phase 6 implementation, platform portability, reliability hardening, release engineering, documentation and release-candidate audit are in progress from the master prompt uploaded on 2026-08-19.

The repository is now a substantial Kotlin + Compose Multiplatform stopwatch implementation for Android/Desktop with a Kotlin/Native iOS framework and native iOS export/share bridges. This handoff records the current `main` state after the latest reliability/platform audit. Environment-gated items are left explicitly open instead of being represented as verified.

## Implementation contract

- Public/open-source TempoTrack stopwatch.
- Kotlin + Compose Multiplatform.
- Android and Desktop primary applications.
- Kotlin/Native/Compose iOS framework/host integration.
- MIT license.
- Visible product credit: **Made by the Sanskar**.
- Business: `sanskarin@outlook.in`.
- Business: `sanskarin.business@gmail.com`.
- Support: `supportramsandesh@gmail.com`.
- GitHub: `https://github.com/sanskarIN`.
- Buy Me a Coffee: `https://buymeacoffee.com/sanskarIN`.
- Small, atomic, meaningful commits are preferred and were used throughout this continuation.
- Requested maintainer Git email remains `sanskarin@outlook.in`; connector-created commit author metadata cannot be guaranteed from the file-write interface, so local maintainers should still configure it explicitly.

## Current implementation

### Core stopwatch

- Start, pause, resume, reset.
- Lap/split recording.
- Millisecond display precision.
- Injected monotonic clock abstraction.
- Elapsed accumulation saturates at `Long.MAX_VALUE` rather than wrapping negative.
- Live lap recording stops at the same maximum accepted by persistence.
- Running checkpoints are rebased at save time so persisted `accumulatedNanos` is the elapsed-at-save lower bound and `startedAtNanos` is the monotonic reading from that same save.
- `StopwatchEngine` accepts an injected wall clock only for checkpoint recovery metadata; live elapsed duration remains purely monotonic.

### Active checkpoint schema v2 and recovery

`StopwatchCheckpoint` now includes nullable `savedAtEpochMillis`.

Active-stopwatch persistence is schema version 2:

- schema v2 round-trips current checkpoint metadata;
- schema v1 envelopes migrate forward;
- original unversioned checkpoints migrate forward;
- unknown future versions fail closed;
- oversized active-store payloads fail closed before decoding;
- active checkpoint validation and encoded-size bounds remain enforced.

Recovery behavior is platform-specific:

- Android uses `SystemClock.elapsedRealtimeNanos()`.
- iOS uses `NSProcessInfo.systemUptime` converted to nanoseconds.
- Android/iOS compare elapsed uptime since save with elapsed wall time since save. If those deltas reasonably agree, a running timer can continue; if uptime/wall references moved backward, disagree beyond tolerance, or legacy metadata is ambiguous, the checkpoint is normalized to PAUSED at the last safely known elapsed value.
- Desktop uses `System.nanoTime()` only inside the current JVM. A persisted RUNNING checkpoint is converted to PAUSED on a new Desktop process instead of comparing incompatible process-local origins.
- Desktop persists a rebased RUNNING checkpoint every five seconds while active, bounding recent elapsed loss after forced process termination.
- Recovery transformations are persisted once during application initialization.
- Legacy checkpoints preserve known lap elapsed time when normalized.

Decision documentation: `docs/adr/0005-platform-checkpoint-recovery.md`.

### Laps/statistics

- Recorded ordering.
- Fastest-first / slowest-first views without mutating recorded order.
- Fastest/slowest descriptors.
- Integer overflow-safe rounded average split calculation; no `Double` conversion is required.
- Sequential index validation.
- Non-negative split/total validation.
- Overflow-safe cumulative validation.
- Explicit negative cumulative-total rejection.
- Lap total may not exceed a saved session duration.

### Session history

- Named local sessions.
- Search.
- Validated rename.
- Delete + undo.
- Newest-first storage.
- Duplicate ID rejection.
- Corrupt stored history fails closed without silently rewriting the original data.
- Bounded stored-session count and encoded character size.
- Session names/IDs/lap records use shared validation contracts.

### Data portability

- Portable JSON export.
- Seven-column CSV export.
- No-lap CSV rows now match the header column count.
- CSV fields escape quotes/commas and neutralize spreadsheet-formula prefixes.
- Validated JSON restore.
- Restore requires explicit replacement confirmation.
- Restore content/session limits are aligned with persistence limits, so a valid self-backup is not rejected only because the importer was configured smaller than storage.
- Large JSON/CSV serialization and JSON import parsing execute off the UI dispatcher.
- Restore submission is single-flight.
- History export/share preparation is single-flight and cannot overlap restore parsing.
- Shared `ExportFileName` sanitization is used by platform file/share operations.

### Android export/share

- MediaStore export on Android 10+.
- App-specific Documents fallback on older supported Android.
- Export directory creation is verified.
- Android system sharing via `ACTION_SEND` chooser.
- Non-exported `FileProvider` exposes only `cache/shared-exports/`.
- Temporary read URI grant instead of raw filesystem paths.
- Share cache creation/path type is verified.
- Coroutine cancellation is preserved instead of being converted to an ordinary write/share failure.
- Platform-unavailable and preparation failures remain distinct.

### Desktop export/share-adjacent UX

- Native `JFileChooser` save destination.
- JSON/CSV filename/filter suggestions.
- Explicit user cancellation result.
- Headless/unavailable chooser path produces a platform-unavailable result.
- Export writes use UTF-8 and preserve coroutine cancellation.

### iOS native export/share

`shared/src/iosMain` now contains real native data-portability bridges:

- `IosTemporaryExportFile.kt` creates a unique operation directory below `NSTemporaryDirectory()` and writes a sanitized UTF-8 source file atomically.
- `IosShareService.kt` presents `UIActivityViewController`, configures a popover source for regular-width/iPad-class presentation, holds one active activity sheet, and cleans temporary operation data when completion/dismissal is reported.
- `IosDocumentExporter.kt` presents `UIDocumentPickerViewController(forExportingURLs:asCopy:)`, strongly retains its delegate for the operation, reports explicit success/cancellation/platform failure, serializes document-picker operations with a mutex, dismisses on coroutine cancellation, and performs non-cancellable temporary-directory cleanup.
- `MainViewController()` wires both native services into the shared app.
- About version metadata is derived from `CFBundleShortVersionString`.
- `iosTest` covers temporary file sanitization, unique operation directories, creation and cleanup.

Native iOS source still requires actual macOS/Xcode compilation/device-or-simulator verification before it can be represented as release-verified.

### Preferences and UX reliability

- First-run onboarding persists before advancing.
- Light/dark/system theme.
- Large controls.
- Reduced-motion preference.
- Desktop mini-stopwatch visibility.
- Desktop keyboard shortcut enable/disable preference.
- Preferences persistence is versioned/migrated and size bounded.
- Settings writes are single-flight; controls temporarily disable while saving.
- Failed Settings writes revert both visible preferences and Desktop side effects.
- Updated failure copy says the setting was reverted.
- Stopwatch session saves are single-flight.
- Saved-session feedback clears after timer/name changes.
- Session-name input uses `SessionValidation.MAX_SESSION_NAME_LENGTH` instead of a duplicated literal.

### Accessibility

- Main timer semantic elapsed-time description.
- Mini stopwatch now exposes the same elapsed-time semantic description.
- Material controls/labels.
- Large-control setting.
- Reduced-motion preference.
- Fastest/slowest meaning is not encoded by color alone.
- Desktop keyboard help and enable/disable option.
- Shared string resources allow accessibility copy to be localized.

### Desktop mini window / shortcuts

- Always-on-top mini stopwatch.
- Persisted visibility.
- Closing the mini window also persists `miniStopwatchVisible = false`, so it does not unexpectedly reopen next launch.
- Space = start/pause/resume.
- L = lap.
- R = reset.
- Shortcut handling respects persisted enable/disable state.
- Shortcut actions persist active checkpoints.

### Atomic private storage

Android and Desktop private string storage now:

- validates the parent path;
- creates required directories explicitly;
- verifies the parent is a directory;
- writes UTF-8 temporary content;
- attempts atomic replace;
- falls back only when the filesystem specifically reports `AtomicMoveNotSupportedException`;
- clears stale sidecar `.tmp` files when the logical store is cleared.

### Gradle/bootstrap integrity

- Project Gradle version: 9.5.0.
- `gradle/wrapper/gradle-wrapper.properties` pins the Gradle 9.5.0 binary distribution SHA-256:
  `553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746`.
- Standard binary `gradle-wrapper.jar` is still not committed.
- Unix `gradlew` and Windows `gradlew.bat` use the wrapper JAR when present.
- When the JAR is absent, launchers require installed Gradle **exactly 9.5.0** and reject a mismatched fallback version.
- Windows bootstrap uses delayed expansion correctly inside its version-detection block.
- A trusted Gradle 9.5.0 installation should generate the standard wrapper JAR if full self-contained wrapper bootstrap is required.

The connected GitHub text/file workflow cannot safely synthesize or transfer this binary JAR without risking corruption, so it is deliberately not fabricated.

### Android release signing

- Build script supports environment-backed release signing.
- Partial signing configuration fails immediately.
- Configured keystore path must be a real file.
- Tag release workflow requires protected signing secrets.
- Base64 keystore is decoded only into runner temporary storage.
- Password/alias secrets are scoped to the build step that needs them.
- Release job builds APK + AAB.
- Release job verifies both output types exist.
- Publish job includes APK/AAB/Desktop/iOS artifacts and `SHA256SUMS.txt`.
- Build jobs use read-only repository permission; only the publish job receives `contents: write`.
- Release tags must match `vMAJOR.MINOR.PATCH`.
- Release workflows serialize by tag.

Actual production signing secrets still have to be provisioned in protected repository/environment settings by the repository owner/admin.

### CI/security/repository automation

- Main/PR CI for shared/Desktop/Android/iOS/docs.
- Superseded CI runs cancel by branch/PR.
- iOS simulator framework link + simulator test jobs are configured on macOS.
- Markdown local-link checker.
- CodeQL.
- Dependency review.
- Secret scanning with read-only permissions, timeout and superseded-run cancellation.
- Dependabot.
- Issue templates.
- Pull request template.
- Release workflow.

## Important files added during the continuation series

- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/SessionValidation.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/StopwatchCheckpointRecovery.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionImport.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionStoreCodec.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ExportFileName.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ShareService.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/util/SuspendResult.kt`
- `shared/src/commonMain/composeResources/values/shortcuts.xml`
- `shared/src/commonMain/composeResources/values/reliability.xml`
- `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosPlatformAdapters.kt`
- `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/MainViewController.kt`
- `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosTemporaryExportFile.kt`
- `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosShareService.kt`
- `shared/src/iosMain/kotlin/in/sanskar/tempotrack/ios/IosDocumentExporter.kt`
- `shared/src/iosTest/kotlin/in/sanskar/tempotrack/ios/IosTemporaryExportFileTest.kt`
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/StopwatchJourneyTest.kt`
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/StopwatchCheckpointRecoveryTest.kt`
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/domain/LapStatisticsTest.kt`
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/ExportFileNameTest.kt`
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/util/SuspendResultTest.kt`
- `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidShareService.kt`
- `androidApp/src/main/res/xml/file_paths.xml`
- `docs/ios.md`
- `docs/adr/0004-versioned-session-storage.md`
- `docs/adr/0005-platform-checkpoint-recovery.md`

## Significant files changed during this continuation series

- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/Models.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/StopwatchEngine.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/domain/StopwatchCheckpointValidation.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionRepository.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/SessionCodec.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/PreferencesRepository.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/PreferencesStoreCodec.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ActiveStopwatchRepository.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/data/ActiveStopwatchStoreCodec.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/TempoTrackDependencies.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/TempoTrackApp.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/MiniStopwatch.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/StopwatchScreen.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/HistoryScreen.kt`
- `shared/src/commonMain/kotlin/in/sanskar/tempotrack/ui/screens/SettingsScreen.kt`
- shared Compose resources.
- common persistence/domain regression tests.
- `androidApp/build.gradle.kts`
- `androidApp/src/main/AndroidManifest.xml`
- `androidApp/src/main/kotlin/in/sanskar/tempotrack/MainActivity.kt`
- `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidExporter.kt`
- `androidApp/src/main/kotlin/in/sanskar/tempotrack/AndroidStringStorage.kt`
- Android backup/FileProvider resources.
- `desktopApp/build.gradle.kts`
- `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/Main.kt`
- `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/DesktopExporter.kt`
- `desktopApp/src/main/kotlin/in/sanskar/tempotrack/desktop/JvmStringStorage.kt`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`
- `.github/workflows/secret-scan.yml`
- `README.md`
- `PRIVACY.md`
- `CHANGELOG.md`
- `ROADMAP.md`
- `docs/setup.md`
- `docs/architecture.md`
- `docs/performance.md`
- `docs/testing.md`
- `docs/release.md`
- `docs/github.md`
- `docs/accessibility.md`
- `what_changed.md`

## Latest reliability/audit commit themes

The latest continuation intentionally used granular commits rather than one bulk change. Commit messages include:

- `fix: avoid implicit button lambda return label`
- `fix: serialize history export and share launches`
- `a11y: describe mini stopwatch elapsed time`
- `fix: align settings failure copy with rollback`
- `docs: describe checkpoint v2 recovery architecture`
- `docs: record platform checkpoint recovery decision`
- `docs: document checkpoint heartbeat performance`
- `docs: expand checkpoint recovery verification`
- `docs: document resilient checkpoint recovery`
- `docs: clarify exact Gradle bootstrap requirements`
- `docs: record checkpoint v2 and UI reliability work`
- `docs: align release guide with native iOS export`
- `fix: use explicit iOS document export initializer`
- `feat: timestamp persisted stopwatch checkpoints`
- `feat: record wall time with engine checkpoints`
- `fix: validate persisted checkpoint wall timestamps`
- `feat: migrate active stopwatch schema to v2`
- `test: cover active checkpoint v1 migration`
- `feat: detect uptime clock resets safely`
- `test: cover uptime reboot detection`
- `refactor: attach wall clock to checkpoint metadata`
- `fix: detect Android uptime resets on restore`
- `fix: detect iOS uptime resets on restore`
- `test: cover checkpoint wall metadata`
- `feat: add safe running checkpoint recovery policy`
- `test: cover safe process restart recovery`
- `feat: expose checkpoint recovery policy`
- `feat: apply platform checkpoint recovery on launch`
- `fix: pause Desktop timers across JVM restarts`
- `feat: expose checkpoint heartbeat interval`
- `feat: persist running checkpoint heartbeats`
- `feat: enable Desktop checkpoint heartbeat`
- `fix: allow arbitrary monotonic timestamp origins`
- `fix: preserve legacy running checkpoints`
- `fix: rebase running checkpoints at persistence time`
- `test: accept negative monotonic clock origins`
- `test: cover rebased running checkpoints`
- `fix: saturate stopwatch elapsed overflow`
- `test: cover elapsed overflow saturation`
- `fix: compute lap averages without floating point`
- `test: cover overflow-safe lap averages`
- `fix: make session lap validation overflow-safe`
- `test: cover negative lap totals`
- `fix: serialize preference writes from Settings`
- `fix: serialize stopwatch session saves`
- `fix: align backup restore limits with storage`
- `test: lock restore limits to persistence contract`
- `fix: align active checkpoint lap limit`
- `test: lock checkpoint lap limit to session limit`
- `fix: bound active stopwatch persistence size`
- `fix: bound preference persistence size`
- `test: reject oversized preference payloads`
- `fix: keep empty-lap CSV rows schema-aligned`
- `test: verify empty-lap CSV column count`
- `perf: move history serialization off UI thread`
- `fix: persist mini stopwatch close state`
- `fix: preserve Android export cancellation`
- `fix: preserve Desktop export cancellation`
- `fix: verify Android share cache creation`
- `fix: harden Android atomic string storage`
- `fix: harden Desktop atomic string storage`
- `build: validate fallback Gradle version on Unix`
- `build: validate fallback Gradle version on Windows`
- `fix: use delayed expansion in Windows bootstrap`
- `build: pin Gradle distribution checksum`
- `ci: validate semantic release tags`
- `ci: serialize release workflow per tag`
- `ci: cancel superseded branch verification`
- `ci: cancel superseded secret scans`
- `feat: add native iOS file share service`
- `refactor: centralize iOS temporary export files`
- `fix: isolate iOS temporary export files`
- `fix: clean up iOS share files after dismissal`
- `feat: add native iOS document exporter`
- `fix: harden iOS export destination label`
- `feat: wire native iOS document export`
- `test: cover iOS temporary export files`

This list is intentionally detailed because the project instruction asks for many small commits and a durable handoff instead of chat-heavy narration.

## Verification performed

- Current repository tree and relevant source/config/document files were inspected through the connected GitHub API.
- Changed files were re-fetched before sequential writes when a current blob SHA was required.
- Repository search for `TODO`, `FIXME`, `runCatching`, stale iOS-export placeholder wording, and related obsolete terms returned no indexed results in the final sweep used for this handoff.
- Earlier stale-SHA conflicts were resolved by re-fetching current content instead of force-overwriting concurrent repository changes.
- GitHub combined-status checks have not exposed a usable passing status matrix for the latest push commits through this connector.
- A clean local clone/build attempt from the execution container could not resolve `github.com`, so a full Gradle dependency/build/test run could not be executed in that container.
- The native iOS bridge was source-audited and `iosTest` coverage was added, but no macOS/Xcode compiler result is claimed from this chat environment.

## Verification integrity / current release status

This handoff does **not** claim that the newest `main` commit has passed the complete Android/Desktop/iOS build, lint, test, packaging and manual-device matrix.

Configured automation and tests are extensive, but results that were not actually observable remain **not observed**, not “passed”.

Before declaring a release candidate, observe/execute:

```bash
./gradlew quality
./gradlew :androidApp:testDebugUnitTest :androidApp:lintRelease :androidApp:assembleRelease :androidApp:bundleRelease
./gradlew :desktopApp:test :desktopApp:compileKotlin :desktopApp:packageDistributionForCurrentOS
python tools/check_markdown_links.py
```

On macOS with Xcode:

```bash
./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkReleaseFrameworkIosArm64
```

Also complete the manual lifecycle/export/share/accessibility checks documented in `docs/testing.md`.

## Remaining externally gated items

These items cannot be honestly completed only by editing repository text/source through the current connector:

1. **Standard Gradle wrapper binary** — `gradle-wrapper.jar` remains absent. It should be generated from a trusted Gradle 9.5.0 installation; wrapper properties already pin the distribution checksum and launchers enforce the exact fallback version.
2. **Production Android signing secrets** — source/workflow support is complete, but repository/environment secrets must be provisioned by an authorized admin before a distributable Android tag release.
3. **Observed CI/build result** — GitHub Actions result contexts for the newest pushes have not been exposed through the connected status interface here.
4. **macOS/Xcode Native verification** — iOS framework, document picker, activity sheet, delegate lifetimes and simulator/device lifecycle behavior need an actual macOS/Xcode run.
5. **Real release screenshots** — must be captured from verified builds, not fabricated or represented by placeholders.
6. **Manual device/accessibility verification** — Android TalkBack/font scaling, Desktop keyboard/focus behavior, iOS picker/share presentation, reboot/restart lifecycle recovery and large-history responsiveness need target-host execution.

Optional roadmap items such as per-action Desktop key rebinding and encrypted backup remain intentionally optional and should not be implemented merely to mark boxes without user demand/threat-model justification.

## Repository state conclusion

Source-level functional gaps identified during this continuation have been addressed with atomic commits, regression tests, platform-specific recovery rules, native iOS data portability, safer storage, bounded persistence/import contracts, single-flight UI writes, release-signing support and synchronized documentation.

The remaining blockers are verification/environment/credential/binary-artifact tasks rather than knowingly unfinished `TODO`/`FIXME` source work.
