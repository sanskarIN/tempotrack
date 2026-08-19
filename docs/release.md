# Release

## Versioning

TempoTrack follows semantic versioning.

Default development values live in `gradle.properties`:

- `appVersion`
- `appVersionCode`

Android and Desktop package builds read these properties. On a `vMAJOR.MINOR.PATCH` tag, the release workflow removes the leading `v` and passes the tag version to Gradle. The Android release job uses the workflow run number as the tag-build `versionCode` override so repeated releases can move forward without committing credentials or generated build metadata.

Before a release, also update:

- `CHANGELOG.md`;
- `ROADMAP.md` when scope changed;
- `what_changed.md`;
- user-visible runtime version metadata when preparing a version other than the current baseline.

## Pre-release gate

From a clean checkout:

```bash
./gradlew quality :androidApp:lintRelease :androidApp:assembleRelease
python tools/check_markdown_links.py
```

Also run the current-OS Desktop package task:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

On macOS with Xcode:

```bash
./gradlew :shared:iosSimulatorArm64Test :shared:linkReleaseFrameworkIosArm64
```

Do not tag until the relevant supported-platform checks are actually green.

## Android signing

Do not commit keystores, passwords, base64 keystores, or signing properties. Configure production release signing locally or through encrypted CI secrets.

The repository's tag workflow currently verifies and packages an **unsigned** Android release APK. That artifact proves the release variant can be assembled, but it is not a Play-ready production package and should not be presented as signed production software.

Before Play/public Android distribution, add a secret-backed signing step and verify the resulting signature independently.

## Desktop packaging

Compose Desktop produces the installer type appropriate to each release runner:

- Linux: DEB;
- macOS: DMG;
- Windows: MSI.

The workflow retains these build outputs as artifacts and collects installer files for the tagged GitHub Release.

## iOS framework packaging

The macOS release job runs the iOS simulator tests, links `TempoTrackShared` for arm64, archives the framework as a ZIP, and uploads it as a release artifact. A containing Xcode app is still responsible for app signing, native document/share UI, and App Store packaging.

## GitHub Release publishing

After Android, Desktop, and iOS framework jobs succeed, the publish job:

1. downloads workflow artifacts;
2. selects APK/DEB/DMG/MSI/ZIP deliverables;
3. computes `SHA256SUMS.txt`;
4. creates the GitHub Release for the tag when one does not already exist;
5. uploads or replaces the release assets.

This makes tag builds reproducible and gives reviewers checksums for downloaded artifacts.

## Tag

Create an annotated `vMAJOR.MINOR.PATCH` tag only after the pre-release gate is green and release notes are ready.

## Release notes

Start from `release-notes-template.md`, copy the relevant `CHANGELOG.md` section, and include known limitations. Do not claim a platform has been tested unless it was actually built/run.

For Android, clearly label unsigned artifacts until production signing is configured. For iOS, clearly distinguish the reusable framework from a complete App Store application.
