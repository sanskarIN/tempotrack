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
- lap split/cumulative math;
- stale post-reboot monotonic checkpoints recover safely;
- reset behavior;
- duration formatting;
- CSV escaping, spreadsheet-formula neutralization and JSON export content;
- session validation, rename behavior, sorting and duplicate-id rejection;
- validated JSON restore and stable import error codes;
- session-store schema migration;
- active-stopwatch checkpoint validation and schema migration;
- preference schema migration, including defaults for newly added settings.

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

## Desktop checks

```bash
./gradlew :desktopApp:test
./gradlew :desktopApp:compileKotlin
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Desktop packaging is OS-specific.

## iOS checks

On macOS with Xcode:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:iosSimulatorArm64Test
```

The release workflow additionally links the arm64 release framework.

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
