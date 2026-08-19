# Setup

## Requirements

- Git
- JDK 17 or newer
- Gradle 9.5.0 (the bootstrap scripts use a standard wrapper JAR if one is present, otherwise they delegate to an installed `gradle`)
- Android Studio compatible with AGP 9.3 for Android work
- Android SDK Platform 37
- A supported desktop OS for Compose Desktop packaging
- macOS with Xcode for Kotlin/Native iOS framework builds

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

The framework exposes the shared `MainViewController()` entry point. See `docs/ios.md` for host-app wiring and the native export bridge requirement.

## Local Android SDK path

Create `local.properties` only on your machine when needed:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

`local.properties` is ignored by Git.
