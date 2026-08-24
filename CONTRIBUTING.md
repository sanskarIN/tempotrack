# Contributing

Thank you for helping improve TempoTrack.

## Development requirements

- Git and Python 3 for repository-local integrity checks.
- JDK 17 or newer.
- Gradle 9.5.0.
- Android SDK 37 for Android work.
- A supported Desktop host for Compose Desktop work.
- macOS with Xcode for iOS framework/host verification.

The repository currently does not commit the standard binary `gradle-wrapper.jar`. Until a trusted Gradle 9.5.0 installation generates it, `gradlew`/`gradlew.bat` deliberately require an installed Gradle exactly matching 9.5.0.

See [`docs/setup.md`](docs/setup.md) for full environment preparation and [`docs/README.md`](docs/README.md) for the complete documentation index.

## Kotlin namespace syntax

The compiled/runtime namespace is `in.sanskar.tempotrack...`, but `in` is a Kotlin keyword. Kotlin source must escape the leading segment:

```kotlin
package `in`.sanskar.tempotrack.domain
import `in`.sanskar.tempotrack.data.SessionRepository
```

Do not add unescaped `package in.sanskar...` or `import in.sanskar...` directives.

## Before opening a pull request

Run the deterministic repository checks:

```bash
python tools/check_release_metadata.py
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

`check_release_metadata.py` verifies that `appVersion`, Android `appVersionCode`, the README release marker, dated changelog release heading, and roadmap release section all agree.

`check_gradle_version_alignment.py` verifies that wrapper metadata, both bootstrap launchers, and every workflow using `gradle/actions/setup-gradle` agree on the Gradle version and preserve checksum/retry hardening.

`check_repository_reference.py` requires every tracked file from `git ls-files` to appear with its exact path in `docs/repository-reference.md`. If your change adds, renames, or removes a tracked file, update that reference in the same PR.

Then run the checks relevant to your code change:

```bash
./gradlew quality
./gradlew :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug
./gradlew :desktopApp:test :desktopApp:compileKotlin
```

On macOS for iOS/shared Native changes:

```bash
./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64
```

For Desktop packaging changes, also run:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Follow the manual lifecycle/export/share/accessibility guidance in [`docs/testing.md`](docs/testing.md) when your change affects those areas.

## Commit style

Keep commits focused and describe the behavior changed. Maintainers using the project identity should configure:

```bash
git config user.email "sanskarin@outlook.in"
```

Other contributors should use their own Git identity.

Do not mix unrelated formatting/refactors with behavior changes unless required for the implementation. Prefer a separate test/documentation commit when it makes review/history clearer.

## Rules

- Do not commit credentials, production signing material, generated secrets, local SDK paths, or private user data.
- Preserve monotonic live-time calculations; wall time is recovery/session metadata, not the live elapsed-time source.
- Keep persistence/import limits aligned when changing portable-data constraints.
- Preserve schema migration/fail-closed behavior when changing persisted models.
- Validate legacy data before migration rewrites.
- Reject unsupported future schemas rather than guessing their meaning.
- Preserve coroutine cancellation when adding suspend error handling.
- Add regression tests for bug fixes when practical.
- Keep user-visible strings in Compose resources instead of hardcoding shared UI copy.
- Keep platform filesystem/share/export permissions as narrow as possible.
- Keep Android `FileProvider` restricted to the intended share-cache subtree.
- Update documentation when behavior, setup, security, privacy, recovery, build, or release steps change.
- Update `docs/repository-reference.md` for every tracked-file addition/removal/rename.
- Keep release metadata/document markers synchronized; release-tag preparation must use `check_release_metadata.py --tag` with the intended tag.
- Keep Gradle wrapper metadata, launchers, and every Gradle-bearing workflow aligned; update the alignment guard if another workflow starts installing Gradle.
- Review Kotlin/Gradle/AGP compatibility before changing the pinned build toolchain; alignment alone does not prove compatibility.
- Do not claim a platform/build/check passed unless it actually ran.
- Keep third-party assets license-compatible with MIT distribution.

## High-value references

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/code-reference.md`](docs/code-reference.md)
- [`docs/state-and-recovery.md`](docs/state-and-recovery.md)
- [`docs/data-model-and-storage.md`](docs/data-model-and-storage.md)
- [`docs/platforms.md`](docs/platforms.md)
- [`docs/security-model.md`](docs/security-model.md)
- [`docs/maintainer-guide.md`](docs/maintainer-guide.md)
- [`docs/testing.md`](docs/testing.md)
