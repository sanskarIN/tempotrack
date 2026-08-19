# Testing

## Formatting and lint

```bash
./gradlew :shared:ktlintCheck :androidApp:ktlintCheck :desktopApp:ktlintCheck
```

Android Lint runs separately because it also checks manifests/resources and Android-specific correctness.

## Shared unit tests

`shared/src/commonTest` covers platform-independent rules:

- pause/resume excludes paused time;
- monotonic-clock jumps represent device sleep correctly;
- lap split/cumulative math;
- fastest/slowest/average lap statistics;
- stale post-reboot monotonic checkpoints recover safely;
- reset behavior;
- repeated timer snapshots reuse unchanged immutable lap history;
- duration formatting;
- CSV quote/comma escaping;
- spreadsheet-formula neutralization across common dangerous prefixes, including leading whitespace;
- Unicode JSON export content.

Run:

```bash
./gradlew :shared:allTests
```

## Repository integration tests

The in-memory storage integration suite verifies:

- session repository JSON round trips and newest-first ordering;
- corrupt session data fails closed instead of crashing;
- preference persistence and safe default recovery;
- active-stopwatch checkpoint save/load/clear behavior;
- malformed active checkpoint data is rejected safely.

These tests exercise real serializers/repositories without requiring production files or credentials.

## Android checks

```bash
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
```

## Desktop checks

```bash
./gradlew :desktopApp:test
./gradlew :desktopApp:compileKotlin
```

## Full local quality gate

```bash
./gradlew quality :androidApp:assembleDebug :desktopApp:packageDistributionForCurrentOS
```

Desktop packaging is OS-specific, so CI builds the package format appropriate to each runner when release workflows execute.

## Manual accessibility checks

See `docs/accessibility.md`.

## Security automation

Pull requests and main-branch changes are checked by the repository secret-scan workflow. CodeQL analyzes Kotlin/Java build output, and dependency review checks dependency changes on pull requests. Workflow concurrency cancels superseded verification runs so only the latest branch state consumes CI capacity.
