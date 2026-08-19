# Troubleshooting

## Wrapper JAR is unavailable

If `gradle/wrapper/gradle-wrapper.jar` is not present in your checkout, the included bootstrap scripts delegate to an installed Gradle. Install Gradle 9.5.0 and rerun the command. Maintainers should regenerate the official wrapper with `gradle wrapper --gradle-version 9.5.0` when a trusted local Gradle installation is available.

## Gradle cannot find Java

Install JDK 17+ and set `JAVA_HOME`.

## Android SDK not found

Install Android SDK Platform 37 in Android Studio and create a local `local.properties` with `sdk.dir=...`.

## Desktop package task fails

Native packaging needs host-specific tooling. Run `:desktopApp:run` first to separate compilation problems from installer-tool problems.

## Active timer after an OS reboot

A reboot can reset a platform monotonic clock. TempoTrack detects a restored running checkpoint whose old start reading is ahead of the new monotonic reading and restores it as paused. Resume it explicitly to continue from the accumulated duration.

## History file is malformed

The repository fails closed to an empty readable list instead of crashing the UI. Back up the application-private file before manual repair. Future releases may add an import/repair tool.

## Still blocked?

Open a GitHub issue with platform, JDK version, Gradle task, and the full non-sensitive error output. Never paste credentials or signing secrets.
