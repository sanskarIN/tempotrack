# Build System and CI

TempoTrack uses a small Gradle multi-module build with Kotlin Multiplatform/Compose shared code, an Android application module, and a Desktop JVM application module. iOS framework targets live inside the shared Kotlin Multiplatform module.

## Modules

```text
TempoTrack
├── shared
├── androidApp
└── desktopApp
```

`settings.gradle.kts` uses centralized repositories with `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, so module build scripts should not add ad-hoc repositories.

## Toolchain versions

The authoritative dependency/plugin/SDK catalog is `gradle/libs.versions.toml`.

Current declared versions:

| Tool/library | Version |
|---|---:|
| Kotlin | 2.4.10 |
| Android Gradle Plugin | 9.3.1 |
| Compose Multiplatform | 1.11.1 |
| Kotlinx Coroutines | 1.11.0 |
| Kotlinx Serialization | 1.11.0 |
| AndroidX Activity Compose | 1.13.0 |
| AndroidX Core | 1.19.0 |
| ktlint Gradle plugin | 14.2.0 |
| JUnit | 4.13.2 |
| Android compile/target SDK | 37 |
| Android min SDK | 26 |
| Gradle | 9.5.0 |
| JVM target | 17 |

When updating one item, review compatibility with Kotlin compiler, Compose compiler/plugin, AGP, Gradle, Android SDK, Kotlin/Native, and CI runner images together.

## Root Gradle configuration

`build.gradle.kts` declares all project plugins with `apply false`, applies ktlint to every subproject, excludes generated Kotlin such as Compose resource output from ktlint ownership, and defines:

```bash
./gradlew quality
```

`quality` depends on:

- `:shared:allTests`;
- `:desktopApp:test`;
- `:androidApp:testDebugUnitTest`;
- `:androidApp:lintDebug`;
- ktlint for shared/Desktop/Android.

The generated-source exclusion is deliberate: generated Kotlin is validated by the tool that produces/compiles it, while repository-owned Kotlin remains subject to ktlint. This prevents generated Compose resources from making source-style checks fail.

This is the primary local cross-module verification task but it is not a substitute for platform packaging, release lint, iOS linking, repository-local Python guards, or manual lifecycle/accessibility checks.

## Gradle performance/configuration flags

`gradle.properties` enables:

- 3 GiB Gradle JVM heap;
- UTF-8 file encoding;
- parallel execution;
- build cache;
- configuration cache;
- Kotlin incremental compilation;
- official Kotlin code style;
- AndroidX;
- non-transitive Android R classes.

The file also contains the version 2.0.12 application defaults:

```properties
appVersion=2.0.12
appVersionCode=20012
```

Tagged release jobs override `appVersion` from the semantic tag and derive Android `appVersionCode` as `MAJOR * 10000 + MINOR * 100 + PATCH`, so `v2.0.12` maps to `20012` and remains aligned with the source default.

## Wrapper/bootstrap behavior

`gradle/wrapper/gradle-wrapper.properties` pins the official Gradle 9.5.0 binary distribution and SHA-256.

The standard binary `gradle-wrapper.jar` is not currently tracked. Therefore `gradlew`/`gradlew.bat`:

1. use the standard wrapper JAR when a trusted generated copy is present;
2. otherwise locate installed Gradle;
3. require the installed version to be exactly 9.5.0;
4. fail instead of silently building with another Gradle version.

Do not fabricate or hand-encode a wrapper JAR. Generate it from a trusted Gradle 9.5.0 installation and verify the official distribution chain.

A future Gradle-wrapper upgrade must update the wrapper/bootstrap contract as one verified change. Do not change only the distribution URL or only CI's Gradle version while leaving the rest of the repository pinned to another version.

## Shared module

`shared/build.gradle.kts` enables:

- Kotlin Multiplatform;
- Kotlin Serialization;
- Compose Multiplatform + compiler plugin;
- Android KMP library target;
- Desktop JVM target;
- iOS x64, arm64, and simulatorArm64 targets.

Android shared library:

- namespace: `in.sanskar.tempotrack.shared`;
- SDK values come from version catalog;
- JVM target 17;
- Android resources enabled.

Desktop shared JVM target also compiles to JVM 17.

Each iOS target produces a static framework:

```text
TempoTrackShared.framework
```

Common main dependencies include Compose runtime/foundation/material3/UI/resources, coroutines, and serialization JSON.

Common tests use Kotlin test + coroutine test support.

## Compose resources

`compose.resources.packageOfResClass` is:

```text
in.sanskar.tempotrack.resources
```

Kotlin imports must spell this as:

```kotlin
import `in`.sanskar.tempotrack.resources.Res
```

because `in` is a Kotlin keyword.

## Android application build

`androidApp/build.gradle.kts` configures:

- application ID/namespace `in.sanskar.tempotrack`;
- SDK versions from catalog;
- versionName/versionCode from Gradle properties;
- Compose/buildConfig;
- release build type;
- Android lint;
- shared module + AndroidX/coroutines dependencies;
- JUnit local tests.

### Optional local release signing

Four environment variables form one all-or-none signing configuration:

```text
TEMPOTRACK_KEYSTORE_PATH
TEMPOTRACK_KEYSTORE_PASSWORD
TEMPOTRACK_KEY_ALIAS
TEMPOTRACK_KEY_PASSWORD
```

If any but not all are set, Gradle configuration fails. If configured, keystore path must be a real file.

No real value belongs in source control.

## Desktop application build

`desktopApp/build.gradle.kts`:

- uses Kotlin JVM + Compose Desktop;
- compiles to JVM 17;
- depends on `shared` and current-OS Compose runtime;
- main class: `in.sanskar.tempotrack.desktop.MainKt`;
- injects `tempotrack.version` JVM property;
- packages DMG/MSI/DEB according to the current host/tool support.

Desktop package metadata uses `appVersion`.

## Deterministic repository checks

These run without a full Gradle dependency resolution:

```bash
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

### Kotlin package keyword check

The repository runtime package begins with `in`, which is a Kotlin keyword. The checker fails on unescaped source such as:

```kotlin
package in.sanskar.tempotrack
```

Correct source:

```kotlin
package `in`.sanskar.tempotrack
```

CI compiles the checker with Python and runs it in the documentation job.

### Repository reference coverage check

`tools/check_repository_reference.py` uses `git ls-files` as the source of truth for tracked repository files. Every tracked path must appear exactly in backticks in `docs/repository-reference.md`.

This turns the project's “do not skip files in documentation” requirement into an executable check. A file addition, rename, or deletion must be accompanied by a repository-reference update or CI fails.

The checker requires Git metadata because it distinguishes tracked files from generated/untracked build output.

### Markdown link check

Checks repository-local Markdown destinations. External URLs are skipped because they are not deterministic from a checkout.

## GitHub Actions runtime policy

Repository workflows intentionally use maintained action majors compatible with GitHub's Node 24 runner transition:

- `actions/checkout@v7`;
- `actions/setup-java@v5`;
- `actions/setup-python@v6`;
- `android-actions/setup-android@v4`;
- `github/codeql-action@v4`;
- `actions/dependency-review-action@v5`;
- `actions/upload-artifact@v7`;
- `actions/download-artifact@v8`;
- `gradle/actions/setup-gradle@v5`.

`gradle/actions/setup-gradle` is intentionally kept on the v5 line. Gradle Actions v6 moved caching into a separately licensed proprietary component, so TempoTrack does not adopt v6 by default. Dependabot ignores `gradle/actions` `6.x` specifically; a later major may be evaluated independently rather than being permanently blocked.

Action-major upgrades are release-engineering changes. Review runtime requirements, permissions, behavioral changes, and licensing before merging them.

## Main CI workflow

`.github/workflows/ci.yml` runs on pushes and pull requests targeting `main` with read-only repository permissions. Concurrency cancels superseded branch/PR verification.

### `shared-and-desktop`

Ubuntu + JDK 17 + Gradle 9.5.0:

```text
:shared:ktlintCheck
:desktopApp:ktlintCheck
:shared:allTests
:desktopApp:test
:desktopApp:compileKotlin
```

### `android`

Ubuntu + JDK 17 + Android SDK + Gradle 9.5.0:

```text
:androidApp:ktlintCheck
:androidApp:testDebugUnitTest
:androidApp:lintDebug
:androidApp:assembleDebug
```

The workflow explicitly installs Android platform 37 and build-tools 36.0.0 after `android-actions/setup-android@v4` provisions current command-line tools. This avoids the older command-line-tools resolution failure that previously prevented API 37 installation.

### `ios-shared`

macOS + JDK 17 + Gradle 9.5.0:

```text
:shared:linkDebugFrameworkIosSimulatorArm64
:shared:iosSimulatorArm64Test
```

This verifies shared iOS framework linkage/simulator tests, not a complete signed containing Xcode app.

### `documentation`

Ubuntu + Python 3.13:

```text
python -m py_compile tools/check_markdown_links.py tools/check_kotlin_package_keywords.py tools/check_repository_reference.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

The job therefore checks Python syntax, Kotlin namespace source syntax, exhaustive tracked-file documentation coverage, and local Markdown navigation.

## CodeQL workflow

`.github/workflows/codeql.yml` runs on:

- main pushes;
- main pull requests;
- weekly schedule.

It initializes CodeQL v4 for `java-kotlin`, installs JDK/Android SDK/Gradle, builds Android debug + Desktop Kotlin, then runs analysis.

Permissions are limited to what CodeQL needs (`security-events: write` plus read permissions).

## Dependency review

`.github/workflows/dependency-review.yml` runs on pull requests to `main` with read-only content permission and `actions/dependency-review-action@v5`.

This is separate from Dependabot: Dependabot proposes updates; dependency review evaluates dependency changes in PR context.

## Secret scan

`.github/workflows/secret-scan.yml` runs on main pushes/PRs with read-only contents and a 10-minute timeout. It checks repository text/history checkout for common credential patterns including AWS access IDs, private-key headers, and GitHub token-like prefixes.

The current grep excludes documentation Markdown to avoid false positives from examples. That exclusion makes code/config review especially important: never place real secret material in docs merely because the simple pattern job skips them.

## Dependabot

`.github/dependabot.yml` configures automated dependency update PRs for Gradle and GitHub Actions.

The configuration intentionally:

- omits a hard-coded PR label so updates do not fail when a repository label is absent;
- ignores only `gradle/actions` `6.x` because of the separately licensed caching component described above;
- leaves other updates available for normal review.

Review generated updates like any other dependency change; green compilation is necessary but not sufficient for major toolchain upgrades.

## Release workflow

`.github/workflows/release.yml` runs on tags matching the broad trigger `v*`, but the first job strictly validates:

```text
^v[0-9]+\.[0-9]+\.[0-9]+$
```

Only `vMAJOR.MINOR.PATCH` proceeds. The validation job also checks the Android semantic-to-versionCode mapping before any platform build starts.

Release concurrency is per tag and does **not** cancel an in-progress release.

### Android release job

Runs on Ubuntu.

1. Checkout/JDK/Android SDK/Gradle.
2. Require `TEMPOTRACK_KEYSTORE_BASE64` secret.
3. Decode keystore into `$RUNNER_TEMP/tempotrack-release.jks` with restrictive permissions.
4. Require key/password/alias secrets only on the build step.
5. Derive `appVersion` from tag without leading `v`.
6. Derive `appVersionCode` as `MAJOR * 10000 + MINOR * 100 + PATCH`; `v2.0.12` therefore produces `20012`.
7. Run shared tests, Android release lint, Android unit tests, signed APK assembly, and AAB bundle.
8. Verify at least one APK and AAB exist.
9. Upload Android artifact bundle.

The mapping requires MINOR/PATCH values no greater than 99 and a final Android versionCode in `1..2100000000`. This makes release ordering deterministic and prevents a low workflow run number from replacing a higher source/package versionCode.

The release job fails before publishing if required production signing secrets are absent.

### Desktop release matrix

Runs on Ubuntu, Windows, and macOS.

Each host:

- derives version from tag;
- runs shared tests;
- packages the distribution for the current OS;
- uploads the Compose Desktop output.

### iOS framework release job

Runs on macOS.

- derives version from tag;
- runs iOS simulator tests;
- links arm64 release framework;
- archives `TempoTrackShared.framework` as ZIP;
- uploads the framework artifact.

This produces a framework artifact, not a signed App Store IPA.

### Publish job

Runs only after Android/Desktop/iOS jobs succeed.

It has `contents: write` while build jobs remain read-only.

It:

1. downloads all artifacts with `actions/download-artifact@v8`;
2. collects APK/AAB/DEB/DMG/MSI/ZIP outputs;
3. generates `SHA256SUMS.txt`;
4. creates GitHub Release if needed using generated notes;
5. uploads all release assets with `--clobber`.

## Required release secrets

Protected GitHub repository/environment secrets:

```text
TEMPOTRACK_KEYSTORE_BASE64
TEMPOTRACK_KEYSTORE_PASSWORD
TEMPOTRACK_KEY_ALIAS
TEMPOTRACK_KEY_PASSWORD
```

Do not put these values into:

- `.env.example`;
- Gradle properties committed to the repo;
- workflow source;
- issue/PR text;
- logs;
- documentation examples.

## Version/tag relationship

Development defaults live in `gradle.properties`. For the 2.0.12 release line they are `appVersion=2.0.12` and `appVersionCode=20012`.

Release jobs override `appVersion` from the semantic tag and derive Android `versionCode` from the same semantic components. For the intended 2.0.12 release tag, `v2.0.12` therefore remains versionName `2.0.12` / versionCode `20012`.

For a release rehearsal or real release, confirm that generated application metadata and intended tag are consistent before public publishing.

## CI verification integrity

A workflow definition describes intended verification; it is not evidence that a specific commit passed.

For a concrete release candidate record:

- commit SHA;
- workflow run IDs/status;
- failed/retried jobs;
- platform artifacts inspected;
- manual checks performed;
- signing status.

If the connected API/tool cannot expose a job result, record it as **not observed** rather than green.

## Adding a CI check

1. Decide whether it is platform-neutral or platform-specific.
2. Put deterministic cheap checks early/independent where possible.
3. Use least-privilege permissions.
4. Add timeout/concurrency behavior when long/repeated jobs can waste runners.
5. Pin to intentional action major versions.
6. Keep build tool versions aligned with repository catalog/wrapper.
7. Update `testing.md`, `github.md`, this document, and PR template if contributors must satisfy a new requirement.
8. If the check adds a tracked file, update `repository-reference.md` before enabling its coverage guard.

## Related documentation

- [`setup.md`](setup.md)
- [`testing.md`](testing.md)
- [`release.md`](release.md)
- [`github.md`](github.md)
- [`maintainer-guide.md`](maintainer-guide.md)
- [`repository-reference.md`](repository-reference.md)
