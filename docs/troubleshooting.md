# Troubleshooting

Use this guide to separate environment/toolchain problems from application logic, platform integration, persistence, and release configuration failures.

## Wrapper JAR is unavailable

The standard `gradle/wrapper/gradle-wrapper.jar` is not currently tracked.

The included `gradlew`/`gradlew.bat` scripts therefore:

1. use the standard wrapper JAR if a trusted generated copy exists;
2. otherwise delegate to installed Gradle;
3. require the installed version to be **exactly 9.7.0**.

Check:

```bash
gradle --version
```

If the version differs, install/use Gradle 9.7.0.

Maintainers should regenerate the official wrapper only from a trusted Gradle 9.7.0 installation:

```bash
gradle wrapper --gradle-version 9.7.0
```

Do not fabricate the binary JAR. `gradle/wrapper/gradle-wrapper.properties` already pins the expected distribution/checksum and download retry/backoff settings.

## Gradle cannot find Java

TempoTrack targets JVM 17. Install JDK 17+ and ensure both are correct:

```bash
java -version
javac -version
```

Set `JAVA_HOME` to the intended JDK and restart the shell/IDE if it cached an older environment.

CI uses Temurin JDK 17.

## Gradle fallback says the version is wrong

This is intentional. The bootstrap scripts reject an installed fallback Gradle version other than 9.7.0 so local behavior does not silently depend on a different build tool.

Use Gradle 9.7.0 or restore a trusted standard wrapper JAR.

If a repository branch appears to mix Gradle versions, run:

```bash
python tools/check_gradle_version_alignment.py
```

The guard reports drift between wrapper metadata, Unix/Windows launchers, CI, CodeQL, and release automation.

## Android SDK not found

Install Android SDK Platform 37 in Android Studio/SDK Manager.

A local Android checkout may need `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

On Windows use the path format accepted by Gradle/Android Studio.

CI explicitly installs:

```text
platforms;android-37
build-tools;36.0.0
```

## Android local tests do not resolve JUnit

`androidApp` declares JUnit 4.13.2 through the version catalog. Ensure dependency resolution can reach configured repositories and run:

```bash
./gradlew :androidApp:testDebugUnitTest --stacktrace
```

The local tests exercise pure staging-file helpers and do not require a device/emulator.

## Kotlin reports syntax errors at `package in.sanskar...`

`in` is a Kotlin keyword. Kotlin source must use an escaped identifier:

```kotlin
package `in`.sanskar.tempotrack
import `in`.sanskar.tempotrack.domain.StopwatchEngine
```

The runtime/compiled package remains `in.sanskar.tempotrack`.

Run:

```bash
python tools/check_kotlin_package_keywords.py
```

If it fails, correct every reported package/import directive instead of renaming the application namespace.

## Compose resource import is unresolved

The resource class package is configured as:

```text
in.sanskar.tempotrack.resources
```

Kotlin imports still require escaping:

```kotlin
import `in`.sanskar.tempotrack.resources.Res
```

Then run shared compilation/ktlint so generated resource accessors are refreshed/validated.

## `quality` fails but the error is unclear

Run the component tasks separately to isolate the failure:

```bash
./gradlew :shared:allTests --stacktrace
./gradlew :shared:ktlintCheck --stacktrace
./gradlew :androidApp:testDebugUnitTest --stacktrace
./gradlew :androidApp:lintDebug --stacktrace
./gradlew :desktopApp:test --stacktrace
./gradlew :desktopApp:compileKotlin --stacktrace
```

For deterministic repository/source/documentation checks:

```bash
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

## Markdown link checker fails

`tools/check_markdown_links.py` validates repository-local destinations.

Typical causes:

- renamed/moved documentation file;
- incorrect relative path;
- link to a planned file that was not committed;
- case mismatch that worked on a case-insensitive filesystem but fails on Linux.

Fix the link or add the intended tracked file. Do not disable local checking merely because an incorrect link renders in one environment.

## Desktop package task fails

Native packaging depends on host-specific tooling. First separate application compilation from installer creation:

```bash
./gradlew :desktopApp:compileKotlin
./gradlew :desktopApp:run
```

Then retry:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS --stacktrace
```

A successful Desktop run does not prove DMG/MSI/DEB packaging works on every OS.

## Desktop timer returns paused after restart

This is intentional.

Desktop uses `System.nanoTime()`, whose origin must not be compared across JVM processes. A persisted RUNNING timer therefore restores PAUSED at the latest safely persisted elapsed value.

While running, Desktop saves a rebased checkpoint about every five seconds, so after an abrupt kill the restored paused duration should be near the latest heartbeat/action checkpoint.

Resume explicitly to continue.

## Android/iOS timer returns paused after process/device restart

Android/iOS use system-uptime clocks. TempoTrack compares:

- elapsed uptime since checkpoint save;
- elapsed wall time since checkpoint save.

If the values remain plausible, a same-boot process restart can continue RUNNING.

TempoTrack restores PAUSED when:

- the device rebooted/uptime reset;
- current uptime is before saved uptime;
- wall time moved backward;
- uptime/wall elapsed deltas disagree beyond tolerance;
- a legacy running checkpoint has no wall save metadata.

This safe pause prevents an incorrect duration from an incompatible clock reference.

## Active timer disappears and app starts idle

The active checkpoint is transient and fails closed. It can be ignored when:

- JSON is malformed;
- payload exceeds the active-store limit;
- checkpoint invariants are invalid;
- schema version is unsupported/future.

Saved history is a separate store and should remain unaffected.

## Saved history cannot be read

Saved history uses a stricter policy than preferences/active state.

Malformed, oversized, duplicate-ID, unsupported-schema, or semantically invalid history causes `SessionStoreCorruptionException`; the History UI reports a controlled read failure. TempoTrack does **not** silently convert corrupt durable history into an empty valid list or rewrite only the remaining records.

Before manual repair:

1. copy the application-private history file/value if accessible through development tooling;
2. do not overwrite it with guessed JSON;
3. compare against the documented internal/portable formats;
4. prefer restoring a known-good portable JSON backup through the validated restore flow.

See [`data-model-and-storage.md`](data-model-and-storage.md).

## Restore says backup is invalid

Check whether the input is:

- valid TempoTrack portable JSON list, not an internal envelope;
- within configured size/session/lap limits;
- free of duplicate session IDs;
- using sequential lap indices;
- using nonnegative durations/timestamps;
- internally consistent (`split`/`total`/session duration).

CSV exports cannot be restored; JSON is the restore format.

The UI deliberately does not expose raw parser/validation exceptions or echo the whole input.

## Restore is valid but nothing appears to change

If the imported list, after newest-first normalization, is exactly the same as current history, `replaceAll` skips the storage rewrite. The validated restore is still semantically successful.

## Android export fails on Android 10+

MediaStore export must successfully:

1. insert a pending row;
2. open an output stream;
3. write content;
4. finalize by clearing `IS_PENDING`.

TempoTrack treats failed finalization as a write failure and deletes the incomplete row.

Use Android logs/development tooling to inspect OS/storage errors, but do not expose raw internal paths/errors to end users.

## Older Android export does not overwrite an existing file

This is intentional. Pre-Android-10 app-specific export reserves collision-safe names:

```text
backup.json
backup (1).json
backup (2).json
```

Existing backups are preserved.

## Android share target cannot read the file

Verify:

- the manifest `FileProvider` authority matches `${applicationId}.fileprovider`;
- provider remains `exported=false` and grants URI permissions;
- `file_paths.xml` exposes only `shared-exports/` cache subtree;
- outgoing intent contains URI in `EXTRA_STREAM` and `ClipData`;
- `FLAG_GRANT_READ_URI_PERMISSION` is present;
- the URI is `content://`, not `file://` or an absolute path.

A share recipient cannot read outside the configured provider subtree.

## Android sharing creates multiple cache files

This is intentional. Each share gets a unique staged file so starting a second share cannot overwrite content that an earlier recipient may still be reading through a granted URI.

The cache directory is OS-managed transient storage, not durable session history.

## iOS framework does not link

Run on macOS with Xcode installed:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --stacktrace
```

Then run simulator tests:

```bash
./gradlew :shared:iosSimulatorArm64Test --stacktrace
```

Kotlin/Native/UIKit errors cannot be fully diagnosed from a non-macOS environment.

See [`ios.md`](ios.md) for host integration.

## iOS export/share does not present

Check:

- `MainViewController()` is hosted in a live containing Xcode app;
- presenter returns the currently presentable controller;
- operation is invoked on a visible UI;
- iPad/regular-width activity sheet has a valid popover anchor;
- no previous activity/picker operation is still active;
- simulator/device runtime permissions/presentation warnings.

Temporary staging is intentionally cleaned after native completion/failure paths.

## Settings visibly change then revert

Settings use optimistic UI plus persistence rollback. If repository save fails:

- visible preferences revert;
- Desktop side effects such as mini-window/shortcut enabled state revert;
- a localized save-failed message appears.

Investigate the underlying private storage path/permissions rather than removing rollback.

## Android release says signing is partially configured

Local release signing requires all four values or none:

```text
TEMPOTRACK_KEYSTORE_PATH
TEMPOTRACK_KEYSTORE_PASSWORD
TEMPOTRACK_KEY_ALIAS
TEMPOTRACK_KEY_PASSWORD
```

Set every required value and ensure the keystore path is a file, or unset all signing variables for an unsigned local build verification.

Do not commit the values.

## GitHub release job fails before Android build

The release workflow intentionally requires protected secrets, including `TEMPOTRACK_KEYSTORE_BASE64` and signing passwords/alias. A public release tag should not publish an unsigned Android artifact as if production-ready.

See [`release.md`](release.md) and [`build-and-ci.md`](build-and-ci.md).

## CI appears configured but no status is visible

A workflow definition is not proof of a run result. Use the commit's GitHub Actions/checks page or API and record the actual status. If the current tool/API cannot expose the result, treat it as **not observed**, not passing.

## Still blocked?

Before opening an issue collect:

- platform/OS version;
- affected TempoTrack commit/tag;
- JDK version;
- Gradle version;
- exact task/action;
- relevant non-sensitive stack trace/log output;
- whether the problem reproduces after a clean build;
- whether it is build-only, packaging-only, device-only, or data-specific.

Never include:

- signing secrets/keystore bytes/passwords;
- real private user data/backups;
- authentication tokens;
- undisclosed security vulnerability details in a public issue.

For security vulnerabilities follow [`../SECURITY.md`](../SECURITY.md).
