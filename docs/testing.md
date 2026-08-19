# Testing

## Formatting and lint

```bash
./gradlew :shared:ktlintCheck :androidApp:ktlintCheck :desktopApp:ktlintCheck
```

Android Lint runs separately because it also checks manifests/resources and Android-specific correctness.

## Unit tests

`shared/src/commonTest` covers platform-independent rules:

- pause/resume excludes paused time;
- monotonic-clock jumps represent device sleep correctly;
- lap split/cumulative math;
- stale post-reboot monotonic checkpoints recover safely;
- reset behavior;
- duration formatting;
- CSV escaping, spreadsheet-formula neutralization and JSON export content.

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

Pull requests and main-branch changes are checked by the repository secret-scan workflow. CodeQL analyzes Kotlin/Java build output, and dependency review checks dependency changes on pull requests.
