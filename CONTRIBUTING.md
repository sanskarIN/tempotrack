# Contributing

Thank you for helping improve TempoTrack.

## Development requirements

- JDK 17 or newer.
- Gradle 9.5.0.
- Android SDK 37 for Android work.
- A supported Desktop host for Compose Desktop work.
- macOS with Xcode for iOS framework/host verification.

The repository currently does not commit the standard binary `gradle-wrapper.jar`. Until a trusted Gradle 9.5.0 installation generates it, `gradlew`/`gradlew.bat` deliberately require an installed Gradle exactly matching 9.5.0.

## Before opening a pull request

Run the checks relevant to your change:

```bash
./gradlew quality
./gradlew :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug
./gradlew :desktopApp:test :desktopApp:compileKotlin
python tools/check_markdown_links.py
```

On macOS for iOS/shared Native changes:

```bash
./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64
```

For Desktop packaging changes, also run:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Follow the manual lifecycle/export/share/accessibility guidance in `docs/testing.md` when your change affects those areas.

## Commit style

Keep commits focused and describe the behavior changed. Maintainers using the project identity should configure:

```bash
git config user.email "sanskarin@outlook.in"
```

Do not mix unrelated formatting/refactors with behavior changes unless required for the implementation.

## Rules

- Do not commit credentials, production signing material, generated secrets, local SDK paths, or private user data.
- Preserve monotonic live-time calculations; wall time is recovery/session metadata, not the live elapsed-time source.
- Keep persistence/import limits aligned when changing portable-data constraints.
- Preserve schema migration/fail-closed behavior when changing persisted models.
- Preserve coroutine cancellation when adding suspend error handling.
- Add regression tests for bug fixes when practical.
- Keep user-visible strings in Compose resources instead of hardcoding shared UI copy.
- Keep platform filesystem/share/export permissions as narrow as possible.
- Update documentation when behavior, setup, security, privacy, recovery or release steps change.
- Do not claim a platform/build/check passed unless it actually ran.
- Keep third-party assets license-compatible with MIT distribution.
