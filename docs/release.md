# Release

## Versioning

TempoTrack follows semantic versioning.

Default development values live in `gradle.properties`:

- `appVersion`
- `appVersionCode`

Android and Desktop package builds read these properties. On a canonical `vMAJOR.MINOR.PATCH` tag, the release workflow removes the leading `v` and passes the tag version to Gradle. Numeric version components do not use leading zeros, so tags such as `v02.12.4`, `v2.012.4`, and `v2.12.04` are rejected. Android release versionCode is derived deterministically as:

```text
MAJOR * 10000 + MINOR * 100 + PATCH
```

For that mapping, MINOR and PATCH must each be between 0 and 99, and the resulting Android versionCode must remain in the supported `1..2100000000` range. This keeps source defaults and tagged Android artifacts on the same monotonic versioning scheme instead of tying install ordering to an unrelated workflow run number.

`tools/check_release_metadata.py` is the single repository-local implementation of the release metadata contract. Without arguments it validates source metadata and release-document markers. With `--tag vMAJOR.MINOR.PATCH`, it also requires the tag to be canonical and to match `appVersion` exactly.

The release workflow runs that tagged guard plus `tools/check_gradle_version_alignment.py` on the checked-out tag before any Android/Desktop/iOS release build starts. Release runs are serialized per tag.

## Version 2.12.4 release line

The repository defaults for this release line are:

```properties
appVersion=2.12.4
appVersionCode=21204
```

The intended semantic release tag is:

```text
v2.12.4
```

The tag maps to Android versionCode `21204`, matching the source-tree default. Do not create or promote that tag until the release-candidate checks described below are actually observed green and required Android signing secrets are configured.

Before a release, also update:

- `CHANGELOG.md`;
- `ROADMAP.md` when scope changed;
- `what_changed.md`;
- user-visible/runtime version metadata when preparing a version other than the current baseline.

## Gradle bootstrap integrity

`gradle/wrapper/gradle-wrapper.properties` pins Gradle 9.5.0, the official binary distribution SHA-256, URL validation, and bounded retry/backoff settings. The standard `gradle-wrapper.jar` is not committed in the current repository state, so local bootstrap scripts require an installed Gradle 9.5.0 until a trusted wrapper binary is generated.

Do not create a release from a machine silently using another Gradle version. Either use the exact fallback version or generate the standard wrapper JAR from a trusted Gradle 9.5.0 installation before the release-candidate audit.

Run `python tools/check_gradle_version_alignment.py` before release work. It verifies that wrapper metadata, both launchers, and all workflows using `gradle/actions/setup-gradle` use the same Gradle version and that the wrapper checksum/retry contract remains present.

A Gradle-wrapper upgrade is not part of the 2.12.4 release freeze unless its wrapper properties, launcher/bootstrap assumptions, CI Gradle installation, documentation, and full build matrix are updated and verified together. Matching version pins alone are not compatibility evidence.

## Pre-release gate

From a clean Git checkout, first verify deterministic repository integrity:

```bash
python tools/check_release_metadata.py
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

The release-metadata guard verifies the semantic source version, Android versionCode mapping, README release marker, dated changelog release heading, and roadmap release section. The repository-reference check uses `git ls-files`; it ensures every tracked file is covered by `docs/repository-reference.md` before a release is declared fully documented.

For the exact tag candidate, additionally run:

```bash
python tools/check_release_metadata.py --tag v2.12.4
```

Then run the build/test gate:

```bash
./gradlew quality :androidApp:lintRelease :androidApp:assembleRelease :androidApp:bundleRelease
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

## Documentation release audit

Before tagging:

1. Run all five repository-local Python guards.
2. Run `check_release_metadata.py --tag` with the exact intended tag.
3. Confirm `README.md` and `docs/README.md` point to the current documentation set.
4. Confirm every tracked file is present in `docs/repository-reference.md`.
5. Confirm persistence/recovery/platform behavior matches `state-and-recovery.md`, `data-model-and-storage.md`, and `platforms.md`.
6. Confirm new security/privacy behavior is reflected in `SECURITY.md`, `PRIVACY.md`, and `security-model.md`.
7. Confirm contributor/build/release commands match actual Gradle/CI configuration.
8. Confirm `CHANGELOG.md` contains a dated section for the intended release version.
9. Confirm `gradle.properties`, README release marker, changelog release section, roadmap release-hardening section, intended tag, and Android versionCode mapping all identify the same release.
10. Confirm `what_changed.md` records observed verification and unresolved environment-gated work.
11. Do not publish placeholder screenshots as real release captures.

The tag workflow repeats the release-metadata and Gradle-alignment portions of this audit automatically. That automation is a guard against stale metadata/toolchain drift; it does not replace build, device, signing, accessibility, lifecycle, or artifact inspection evidence.

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

The framework includes the Compose controller plus native document-picker export and activity-sheet sharing bridges. A containing Xcode app is still responsible for application signing, lifecycle/container integration, bundle metadata, device testing, and App Store packaging.

## GitHub Release publishing

After Android, Desktop, and iOS framework jobs succeed, the publish job:

1. downloads workflow artifacts;
2. selects APK/AAB/DEB/DMG/MSI/ZIP deliverables;
3. computes `SHA256SUMS.txt`;
4. creates the GitHub Release for the tag when one does not already exist;
5. uploads or replaces the release assets.

The build jobs use read-only repository permissions. Only the final publish job receives `contents: write`.

## Tag

For this release line, create annotated tag `v2.12.4` only after the pre-release gate is green, production Android signing secrets are configured for a distributable Android release, and release notes are ready.

Do not create a release tag merely to test whether configuration might work; use a release-candidate branch/PR first so failures can be fixed without publishing release semantics.

## Release notes

Use the dated `## [2.12.4] - 2026-08-24` section in `CHANGELOG.md` as the source of truth for 2.12.4 release notes. Copy only the release-relevant items and include known limitations. Do not claim a platform has been tested unless it was actually built/run.

For Android, do not describe artifacts as production-ready unless the signed tag workflow has succeeded. For iOS, clearly distinguish the reusable framework from a signed/packaged App Store application.

At minimum, 2.12.4 release notes should identify:

- local-first stopwatch/history/data-portability scope;
- reliability and recovery hardening inherited from the existing implementation;
- Android/Desktop/iOS framework/platform support boundaries;
- the current Kotlin/Compose/AGP/Gradle toolchain;
- deterministic release metadata and Gradle-alignment validation;
- any verification or signing limitations that remain at publication time.

## Verification record

For a release candidate, preserve the commit SHA and actual observable results for:

- all repository-local Python guards;
- tagged release-metadata validation;
- shared tests;
- Android unit/lint/release APK/AAB;
- Desktop tests/packaging by host;
- iOS simulator tests/framework links;
- manual lifecycle/export/share/accessibility checks;
- Android signing;
- generated checksums/artifact inspection.

Configured workflows are not evidence that a specific commit passed. If a result cannot be observed, record it as **not observed** and do not promote that platform/artifact as verified.

See [`build-and-ci.md`](build-and-ci.md) for workflow internals and [`maintainer-guide.md`](maintainer-guide.md) for safe change procedures.
