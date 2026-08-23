# Setup

This guide prepares a checkout for shared, Android, Desktop, and (on macOS) iOS development. For build internals/CI, see [`build-and-ci.md`](build-and-ci.md); for common failures, see [`troubleshooting.md`](troubleshooting.md).

## Requirements

### All development environments

- Git
- Python 3 for repository-local documentation/source guards
- JDK 17 or newer
- Gradle 9.5.0 while the standard wrapper JAR remains absent

### Android development

- Android Studio compatible with Android Gradle Plugin 9.3
- Android SDK Platform 37
- Android SDK build tools compatible with the project (CI installs 36.0.0)

### Desktop development

- Windows, macOS, or Linux supported by Compose Desktop
- JVM 17
- host-specific native packaging prerequisites when building MSI/DMG/DEB installers

### iOS development

- macOS
- Xcode with an iOS simulator/runtime supported by the Kotlin/Native/Compose toolchain
- command-line tools selected for the intended Xcode installation

## Clone

```bash
git clone https://github.com/sanskarIN/tempotrack.git
cd tempotrack
```

## Repository structure after clone

```text
androidApp/     Android application and Android-native adapters/resources/tests
desktopApp/     Desktop JVM application and Desktop-native adapters
shared/         Domain/data/shared Compose UI + Android library/Desktop/iOS targets
docs/           Architecture, user, maintainer, platform, data, testing, release docs
tools/          Deterministic Python repository checks
gradle/         Version catalog and wrapper properties
.github/        CI/security/release automation and contribution templates
```

For every tracked file see [`repository-reference.md`](repository-reference.md).

## Commit identity for project maintainer

The project maintainer can configure the requested commit email locally:

```bash
git config user.email "sanskarin@outlook.in"
```

Other contributors should use their own Git identity rather than impersonating the maintainer.

## Gradle bootstrap state

`gradle/wrapper/gradle-wrapper.properties` pins Gradle 9.5.0 and its official binary distribution SHA-256. It also enables bounded download retries/backoff and distribution URL validation. The standard binary `gradle-wrapper.jar` is not committed in the current repository state.

Therefore:

- if a trusted standard wrapper JAR is generated/added later, `gradlew`/`gradlew.bat` will use it and Gradle can validate the pinned distribution checksum;
- while that JAR is absent, the bootstrap scripts require an installed **Gradle 9.5.0** and reject a different installed Gradle version instead of silently changing the toolchain.

Verify installed Gradle:

```bash
gradle --version
```

Generate the wrapper binary only from a trusted Gradle 9.5.0 installation:

```bash
gradle wrapper --gradle-version 9.5.0
```

Do not hand-create, base64-invent, or copy an unverified wrapper binary.

## Verify Java

```bash
java -version
javac -version
```

Both should resolve to the intended JDK 17+ installation. If not, set `JAVA_HOME` and update shell/IDE configuration.

CI uses Temurin JDK 17, so JDK 17 is the best local baseline even if a newer JDK is installed.

## Verify repository-local guards

Before Gradle dependency resolution, run:

```bash
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

The Gradle alignment guard verifies wrapper metadata, checksum/retry settings, both launcher fallback versions, and the Gradle pins used by CI, CodeQL, and release automation. Alignment is only one part of a toolchain update; review Kotlin/Gradle/AGP compatibility before changing the pin.

The Kotlin namespace guard exists because the runtime package starts with `in`, a Kotlin keyword. Source must use escaped syntax:

```kotlin
package `in`.sanskar.tempotrack
```

The compiled package remains `in.sanskar.tempotrack`.

The repository-reference guard uses `git ls-files`, so it must run from a real Git checkout. It verifies that every tracked path is documented exactly in [`repository-reference.md`](repository-reference.md).

## Verify Gradle launcher

Linux/macOS:

```bash
./gradlew --version
```

Windows PowerShell:

```powershell
.\gradlew.bat --version
```

The command should report Gradle 9.5.0.

If it reports a fallback mismatch, install/select the exact project Gradle version rather than modifying scripts to accept arbitrary versions.

## First shared verification

```bash
./gradlew :shared:allTests :shared:ktlintCheck
```

This verifies the platform-neutral correctness rules before Android/Desktop packaging complexity is introduced.

## Run Desktop

```bash
./gradlew :desktopApp:run
```

Private Desktop data is stored below:

```text
~/.tempotrack
```

During development you can back up/remove that directory to test first-run/migration/recovery behavior. Do not delete real user data during ordinary debugging.

## Desktop packaging

```bash
./gradlew :desktopApp:test :desktopApp:compileKotlin
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Installer generation is host-specific. A successful Linux package build does not prove Windows MSI or macOS DMG packaging.

## Android Studio setup

1. Open the repository root in Android Studio.
2. Allow Gradle sync using JDK 17.
3. Open SDK Manager.
4. Install Android SDK Platform 37.
5. Ensure the configured Android SDK is discoverable.
6. Run the `androidApp` configuration or use Gradle tasks below.

### Local Android SDK path

Create `local.properties` only on your machine when needed:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

`local.properties` is ignored by Git and must not be committed.

### Android debug verification

```bash
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
```

The JVM unit tests include pure staging/collision behavior and do not require an emulator. Android framework/export/share/lifecycle behavior still needs device/emulator/manual verification.

## Optional local Android release signing

Runtime development does not require signing secrets. For a locally signed release build, set all four environment variables:

```text
TEMPOTRACK_KEYSTORE_PATH
TEMPOTRACK_KEYSTORE_PASSWORD
TEMPOTRACK_KEY_ALIAS
TEMPOTRACK_KEY_PASSWORD
```

The build rejects partial configuration and requires the keystore path to be an actual file.

Do not place real values in `.env.example`, Gradle properties committed to Git, documentation, or shell history that will be shared.

For production GitHub release secrets see [`release.md`](release.md).

## Build the iOS shared framework

On macOS with Xcode installed:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:iosSimulatorArm64Test
```

The framework exposes the shared `MainViewController()` entry point. The iOS composition root includes native document-picker export and activity-sheet sharing.

The Gradle project does **not** replace a containing Xcode app. The Xcode host owns:

- app bundle identifier;
- signing/team/provisioning;
- deployment target/application lifecycle;
- App Store packaging;
- simulator/device launch.

See [`ios.md`](ios.md) for host wiring and native export/share verification.

## IDE recommendations

### IntelliJ IDEA / Android Studio

- use project JDK 17;
- enable Kotlin official code style;
- respect `.editorconfig`;
- avoid auto-importing unescaped `in.sanskar...` package references—Kotlin source needs `` `in`.sanskar... ``;
- do not treat generated Compose resource output/build directories as source files to edit.

### VS Code or other editors

They can edit the repository, but Android/iOS native integration is easier to verify in Android Studio/Xcode. Always use the Gradle/Python repository commands as the source of truth rather than editor diagnostics alone.

## First full local quality gate

Once required SDK/tooling is available:

```bash
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
./gradlew quality :androidApp:assembleDebug :desktopApp:packageDistributionForCurrentOS
python tools/check_markdown_links.py
```

Run iOS verification separately on macOS.

## Suggested setup validation order

If the first build fails, isolate layers in this order:

1. Python repository guards;
2. Java/Gradle version;
3. shared tests/ktlint;
4. Desktop compile/run;
5. Android SDK/unit/lint/build;
6. iOS simulator framework/tests on macOS;
7. packaging/signing only after compilation/tests are stable.

This avoids debugging installer/signing problems before basic compilation is proven.

## Next reading

- [`user-guide.md`](user-guide.md) — product behavior.
- [`architecture.md`](architecture.md) — module/dependency design.
- [`development.md`](development.md) — day-to-day coding workflow reference.
- [`maintainer-guide.md`](maintainer-guide.md) — safe change recipes.
- [`testing.md`](testing.md) — complete verification matrix.
- [`build-and-ci.md`](build-and-ci.md) — build/CI/release automation internals.
