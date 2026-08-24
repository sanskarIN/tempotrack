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
- Repository writes are serialized with a mutex.
- Identical upserts do not rewrite the session store.
- Renames that normalize to the existing session name do not rewrite the session store.
- Deletes for missing session IDs do not rewrite the session store.
- Full-history replacement skips persistence when the normalized replacement is already identical to current history.
- History delete, undo, and rename actions are single-flight at the UI boundary.
- History mutations cannot overlap export/share/restore preparation, and conflicting controls disable while a mutation is active.
- Rename input, save, cancel, and dialog dismissal are locked while the rename mutation is running.

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
- Export/share/restore actions are also blocked while a history mutation is active.
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
- `shared/src/commonTest/kotlin/in/sanskar/tempotrack/data/SessionRepositoryTest.kt`
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

- `perf: skip no-op session renames`
- `perf: avoid writes for missing session deletes`
- `test: cover no-op session rename persistence`
- `test: cover missing session delete persistence`
- `perf: skip identical session upserts`
- `test: cover identical session upsert persistence`
- `fix: serialize history mutation actions`
- `docs: document no-op history write avoidance`
- `docs: expand history persistence regression checks`
- `perf: skip identical history replacements`
- `test: cover identical history replacement persistence`
- `docs: record history reliability improvements`
- `docs: cover unchanged history restore writes`
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

## Continuation checkpoint — 2026-08-19 14:32 IST

### Repository/history hardening completed

- `JsonSessionRepository.upsert` now loads the normalized current history and skips persistence if the resulting model list is unchanged.
- `JsonSessionRepository.rename` returns success without serializing/writing when trimming the requested name yields the existing name.
- `JsonSessionRepository.delete` skips persistence when the requested ID does not exist.
- `JsonSessionRepository.replaceAll` validates and normalizes the replacement, loads the current validated history, and skips persistence if both are equal.
- `SessionRepositoryTest` tracks write counts in its in-memory storage helper and now locks all four no-op persistence contracts with regression tests.
- `HistoryScreen` now tracks `historyMutationInProgress` independently of restore/export/share state.
- Delete, undo, and rename now run through one `launchHistoryMutation` single-flight boundary.
- History mutation controls disable during a mutation.
- Export/share/restore controls disable while a history mutation is running.
- Data portability launch guards reject work while a history mutation is running.
- Rename dialog editing, dismissal, save, and cancel are disabled while the rename persistence operation is active.
- Performance, testing, and changelog documentation now describe these guarantees.

### Audit correction recorded

A repository code-search query did not surface the existing `ConcurrentWriteDetectingStorage.kt` helper, so the first audit pass incorrectly treated the helper as missing and created commit `afe9de6556377b92a991c276f388f07b94322851` (`fix: restore concurrent session storage test helper`).

A directory-level source inspection immediately found the existing dedicated helper file. Commit `8f45f5b5641409283c14066949af2d61124a6711` (`fix: remove duplicate session test helper`) restored `SessionRepositoryTest.kt` to the correct structure before subsequent reliability changes. The final repository does not contain the duplicate helper introduced by that false positive.

This correction is intentionally retained in the handoff rather than hidden so later maintainers can understand the two adjacent commits.

### New commits in this continuation

- `afe9de6556377b92a991c276f388f07b94322851` — `fix: restore concurrent session storage test helper` — audit false positive; corrected immediately by the next commit.
- `8f45f5b5641409283c14066949af2d61124a6711` — `fix: remove duplicate session test helper` — restores the pre-audit helper layout.
- `d41ae5bffcd9c2274be1f46d8a28cfe9c5ea2dd3` — `perf: skip no-op session renames`.
- `b3ecca42df46641b306530403e4ebc1985bba767` — `perf: avoid writes for missing session deletes`.
- `3f0343e3a8024a49ef1e205f576bde6f03534a95` — `test: cover no-op session rename persistence`.
- `af113fad5d7a79d5815a9eb61be0ab72535f9975` — `test: cover missing session delete persistence`.
- `f9b9bcee5c948985212e1f81e4534bb1fb9d43cd` — `perf: skip identical session upserts`.
- `3a22e68688f199e752b0ea1d096b3fa164a001c0` — `test: cover identical session upsert persistence`.
- `801c787d33b22e876495b284ea6237f5245b3f21` — `fix: serialize history mutation actions`.
- `7c63bf13ad08560c18ca33f1145505fd5abeceda` — `docs: document no-op history write avoidance`.
- `5719ea6346d66d8b462bd3f17bc428fa6ba330b8` — `docs: expand history persistence regression checks`.
- `6ebf113080062119d6545c662e8d8b130306e1f6` — `perf: skip identical history replacements`.
- `f467c21c1978b6d80fff075e1cd13bc3252c8b33` — `test: cover identical history replacement persistence`.
- `e6e9d30a6910a34bdcc711b6a80134aafea0b9a0` — `docs: record history reliability improvements`.
- `3c27c46745f236187f6f5e237ac38617a9d8dd78` — `docs: cover unchanged history restore writes`.

The commit that updates this handoff follows the list above and should be treated as the documentation checkpoint for this continuation.

## Verification performed

- Current repository tree and relevant source/config/document files were inspected through the connected GitHub API.
- Changed files were re-fetched before sequential writes when a current blob SHA was required.
- `HistoryScreen.kt` was re-fetched after the mutation-serialization commit, including the modified operation guards, rename dialog, and `SessionCard` signature/control wiring, to verify that the intended source structure was present in `main`.
- `SessionRepository.kt` and `SessionRepositoryTest.kt` were re-fetched during the continuation before subsequent sequential edits.
- Repository search for `TODO`, `FIXME`, `runCatching`, stale iOS-export placeholder wording, and related obsolete terms returned no indexed results in the final sweep used for this handoff.
- Earlier stale-SHA conflicts were resolved by re-fetching current content instead of force-overwriting concurrent repository changes.
- GitHub combined-status checks have not exposed a usable passing status matrix for the latest push commits through this connector; a queried newest-commit status returned no status entries.
- A clean local clone/build attempt from the execution container could not resolve `github.com`, so a full Gradle dependency/build/test run could not be executed in that container.
- No Gradle test/lint/package command is represented as passing from this continuation because none was actually observable through this execution environment.
- The native iOS bridge was source-audited and `iosTest` coverage was added in earlier continuation work, but no macOS/Xcode compiler result is claimed from this chat environment.

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

Also complete the manual lifecycle/export/share/accessibility checks documented in `docs/testing.md`, including the new History mutation single-flight checks.

## Remaining externally gated items

These items cannot be honestly completed only by editing repository text/source through the current connector:

1. **Standard Gradle wrapper binary** — `gradle-wrapper.jar` remains absent. It should be generated from a trusted Gradle 9.5.0 installation; wrapper properties already pin the distribution checksum and launchers enforce the exact fallback version.
2. **Production Android signing secrets** — source/workflow support is complete, but repository/environment secrets must be provisioned by an authorized admin before a distributable Android tag release.
3. **Observed CI/build result** — GitHub Actions result contexts for the newest pushes have not been exposed through the connected status interface here.
4. **macOS/Xcode Native verification** — iOS framework, document picker, activity sheet, delegate lifetimes and simulator/device lifecycle behavior need an actual macOS/Xcode run.
5. **Real release screenshots** — must be captured from verified builds, not fabricated or represented by placeholders.
6. **Manual device/accessibility verification** — Android TalkBack/font scaling, Desktop keyboard/focus behavior, iOS picker/share presentation, reboot/restart lifecycle recovery and large-history responsiveness need target-host execution.

Optional roadmap items such as per-action Desktop key rebinding and encrypted backup remain intentionally optional and should not be implemented merely to mark boxes without user demand/threat-model justification.

## Next exact tasks

When an environment capable of running the complete toolchain is available, continue in this order:

1. Generate/verify the standard Gradle 9.5.0 wrapper JAR from a trusted installation and commit the binary only after verifying the official wrapper/distribution chain.
2. Run `./gradlew quality` and fix any compiler, ktlint, unit-test, or Android Lint failure before adding new product scope.
3. Run Android debug/release assembly and bundle checks with a real Android SDK.
4. Run Desktop test/compile/package checks on each intended packaging host.
5. Run the iOS simulator tests and debug/release framework link tasks on macOS/Xcode; fix any Kotlin/Native/UIKit interop compiler errors before a release tag.
6. Exercise History delete/undo/rename repeatedly during slow or instrumented storage writes to confirm the new control locking and single-flight behavior manually.
7. Exercise large-history export/share/restore while attempting conflicting mutations to confirm no overlapping operation reaches the repository/platform boundary.
8. Provision protected Android production signing secrets using repository/environment controls; do not commit keystore material or passwords.
9. Create a semantic test tag only after the build matrix is observed green, inspect generated APK/AAB/Desktop/iOS/checksum artifacts, then remove/replace the tag if it was only a release rehearsal.
10. Capture real Android/Desktop/iOS screenshots only from verified builds and then close the screenshot roadmap item.

## Repository state conclusion

Source-level functional gaps identified during this continuation have been addressed with atomic commits, regression tests, platform-specific recovery rules, native iOS data portability, safer storage, bounded persistence/import contracts, single-flight UI writes, no-op history write avoidance, serialized History mutations, release-signing support and synchronized documentation.

The remaining blockers are verification/environment/credential/binary-artifact tasks rather than knowingly unfinished `TODO`/`FIXME` source work.

---

## Continuation checkpoint — 2026-08-19 comprehensive documentation and compile-syntax audit

### Critical Kotlin namespace compile blocker fixed

A direct Kotlin syntax audit identified a repository-wide compile blocker that earlier source-only review had not surfaced: the runtime namespace begins with `in.sanskar...`, but `in` is a Kotlin keyword. Kotlin source written as `package in.sanskar...` or `import in.sanskar...` is invalid syntax.

The intended runtime/application namespace was preserved. Every known Kotlin production/test source was corrected to use escaped source syntax:

```kotlin
package `in`.sanskar.tempotrack...
import `in`.sanskar.tempotrack...
```

This correction was applied across:

- Android application entry/adapters/staging tests;
- all shared common domain/data/UI/utility production files;
- all shared common tests;
- iOS Kotlin/Native production and simulator-test files;
- Desktop entry/export/storage files.

Residual GitHub source searches for the exact unescaped declaration/import forms returned no indexed matches after the correction.

A repository-local guard was added at `tools/check_kotlin_package_keywords.py`, and `.github/workflows/ci.yml` runs it in the documentation job so this specific syntax regression cannot silently return.

### Stale active-schema regression assertion fixed

During the namespace test pass, `ActiveStopwatchRepositoryTest` was found still asserting an active-store schema-v1 envelope even though production persistence had already moved to schema v2.

The test now expects schema v2 both for current saves and migrated legacy checkpoints. The dedicated codec test continues to verify schema-v1 migration into the current schema.

### Android data-portability hardening completed

Android sharing/export was audited beyond the earlier baseline:

- `AndroidShareService` now attaches the granted `content://` URI through both `EXTRA_STREAM` and `ClipData` while retaining `FLAG_GRANT_READ_URI_PERMISSION`.
- Each share operation stages a unique cache file, preventing a later share from overwriting bytes behind an earlier recipient's granted URI.
- Share staging handles very short sanitized filenames without violating Java temporary-file prefix requirements.
- If coroutine cancellation races chooser launch, the staged file is intentionally retained because a target application may already possess/read the URI.
- Android 10+ MediaStore export now checks that the `IS_PENDING=0` finalization update affects exactly one row; failure deletes the incomplete item and reports write failure.
- Pre-Android-10 app-specific exports now reserve collision-safe filenames (`name.ext`, `name (1).ext`, …) with `createNewFile()` instead of overwriting an existing backup.
- Filesystem-only staging logic was extracted to `AndroidStagingFiles.kt` for local JVM testing.
- `AndroidExportStagingTest.kt` covers direct reservation, collisions, extensionless names, bounded exhaustion, and preservation of existing bytes.
- `AndroidShareStagingTest.kt` covers unique per-operation files, extension retention, and short filenames.
- JUnit 4.13.2 was added to the version catalog/Android local-test dependency.
- `PRIVACY.md` and `docs/testing.md` document the unique-cache-file behavior and manual Android verification cases.

### Comprehensive documentation set added

The documentation is now organized around `docs/README.md`, with role-based reading paths for users, contributors, maintainers, persistence/recovery work, and release maintainers.

New deep guides:

- `docs/repository-reference.md` — exhaustive tracked-file inventory with responsibility/maintenance notes for root files, GitHub automation, Gradle metadata, Android source/resources/tests, Desktop source, shared production/tests/resources, iOS source/tests, tools, assets, ADRs, and all documentation.
- `docs/code-reference.md` — source/API reference for clocks, models, engine, validation/recovery, repositories/codecs, shared Compose UI, Android/Desktop/iOS adapters, tests, and Python tools.
- `docs/state-and-recovery.md` — state machine, pause/resume arithmetic, lap semantics, overflow behavior, checkpoint rebasing, active schema v2, Android/iOS uptime-vs-wall recovery, Desktop process-local recovery, heartbeat behavior, startup normalization, and failure matrix.
- `docs/data-model-and-storage.md` — models, persistence limits, session/preference/active schemas, migration policies, JSON backup, restore validation order, CSV schema/formula safety, platform storage locations, atomic writes, concurrency, corruption philosophy, and migration checklist.
- `docs/platforms.md` — Android/Desktop/iOS capability matrix and detailed platform-specific clock/storage/export/share/recovery/packaging behavior.
- `docs/user-guide.md` — complete user workflow for onboarding, stopwatch, laps, session saving, History, export/share/restore, Settings, recovery, privacy, and accessibility.
- `docs/maintainer-guide.md` — safe change recipes for domain rules, preferences, schemas, limits, restore/CSV, Android/Desktop/iOS adapters, localization, accessibility, dependencies, versions/signing, release preparation, documentation, and handoff discipline.
- `docs/build-and-ci.md` — Gradle modules/toolchain versions, bootstrap behavior, module builds, deterministic checks, CI jobs, CodeQL/dependency review/secret scan/Dependabot, release jobs, signing secrets, artifact publishing, and CI integrity rules.
- `docs/security-model.md` — trust boundaries, persistence integrity, malformed import controls, CSV formula injection, filename/path handling, Android FileProvider/MediaStore, iOS staging, atomicity, concurrency, cancellation, backup, signing, supply chain, and security review checklist.

Existing documentation was also deeply synchronized:

- root `README.md` now links the comprehensive docs and includes the repository integrity checks;
- `docs/development.md` now documents the namespace rule, quality workflow, persistence/concurrency/platform boundaries, and documentation ownership;
- `docs/setup.md` now covers Git/Python guards, exact Gradle bootstrap state, Android/Desktop/iOS setup, signing, IDE recommendations, and validation order;
- `docs/troubleshooting.md` was substantially expanded and corrected for package syntax, wrapper/JDK/SDK failures, current checkpoint recovery, durable-history corruption behavior, Android export/share, iOS native presentation, settings rollback, signing, and CI status interpretation;
- `docs/testing.md` now documents the namespace guard, exhaustive tracked-file coverage guard, Android staging JVM tests, current schema-v2 expectations, and complete manual platform matrix;
- `docs/release.md` now includes all deterministic repository guards and a documentation release audit;
- `docs/github.md` now describes the actual CI/security/release automation, repository-reference policy, namespace policy, secrets, permissions, artifacts, branch protection, PR expectations, and missing-status troubleshooting;
- `CONTRIBUTING.md` and `.github/pull_request_template.md` now require all repository integrity checks and explicit documentation coverage for tracked-file changes;
- `ROADMAP.md` now records the comprehensive documentation/maintainability milestone and keeps observed clean-checkout execution explicitly open;
- `CHANGELOG.md` now records the namespace compile fix, schema-v2 assertion correction, Android portability hardening, exhaustive documentation, and new guards.

### “Do not skip files” is now mechanically enforced

A new tool, `tools/check_repository_reference.py`, uses `git ls-files` as the source of truth and requires every tracked path to appear exactly in backticks in `docs/repository-reference.md`.

CI now compiles and runs:

```bash
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

The repository reference itself includes the coverage checker and all documentation added in this continuation. This turns exhaustive file documentation from a manual convention into a CI contract.

### Documentation commits in this continuation

- `409575830d3883a30bb4111865cafce4d416517b` — `docs: add complete documentation index`.
- `2945df6775a0d1164848e6692e4cc2c459e337cc` — `docs: document every repository file`.
- `978fd6a249ffb8ebdeee007a68a5f55e80efee00` — `docs: add deep source code reference`.
- `4a68fa0c18d52013c3db06cc11b3f096bb11df65` — `docs: explain stopwatch state and recovery model`.
- `1cf6bc0693adcda057919ac5bd02ee761693fe41` — `docs: document data model and storage lifecycle`.
- `37d698dff57532d1f1068c062c42b472a2e971f5` — `docs: document platform-specific behavior`.
- `d61ecf9c8bff0131f98887c7120328b06aa7fa44` — `docs: add complete user guide`.
- `817d0ef83aa15c6fd6eec06f58ee5ecec7702ffc` — `docs: add deep maintainer workflow guide`.
- `971e48ca670a16de4932687004c4293b01276398` — `docs: document build and CI system`.
- `f6ebd599e99aa5c84f8c31bff88772d55f517ab1` — `docs: add detailed security model`.
- `de6588f04dda751c2acd2ccdd61647579a3be8e0` — `docs: expand documentation navigation`.
- `2551efa604449961dcf99a418488935fe19c261e` — `docs: link comprehensive project documentation`.
- `8dd36956e7bc962d9a30563672ce93ba0a1aeaa7` — `docs: deepen development workflow guidance`.
- `8045a87479894971e5d7a5562f0eeb8d3eafeebd` — `docs: correct and expand troubleshooting guidance`.
- `72b45588c4e98ca2ac679abbcdd654ebceae0afb` — `docs: expand development environment setup`.
- `f6e478b69524bb7609d480f0bc29bda15eafad4c` — `tools: verify repository file documentation coverage`.
- `ae6258aab48b735890b1a26b0eda0e45793eedc0` — `docs: keep tracked file reference exhaustive`.
- `1befd51b1ecaf055f706ba01ca448ce7d1bbd6b8` — `ci: enforce repository documentation coverage`.
- `0b288d1267a982b56c8d3e72b872586db605e0bb` — `docs: enforce exhaustive repository documentation checks`.
- `6ecb7324628ab8e31bd6941401117105532fade6` — `docs: document tracked file coverage CI`.
- `9a4c9b2440f7c9b9f4f99222c19226089810c3b6` — `docs: add exhaustive file coverage guard to index`.
- `b0ab3feeb82a2ec257157f0ae8c37bd4040b2508` — `docs: require repository integrity guards for contributions`.
- `09d2272fdfd7a5ff0825f6a13cf8f042518bb7f0` — `docs: expand pull request verification checklist`.
- `d4ab094d758e07c419507dd2f5ef4f717c727426` — `docs: include exhaustive documentation guard in quick checks`.
- `d97771ca2e4ac6d13e18ec7d41fa5ac3f2678a2e` — `docs: add repository integrity checks to release gate`.
- `8c01cd7b7f202acb5f495fb35b1befc149c5103d` — `docs: document current repository automation`.
- `cc74b35eba92c1b0c4a2542a52a8538255af6aa4` — `docs: include repository reference guard in setup`.
- `c36a08dcd324ba9318aa9d48ce63080bd2de2ce7` — `docs: record comprehensive documentation milestone`.
- `4802d4b7d508fae7735c577c72a16c785d6627b5` — `docs: record namespace Android and documentation hardening`.

### Namespace/guard commit highlights

The namespace correction was intentionally granular. Representative/critical commits include:

- `6b8ec25fb89d7fa5ecd8819e9329200d663475c3` — `tools: detect unescaped Kotlin package keywords`.
- `d6cbdc565de520e5a256f8049c7d624ea2ec961e` — `ci: reject unescaped Kotlin package keywords`.
- `fc3e603f742f02028fc85709f15df808ba5bc5c9` — `docs: document Kotlin package keyword guard`.
- `759b5463f4c5b78e9cc16fca5a06cc70c8597d0b` — `test: align active repository expectations with schema v2`.
- `7de8ab6983535ef7dd7abc73d44af0ff796d66aa` — Desktop entry-point keyword fix.
- `34ddbe17ef6f538c16c7a4448902d3bbcd81e5c8` — iOS entry-point keyword fix.
- `99fb820264fed887bd3f96e14c0e51b97795fc0` — History screen keyword fix.
- `69f598ef842494619bd3f96e14c0e51b97795fc0` — Stopwatch screen keyword fix.
- `3d71996dd018dada5a3a4a88669e351f66e00603` — shared app-shell keyword fix.
- `4f7fe4ce1c7473d564cbf8e9451952d6655c272a` — session repository keyword fix.
- `fa44269af4c7f61adf9138558925eca0eb48a7d2` — stopwatch engine test keyword fix.

The commit history contains the remaining per-file keyword fixes for domain, data, UI, tests, Android, iOS, and Desktop source sets.

### Verification observed in this continuation

Observed directly through the connected GitHub source/tree interfaces:

- A fresh recursive repository tree was inspected after the documentation additions.
- The new documentation files and repository-reference checker are present in `main`.
- The repository reference was rebuilt from the tracked-tree inventory and now explicitly lists the new documentation and checker paths.
- Residual GitHub source searches after the namespace correction returned no indexed exact unescaped `package in.sanskar...` or `import in.sanskar...` directives.
- Current `CHANGELOG.md`, `ROADMAP.md`, setup/testing/release/GitHub/contributor/PR documentation was re-read before synchronization writes.
- The latest queried commit combined-status endpoint returned an empty status list; no CI success is inferred from that.

Attempted execution-container verification:

```text
git clone --depth 1 https://github.com/sanskarIN/tempotrack.git /tmp/tempotrack-audit
```

failed with:

```text
Could not resolve host: github.com
```

Therefore the clean-checkout Python guards and Gradle tasks could not actually execute in that container. They remain configured and documented, but this handoff does **not** mark them as observed passing.

### Current deterministic verification commands

From a real clean Git checkout with network/toolchain access:

```bash
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

Then:

```bash
./gradlew quality
./gradlew :androidApp:testDebugUnitTest :androidApp:lintRelease :androidApp:assembleRelease :androidApp:bundleRelease
./gradlew :desktopApp:test :desktopApp:compileKotlin :desktopApp:packageDistributionForCurrentOS
```

On macOS/Xcode:

```bash
./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkReleaseFrameworkIosArm64
```

### Remaining externally gated work after the documentation pass

1. Generate and verify the standard Gradle 9.5.0 wrapper JAR from a trusted installation if self-contained wrapper bootstrap is required.
2. Observe all three repository-local Python guards from a clean checkout; the current execution container could not resolve GitHub for cloning.
3. Observe the full Gradle quality/Android/Desktop build matrix on a capable host/CI run.
4. Observe iOS simulator tests/framework linking on macOS/Xcode and manually exercise native document picker/activity-sheet lifecycle paths.
5. Provision protected production Android signing secrets before a distributable Android tag release.
6. Capture real Android/Desktop/iOS release screenshots from verified builds.
7. Complete target-device accessibility/lifecycle/large-history testing.

### Documentation state conclusion

TempoTrack now has both broad and deep documentation rather than only a README-level overview. The repository contains an indexed user/contributor/maintainer documentation set, an exhaustive tracked-file reference, detailed source/data/state/platform/security/build guides, corrected troubleshooting, synchronized contribution/release guidance, and CI enforcement that prevents future tracked files from being silently omitted from the repository reference.

The authoritative current file inventory is `docs/repository-reference.md`; the historical file lists earlier in this handoff are intentionally preserved as prior checkpoints and may refer to structures that were subsequently consolidated or renamed.

---

## Final maintenance checkpoint — 2026-08-19 evening IST

### Build and CI correctness fixes

- Root ktlint now excludes `**/generated/**`, so generated Compose/resource Kotlin cannot fail repository-owned source style checks.
- Main CI, CodeQL and tagged release Android jobs now use `android-actions/setup-android@v4`, whose current command-line tools can resolve Android SDK Platform 37 used by the project.
- Primary GitHub Actions were moved to maintained Node 24-compatible majors: checkout v7, setup-java v5, setup-python v6, CodeQL v4, dependency-review v5, upload-artifact v7 and download-artifact v8.
- `gradle/actions/setup-gradle` is intentionally kept on the v5 line. The v6 line introduces a separately licensed proprietary caching component, so the open-source repository does not adopt it by default.
- Dependabot ignores only `gradle/actions` 6.x and no longer requests a `dependencies` label that is absent from the repository.

### Dependency maintenance

- Android Gradle Plugin updated from 9.3.0 to 9.3.1.
- Compose Multiplatform updated from 1.11.0 to 1.11.1.
- Gradle itself remains pinned to 9.5.0 so the wrapper properties, bootstrap scripts, CI installation and documentation remain internally consistent.
- Dependabot PR #4 for Gradle 9.7.0 remains open intentionally because that upgrade changes wrapper artifacts and must be verified as one coherent toolchain update rather than partially hand-edited.

### Documentation synchronization

- `docs/build-and-ci.md` now records the current AGP/Compose versions, generated-source lint ownership, Android SDK setup fix, Node 24 action policy, Gradle Actions v5 licensing decision, Dependabot behavior and wrapper-upgrade atomicity requirement.
- `CHANGELOG.md` records the same build, CI, dependency and generated-source fixes.
- This `what_changed.md` history has been preserved in full; this checkpoint is appended rather than replacing earlier handoff records.

### Repository maintenance cleanup

- Obsolete/superseded Dependabot PRs for AGP, Compose, dependency-review, checkout, Android setup, setup-java, upload-artifact and CodeQL were closed after their equivalent updates landed directly on `main`.
- The Gradle Actions v6 PR was closed because the repository intentionally stays on the v5 line.
- The old `audit/release-candidate` PR was closed because it targeted the pre-fix CI/toolchain state and was hundreds of commits behind current `main`.
- No open source-code TODO/FIXME gap was found in the final indexed search, and no open repository issue was present during this audit.

### Granular commits created in this final pass

- `6ef47382e1c529683982295f0b295af27305fab7` — `fix: exclude generated Kotlin from ktlint`.
- `2f4e95c97e06a2a97be6a0d123968a6e6e468d53` — `ci: use Android setup action v4`.
- `a35f251c65ec6be152da9b09abb7d42b66112da5` — `ci: move primary actions to Node 24 majors`.
- `028046785786eb245119dc21a9c5b63f04de9e10` — `ci: modernize CodeQL workflow actions`.
- `7cee3f97508f0e2a71d8d9d32d7991ff1c716cdd` — `ci: modernize release workflow actions`.
- `74bc855ad2c700fed25b793733d7926cc208cf1f` — `ci: modernize dependency review actions`.
- `798fc6376d229fa94630e9b9b05db2e003dbc1fb` — `ci: modernize secret scan checkout`.
- `d232aa1da84936578e843602d3665b0746068c16` — `chore: remove invalid Dependabot labels`.
- `c7c4f1d48fb3484e1c0a812fa56b106e89625bfd` — `ci: update Python setup action`.
- `4682837b5a5dd1c8dde0d4271de5e2be3d340500` — `ci: update artifact download action`.
- `bee63b03594a44f68ff2a3cbff78cc7bbaae3ab8` — `build: update Android Gradle Plugin to 9.3.1`.
- `33fd61e6d2028d78163b0d2ea8b956f2e767d644` — `build: update Compose Multiplatform to 1.11.1`.
- `2adcd5d644072249ebbc172c7244ab797dc22c17` — `chore: keep Gradle action on open-source v5 line`.
- `7b6ee2023e48921933cc75724116aa43e921a3d1` — `docs: document final build and CI maintenance`.
- `ff2e0b5aad6a977ece0382deb049eed064db029b` — `docs: record final CI and dependency hardening`.

### Verification status for this checkpoint

This checkpoint is intentionally committed on `audit/final-verification-2026-08-19` and used to trigger a fresh pull-request verification against the hardened workflow definitions. The workflow outcome must be observed before representing the automated build/test matrix as green.

Even after an automated matrix is green, the following remain externally gated and must not be claimed as completed by source edits alone:

1. trusted generation/verification of a standard wrapper JAR if the project wants fully self-contained Gradle wrapper bootstrap;
2. protected production Android signing secret provisioning;
3. actual signed tag-release artifact inspection;
4. manual Android accessibility/lifecycle/device testing;
5. native iOS document-picker/share-sheet/device lifecycle testing in Xcode/simulator/device;
6. real release screenshots captured from verified application builds.

The source/documentation audit is otherwise complete for the currently defined TempoTrack v1.0 scope; future optional roadmap work should be treated as new scope rather than as a hidden release blocker.

---

## Version 2.0.12 release-preparation checkpoint — 2026-08-19

### Release branch and canonical metadata

- Created `release/2.0.12` from final-maintenance `main` commit `503fb29fc37f46afce40933fb66ac6017d548c74`.
- `gradle.properties` now defines `appVersion=2.0.12` and source/development Android `appVersionCode=20012`.
- README now identifies the 2.0.12 release line and is synchronized with Compose Multiplatform 1.11.1 and Android Gradle Plugin 9.3.1.
- `CHANGELOG.md` has a dated `2.0.12` section and an empty post-freeze Unreleased section.
- `ROADMAP.md` has an explicit 2.0.12 release-hardening milestone with externally gated verification/signing/device tasks left open.

### Android release versioning bug fixed

The previous tag workflow used `GITHUB_RUN_NUMBER` as Android `versionCode`. That could produce a tagged 2.0.12 artifact with a versionCode far below source/default `20012`, breaking normal Android upgrade ordering.

The release workflow now derives Android versionCode deterministically from the semantic release tag:

```text
MAJOR * 10000 + MINOR * 100 + PATCH
```

For `v2.0.12`, this produces `20012`.

The tag validation step now also:

- requires canonical numeric `vMAJOR.MINOR.PATCH` with no leading-zero variants;
- rejects component sizes that could overflow shell arithmetic before conversion;
- requires MINOR and PATCH to fit the two-digit mapping;
- rejects a derived Android versionCode outside `1..2100000000`.

Direct shell verification observed during this continuation:

- `v2.0.12` accepted and mapped to `20012`;
- `v02.0.12`, `v2.00.12`, and `v2.0.012` rejected;
- oversized major/minor components rejected;
- non-numeric tags rejected.

### Release documentation corrections

- `docs/build-and-ci.md` now documents source defaults, semantic versionCode mapping, Android range rules and the `v2.0.12 -> 20012` invariant.
- `docs/release.md` now documents canonical no-leading-zero tags, 2.0.12 metadata, release gates, signing boundaries and the exact tag contract.
- The stale release-guide instruction to start from nonexistent `release-notes-template.md` was removed. The dated 2.0.12 changelog section is now the release-note source of truth instead of adding an unnecessary duplicate template.
- Gradle remains pinned to 9.5.0 for 2.0.12 so wrapper properties, bootstrap scripts, CI and release documentation stay aligned.
- Dependabot Gradle 9.7 proposal remains separate from the 2.0.12 freeze and should only be handled as an atomic wrapper/toolchain upgrade with observed build verification.

### 2.0.12 granular commits

- `e47226f64788ac8c1f5085663b758be15247d7b0` — `release: set TempoTrack version 2.0.12`.
- `7f54bbc3427fc18ed1954055598cd58e2ca20478` — `docs: align README with version 2.0.12`.
- `e67f0ae247bd30b5c000eedb45a9d6563919ada8` — `docs: document 2.0.12 build metadata`.
- `2ab7e417213ca5db4242386231c0240b3308bc9d` — `docs: freeze changelog for 2.0.12`.
- `7f2d497e3df20f28dca26149f2d5f42dd7f1a1e8` — `docs: prepare 2.0.12 release procedure`.
- `df4b8be9cba0a97adca26149f2d5f42dd7f1a1e8` — `docs: add 2.0.12 release roadmap`.
- `d7a6132eec55248c0d610aa6b32877de2ee091c3` — `fix: derive Android versionCode from release tag`.
- `256c1c4a1392fa6cbff1c093bf3994a89430090e` — `docs: document semantic Android version codes`.
- `7681c9bc28b2c712a192b489165c648cd4d2ebcd` — `docs: align CI docs with semantic version codes`.
- `5cb7660a6e2671f1f89e154e93de35268bc00a9d` — `docs: record Android release versioning fix`.
- `6d776ef82e98438301584bd67f86d0dd0483de32` — `fix: bound semantic release version parsing`.
- `1037882347ad53347da454220aa36e6b3bdd30f9` — `fix: require canonical semantic release tags`.
- `5b79cd3053cc3fdf57137c76300696477e606a58` — `docs: require canonical 2.0.12 release tags`.

The commit appending this section follows the list above and is the durable 2.0.12 handoff checkpoint.

### Pull-request verification state

PR #13 (`release: prepare TempoTrack 2.0.12`) targets `main` and contains only release/versioning/docs changes plus this handoff append. It was used to trigger CI, CodeQL, Dependency Review and Secret Scan against the current release candidate.

At the time this checkpoint is written, the refreshed hosted workflows are registered but remain **queued**. No failure log exists to fix, and queued status is not recorded as passing.

### Remaining 2.0.12 release gates

The source tree can be merged as the 2.0.12 release line without claiming a production release. The `v2.0.12` tag and distributable-release claim remain gated on:

1. observed CI/build/test results on the supported GitHub runners;
2. protected production Android signing secrets;
3. successful signed APK/AAB tag build and artifact/checksum inspection;
4. Desktop package verification on intended hosts;
5. iOS simulator/framework and native document-picker/share verification on macOS/Xcode;
6. manual accessibility/lifecycle/device checks;
7. real release screenshots captured from verified builds.

No `v2.0.12` tag should be created merely to test these prerequisites. The source release preparation and the production release are deliberately separated so unverified artifacts are never represented as release-ready.

---

## Version 2.12.4 release-preparation checkpoint — 2026-08-24

### Canonical 2.12.4 metadata

- `main` now defines `appVersion=2.12.4` and Android source/development `appVersionCode=21204`.
- The intended release tag is canonical `v2.12.4`; leading-zero aliases remain invalid.
- The semantic Android mapping remains `MAJOR * 10000 + MINOR * 100 + PATCH`, so 2.12.4 maps deterministically to 21204.
- README, CHANGELOG, ROADMAP, release guide, and build/CI guide identify the 2.12.4 release line.
- The release workflow now rejects a tag before platform builds when the tag/versionCode disagree with `gradle.properties` or when README/CHANGELOG/ROADMAP release markers are stale.
- No `v2.12.4` tag has been created by this continuation. Source preparation and production release remain separate.

### Toolchain/repository drift hardening

- Gradle remains pinned to 9.5.0 for this release line.
- `gradle/wrapper/gradle-wrapper.properties` keeps the official Gradle 9.5.0 SHA-256 pin and URL validation and now adds `retries=3` plus `retryBackOffMs=1000`.
- Added `tools/check_gradle_version_alignment.py`.
- The new guard validates the wrapper version/checksum/retry policy, Unix/Windows exact fallback versions, and every GitHub workflow that uses `gradle/actions/setup-gradle`.
- Main CI compiles and executes the Gradle alignment guard alongside the Kotlin package guard, tracked-file coverage guard, Markdown-link guard, and release-metadata consistency check.
- README, `docs/testing.md`, `docs/build-and-ci.md`, `docs/release.md`, `CONTRIBUTING.md`, `.github/pull_request_template.md`, `docs/repository-reference.md`, CHANGELOG, and ROADMAP were synchronized with the four deterministic guard contract.
- The repository intentionally retains Android Gradle Plugin 9.3.1; the stale maintenance branch's unrelated AGP downgrade was not carried into 2.12.4.
- Dependabot PR #14 (`actions/setup-python` v6 → v7) was merged with the Python 3.13 CI contract unchanged.
- Superseded/non-mergeable PR #15 and the obsolete Gradle 9.7 PR #4 were closed after release-safe hardening was salvaged into current `main`.

### 2.12.4 granular commits

- `b63b066c4ef1285b61cb84829d312e6ba097f360` — `release: set TempoTrack version 2.12.4`.
- `8219d579a723be077d0672c4e8a615dc7c1181c2` — `ci: enforce release source version consistency`.
- `98c619740129b2138efdde690d43240e04d65674` — `docs: align README with version 2.12.4`.
- `c09ca8a80e50993f6e3a5108fd5d4e49fc79cdf1` — `docs: freeze changelog for 2.12.4`.
- `140b2afc7b325dca8510290ff0929056c653142e` — `docs: add 2.12.4 release roadmap`.
- `d9743d70c8841d2917f728fa467129d39b5ec5bf` — `docs: prepare 2.12.4 release procedure`.
- `c5b4c6bec827b39f15a5ab1212a7c895761dcce1` — `docs: align CI docs with version 2.12.4`.
- `533d5bb16cef76145985d79f7475a618b5b90ab0` — `ci: validate release metadata on main`.
- `27d8586396adfd62e3bdf979f542ef815f25d3f9` — `build: add bounded Gradle wrapper retries`.
- `2406e4dd62403f0c8b420b857438855b473fec22` — `tools: verify Gradle version alignment`.
- `8b169ade48682e5cf5271eae3b10ef2b9dc4ae6f` — `ci: enforce Gradle version alignment`.
- `d5a4c6efab5b53c4f47a5c3875d96b37bab1b4c4` — `docs: document Gradle alignment guard`.
- `e761aa778581a65190602e14c7b2f96f3b58f2bf` — `ci: update Python setup action to v7` (squash merge of Dependabot PR #14).
- `3c04252b0ef5c6f4410d8f65b6364b3b98dcaeb3` — `docs: add Gradle alignment verification`.
- `7f3beb8b091eeab4c738a9e18d0674bad41205e0` — `docs: document Gradle drift guard`.
- `ef7404b0ed4e93de5a024385238ed8542a29a669` — `docs: require Gradle alignment before releases`.
- `e71498e87635bf053bfa7228cbc0969bbb471457` — `docs: expose Gradle alignment checks in README`.
- `c1bba47cde8e34decf7e37caa2eac9dfea0c2ae3` — `docs: require Gradle alignment for contributions`.
- `e03e6372e5618113580b5977774b6ba500228784` — `docs: add Gradle guard to PR checklist`.
- `15003d835774fb1c4454f734ac7179437fc5b288` — `docs: record 2.12.4 toolchain hardening`.
- `c926b96df486f50c836b3e8ef0de069f9541f9e9` — `docs: expand 2.12.4 release hardening roadmap`.

The commit appending this checkpoint follows the list above and is the durable handoff for this continuation.

### Verification observed during 2.12.4 preparation

- Current repository metadata/source/documents were re-fetched through the connected GitHub interface before sequential writes.
- The current `.github/workflows/ci.yml` was re-read after the setup-python v7 merge and still contains the release-metadata check plus Gradle/package/reference/Markdown guards.
- The combined-status endpoint for the latest queried `main` commit exposed an empty status list. No CI success is inferred from that result.
- PR #14 was observed mergeable and was merged; PR #15 and PR #4 were observed stale/superseded and closed rather than force-merged into the current release line.
- No production Android signing credential was created, requested in source, or committed.
- No signed APK/AAB, Desktop package, iOS framework result, checksum file, real-device result, or release screenshot is represented as verified by this continuation.
- No `v2.12.4` tag or GitHub production release was created.

### Current deterministic guard contract

From a clean Git checkout:

```bash
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

Then run the actual build/test/package matrix documented in `docs/testing.md` and `docs/release.md`.

Configured checks are not evidence that a specific commit passed. Until their real results are observed, the corresponding roadmap boxes remain open.

### Remaining 2.12.4 release gates

1. Observe the complete current `main` CI matrix for shared/Desktop, Android, iOS, documentation, CodeQL, dependency review, and secret scanning as applicable.
2. Observe the four deterministic repository/toolchain guards from a clean Git checkout.
3. Run Android release lint/tests/builds and verify real APK/AAB metadata.
4. Provision protected production Android signing secrets through authorized GitHub repository/environment settings; never commit them.
5. Run Desktop test/package verification on Linux, Windows, and macOS hosts.
6. Run iOS simulator tests and release framework linking on macOS/Xcode; manually verify document picker, activity sheet, cancellation, temporary-file cleanup, and lifecycle behavior.
7. Complete Android target-device lifecycle/export/share/restore/accessibility checks and Desktop keyboard/mini-window/restart checks.
8. After signed release builds exist, inspect every release artifact and generated SHA-256 checksum.
9. Capture real release screenshots only from verified builds.
10. Create/publish `v2.12.4` only after the preceding release gates are actually satisfied.

### 2.12.4 state conclusion

TempoTrack is now source-prepared for the 2.12.4 release line with stronger tag/source consistency checks, deterministic Gradle drift detection, hardened wrapper download policy, synchronized release/verification documentation, and a cleaner maintenance PR queue.

The remaining work is intentionally dominated by observed CI/toolchain execution, private signing configuration, cross-platform packaging, real-device/native verification, artifact inspection, and real screenshots. Those tasks are environment/credential dependent and are not marked complete by repository edits alone.
