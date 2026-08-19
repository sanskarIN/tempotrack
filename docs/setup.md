# Setup

## Requirements

- Git
- JDK 17 or newer
- Gradle 9.5.0 (the bootstrap scripts use a standard wrapper JAR if one is present, otherwise they delegate to an installed `gradle`)
- Android Studio compatible with AGP 9.3 for Android work
- Android SDK Platform 37
- A supported desktop OS for Compose Desktop packaging

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

## Local Android SDK path

Create `local.properties` only on your machine when needed:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

`local.properties` is ignored by Git.
