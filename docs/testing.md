# Testing

## Formatting and lint

```bash
./gradlew :shared:ktlintCheck :androidApp:ktlintCheck :desktopApp:ktlintCheck
```

Android Lint runs separately because it also checks manifests/resources and Android-specific correctness.

## Unit tests

`shared/src/commonTest` covers platform-independent rules including:

- pause/resume excludes paused time;
- monotonic-clock jumps represent device sleep correctly;
- elapsed accumulation saturates instead of wrapping at `Long` boundaries;
- running checkpoints rebase elapsed time at persistence and record injected wall-clock save metadata;
- platform recovery pauses unsafe/legacy checkpoints while retaining known elapsed/lap time;
- Android/iOS-style uptime recovery keeps a running checkpoint only when monotonic and wall elapsed deltas remain consistent;
- active-checkpoint schema v1 migrates to v2 and future schema versions fail closed;
- negative monotonic timestamp origins remain valid;
- lap split/cumulative math and the live lap-count ceiling;
- overflow-safe, integer-accurate lap-average calculation;
- reset behavior;
- duration formatting;
- CSV escaping, spreadsheet-formula neutralization, seven-column row consistency and JSON export content;
- bounded export-filename sanitization and traversal-like filename normalization;
- overflow-safe session validation, rename behavior, sorting and duplicate-id rejection;
- identical session upserts do not rewrite storage;
- renames that normalize to the existing name do not rewrite storage;
- deletes for missing session ids do not rewrite storage;
- full-history replacement skips persistence when the normalized imported history is unchanged;
- validated JSON restore and stable import error codes;
- restore limits staying aligned with persistence limits;
- full stopwatch-to-backup-to-restore regression journeys;
- session-store schema migration;
- active-stopwatch checkpoint validation, bounded storage and limit alignment;
- preference schema migration, oversized-payload fallback, and defaults for newly added settings such as desktop shortcut enablement;
- cancellation-safe suspend result handling.

Run:

```bash
./gradlew :shared:allTests
```

## Android checks

```bash
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
```

Release-oriented Android verification:

```bash
./gradlew :androidApp:lintRelease :androidApp:assembleRelease
```

An unsigned release APK is useful for build verification but must not be treated as a Play-ready signed release artifact.

Manual Android lifecycle/data-portability checks should verify that:

- a running timer survives an ordinary same-boot process restart when the persisted wall/uptime deltas agree;
- a simulated/device reboot or clearly reset uptime reference restores the last checkpoint as PAUSED rather than producing an incorrect running duration;
- a migrated legacy running checkpoint without wall-save metadata is normalized to PAUSED once;
- JSON and CSV exports are created only after the corresponding action;
- Share JSON and Share CSV open the operating-system chooser;
- the shared URI is a `content://` URI rather than a raw filesystem path;
- the receiving app can read the granted file;
- no unrelated cache path is exposed by the `FileProvider` configuration;
- cancelling or returning from the share sheet does not mutate saved history;
- cancelling an in-flight coroutine does not become a false write-failure result.

## Desktop checks

```bash
./gradlew :desktopApp:test
./gradlew :desktopApp:compileKotlin
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Desktop packaging is OS-specific. Manual export checks should confirm the native save chooser opens, honours the suggested JSON/CSV filename, writes to the selected destination, and treats chooser cancellation as a cancellation rather than a write failure.

Desktop restart checks should verify that:

- a running timer persists a rebased checkpoint approximately every five seconds;
- after killing and relaunching the JVM, a previously running timer returns PAUSED at the most recently persisted elapsed value rather than comparing old/new `System.nanoTime()` origins;
- the normalized PAUSED checkpoint is written back once during launch;
- normal pause/resume/lap/reset actions still persist immediately.

Desktop keyboard checks should verify Space/L/R with shortcuts enabled, no stopwatch action from those bindings when disabled, and persistence of the enable/disable setting after restart.

Mini-window checks should verify that enabling the mini stopwatch persists across restart, explicitly closing the mini window persists the hidden state instead of reopening it next launch, and the timer text exposes the same elapsed-time accessibility description as the main stopwatch.

## iOS checks

On macOS with Xcode:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:iosSimulatorArm64Test
```

The `iosTest` source set includes temporary-export-file tests that exercise Foundation directory/file creation, filename sanitization, unique operation directories, and cleanup. This gives the simulator test target direct compilation coverage for the native staging boundary.

The release workflow additionally links the arm64 release framework.

Manual iOS lifecycle/data-portability checks should verify that:

- a running timer survives ordinary same-boot process reconstruction when wall/uptime deltas agree;
- a reboot/reset/legacy checkpoint is normalized to PAUSED at the last safely persisted elapsed value;
- History Export JSON and Export CSV present the native document picker;
- choosing a document destination produces a readable UTF-8 file with the sanitized suggested filename;
- cancelling the document picker returns the explicit cancellation result and does not mutate history;
- History Share JSON and Share CSV present the native activity sheet;
- an iPad/regular-width activity sheet has a valid popover anchor and does not crash during presentation;
- dismissing/cancelling the activity sheet does not mutate history;
- temporary operation directories are removed after document-picker or activity-sheet completion paths;
- the About screen reports the containing app's `CFBundleShortVersionString`.

## UI persistence/concurrency checks

Manually verify that:

- Settings controls temporarily disable while the current preference write is in flight;
- a failed Settings write reverts both visible preference state and Desktop side effects such as mini-window/shortcut enablement;
- the session Save button cannot queue duplicate writes and stale saved feedback clears when the timer or session name changes;
- History JSON/CSV serialization launches one export/share operation at a time;
- delete, undo, and rename history mutations cannot be submitted again while the current history mutation is running;
- history row rename/delete controls and the undo action disable while a history mutation is in flight;
- export/share/restore actions cannot start while a history mutation is running, and a history mutation cannot start while export/share/restore work is in progress;
- the rename dialog cannot be dismissed or edited while its save mutation is running;
- the restore dialog cannot be opened while a data portability operation is being prepared;
- restore confirmation cannot be submitted twice while parsing/replacement is running.

## Large-history responsiveness

For a generated large history near supported limits, manually verify:

- opening History remains responsive;
- JSON/CSV serialization does not freeze the UI thread;
- restore parsing does not freeze the UI thread;
- import failures preserve the existing history.

## Documentation checks

Repository-local Markdown destinations are validated by:

```bash
python tools/check_markdown_links.py
```

The checker intentionally skips external URLs and focuses on links that can be verified deterministically from a clean checkout.

## Full local quality gate

```bash
./gradlew quality :androidApp:assembleDebug :desktopApp:packageDistributionForCurrentOS
python tools/check_markdown_links.py
```

Run the iOS checks separately on macOS.

## Localization review

Compilation verifies generated resource references. Manual UI review should also cover compact and wide layouts, large controls, and unusually long translated strings. See [localization.md](localization.md).

## Manual accessibility checks

See [accessibility.md](accessibility.md).

## Security automation

Pull requests and main-branch changes are checked by the repository secret-scan workflow. CodeQL analyzes Kotlin/Java build output, and dependency review checks dependency changes on pull requests.

## Verification integrity

Do not record a check as passing unless the command or CI job actually ran. If the local environment lacks Gradle, Android SDK, Xcode, or network access, record that limitation in `what_changed.md` and rely only on checks that were actually observable.
