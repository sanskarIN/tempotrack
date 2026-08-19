# Release

## Versioning

TempoTrack follows semantic versioning.

Default development values live in `gradle.properties`:

- `appVersion`
- `appVersionCode`

Android and Desktop package builds read these properties. On a `vMAJOR.MINOR.PATCH` tag, the release workflow removes the leading `v` and passes the tag version to Gradle. The Android release job uses the workflow run number as the tag-build `versionCode` override so repeated releases can move forward without committing generated build metadata.

The release workflow rejects tags that do not exactly match `vMAJOR.MINOR.PATCH` and serializes release runs per tag.

Before a release, also update:

- `CHANGELOG.md`;
- `ROADMAP.md` when scope changed;
- `what_changed.md`;
- user-visible/runtime version metadata when preparing a version other than the current baseline.

## Gradle bootstrap integrity

`gradle/wrapper/gradle-wrapper.properties` pins Gradle 9.5.0 and the binary distribution SHA-256. The standard `gradle-wrapper.jar` is not committed in the current repository state, so local bootstrap scripts require an installed Gradle 9.5.0 until a trusted wrapper binary is generated.

Do not create a release from a machine silently using another Gradle version. Either use the exact fallback version or generate the standard wrapper JAR from a trusted Gradle 9.5.0 installation before the release-candidate audit.

## Pre-release gate

From a clean checkout:

```bash
./gradlew quality :androidApp:lintRelease :androidApp:assembleRelease :androidApp:bundleRelease
python tools/check_markdown_links.py
```

A local unsigned release can still be used for compilation/lint verification when signing environment variables are absent. It must not be distributed as a production artifact.

Also run the current-OS Desktop package task:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

On macOS with Xcode:

```bash
./gradlew :shared:iosSimulatorArm64Test :shared:linkReleaseFrameworkIosArm64
```

Before tagging, manually verify the platform lifecycle/export/share scenarios in [testing.md](testing.md), including Android/iOS checkpoint recovery and Desktop restart heartbeat recovery.

Do not tag until the relevant supported-platform checks are actually green.

## Android signing

Do not commit keystores, passwords, base64 keystores, signing properties, or any production signing material.

`androidApp/build.gradle.kts` supports a release signing configuration only when all four environment variables are present:

- `TEMPOTRACK_KEYSTORE_PATH`
- `TEMPOTRACK_KEYSTORE_PASSWORD`
- `TEMPOTRACK_KEY_ALIAS`
- `TEMPOTRACK_KEY_PASSWORD`

Partial signing configuration fails configuration immediately.

The tag workflow requires these GitHub Actions secrets:

- `TEMPOTRACK_KEYSTORE_BASE64` — base64 representation of the production keystore;
- `TEMPOTRACK_KEYSTORE_PASSWORD`;
- `TEMPOTRACK_KEY_ALIAS`;
- `TEMPOTRACK_KEY_PASSWORD`.

The workflow decodes the keystore into `$RUNNER_TEMP`, restricts its file mode, exposes only the temporary path to Gradle, and fails before the release build if any required secret is absent. The keystore is never written into the repository workspace as tracked source.

The Android release job builds both:

- signed APK via `:androidApp:assembleRelease`;
- signed Android App Bundle via `:androidApp:bundleRelease`.

It verifies that both output types exist before uploading artifacts. This prevents the publish job from intentionally releasing an unsigned Android artifact.

Repository/environment administrators must still provision the actual production secrets and protect access to them. Source code cannot manufacture or safely commit those credentials.

## Desktop packaging

Compose Desktop produces the installer type appropriate to each release runner:

- Linux: DEB;
- macOS: DMG;
- Windows: MSI.

The workflow retains these build outputs as artifacts and collects installer files for the tagged GitHub Release. The packaged runtime receives the Gradle/tag version as `tempotrack.version`, which the About screen displays.

## iOS framework packaging

The macOS release job runs the iOS simulator tests, links `TempoTrackShared` for arm64, archives the framework as a ZIP, and uploads it as a release artifact.

The framework now includes the Compose controller plus native document-picker export and activity-sheet sharing bridges. A containing Xcode app is still responsible for application signing, lifecycle/container integration, bundle metadata, device testing, and App Store packaging.

## GitHub Release publishing

After Android, Desktop, and iOS framework jobs succeed, the publish job:

1. downloads workflow artifacts;
2. selects APK/AAB/DEB/DMG/MSI/ZIP deliverables;
3. computes `SHA256SUMS.txt`;
4. creates the GitHub Release for the tag when one does not already exist;
5. uploads or replaces the release assets.

The build jobs use read-only repository permissions. Only the final publish job receives `contents: write`.

## Tag

Create an annotated `vMAJOR.MINOR.PATCH` tag only after the pre-release gate is green, production Android signing secrets are configured for a distributable Android release, and release notes are ready.

## Release notes

Start from `release-notes-template.md`, copy the relevant `CHANGELOG.md` section, and include known limitations. Do not claim a platform has been tested unless it was actually built/run.

For Android, do not describe artifacts as production-ready unless the signed tag workflow has succeeded. For iOS, clearly distinguish the reusable framework from a signed/packaged App Store application.
