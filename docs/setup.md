# Setup

## Requirements

- Git
- JDK 17 or newer
- Gradle 9.5.0
- Android Studio compatible with AGP 9.3 for Android work
- Android SDK Platform 37
- A supported desktop OS for Compose Desktop packaging
- macOS with Xcode for Kotlin/Native iOS framework builds and iOS host verification

## Gradle bootstrap state

`gradle/wrapper/gradle-wrapper.properties` pins Gradle 9.5.0 and its binary distribution SHA-256. The standard binary `gradle-wrapper.jar` is not committed in the current repository state.

Therefore:

- if a trusted standard wrapper JAR is generated/added later, `gradlew`/`gradlew.bat` will use it and Gradle can validate the pinned distribution checksum;
- while that JAR is absent, the bootstrap scripts require an installed **Gradle 9.5.0** and reject a different installed Gradle version instead of silently changing the toolchain.

Generate the wrapper binary only from a trusted Gradle 9.5.0 installation:

```bash
gradle wrapper --gradle-version 9.5.0
```

Do not hand-create or copy an unverified wrapper binary.

## Clone

```bash
git clone https://github.com/sanskarIN/tempotrack.git
cd tempotrack
```

## Commit identity

For maintainer commits:

```bash
git config user.email "sanskarin@outlook.in"
```

## Verify Gradle

```bash
./gradlew --version
```

Windows:

```powershell
.\gradlew.bat --version
```

The command should report Gradle 9.5.0.

## Build shared tests

```bash
./gradlew :shared:allTests
```

## Run desktop

```bash
./gradlew :desktopApp:run
```

## Build Android

Open the repository in Android Studio, install API 37 when prompted, then run the `androidApp` configuration or:

```bash
./gradlew :androidApp:assembleDebug
```

## Build the iOS shared framework

On macOS with Xcode installed:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:iosSimulatorArm64Test
```

The framework exposes the shared `MainViewController()` entry point. The iOS composition root includes native document-picker export and activity-sheet sharing. See [ios.md](ios.md) for host-app wiring and verification guidance.

## Local Android SDK path

Create `local.properties` only on your machine when needed:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

`local.properties` is ignored by Git.
