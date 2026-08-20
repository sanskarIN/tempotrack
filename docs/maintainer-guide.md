# Maintainer Guide

This guide is for maintainers making code, documentation, dependency, schema, platform, or release changes to TempoTrack. It complements [`CONTRIBUTING.md`](../CONTRIBUTING.md), which is the shorter contributor-facing contract.

## Core maintenance principles

1. **Keep live timing monotonic.** Never calculate stopwatch elapsed duration from epoch/wall time.
2. **Fail safely on persistence ambiguity.** Unknown schema versions, invalid durable history, and unsafe clock references must not be guessed at.
3. **Keep shared rules shared.** Domain/data behavior belongs in `shared/commonMain` unless a platform API is genuinely required.
4. **Keep platform side effects behind small boundaries.** Clocks, storage, export/share, mini windows, and native pickers stay in platform code.
5. **Preserve coroutine cancellation.** Do not turn `CancellationException` into an ordinary persistence/export failure.
6. **Validate before migration rewrite.** A legacy file must be understood and validated before it is replaced by a current envelope.
7. **Document the behavior you actually ship.** Do not record a build/device/signing check as passing unless observed.
8. **Keep changes atomic.** Separate feature/fix/test/docs/tooling commits when practical and meaningful.
9. **Keep the build toolchain coherent.** Wrapper metadata, fallback launchers, CI, CodeQL, and release automation must not silently use different Gradle versions.

## Before changing code

Read the minimum relevant references:

- architecture: [`architecture.md`](architecture.md);
- complete file map: [`repository-reference.md`](repository-reference.md);
- source reference: [`code-reference.md`](code-reference.md);
- timing/recovery: [`state-and-recovery.md`](state-and-recovery.md);
- storage/portability: [`data-model-and-storage.md`](data-model-and-storage.md);
- platform differences: [`platforms.md`](platforms.md);
- tests: [`testing.md`](testing.md).

For a persistence/recovery decision, also read the applicable ADR.

## Kotlin package syntax rule

The compiled namespace begins with `in.sanskar...`, but `in` is a Kotlin keyword. Every Kotlin package/import must escape that segment:

```kotlin
package `in`.sanskar.tempotrack
import `in`.sanskar.tempotrack.domain.StopwatchEngine
```

Run:

```bash
python tools/check_kotlin_package_keywords.py
```

Do not rename the runtime package merely to avoid the source escape unless there is an intentional application-ID/package migration plan.

## Repository quality commands

Fast deterministic repository guards:

```bash
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

The Gradle guard checks wrapper distribution/version/checksum/retry metadata, both fallback launchers, main CI, CodeQL, and release automation. If a new workflow starts installing Gradle, add it to the guard in the same change.

Primary shared quality gate:

```bash
./gradlew quality
```

Useful platform commands:

```bash
./gradlew :shared:allTests
./gradlew :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug
./gradlew :desktopApp:test :desktopApp:compileKotlin :desktopApp:packageDistributionForCurrentOS
```

On macOS/Xcode:

```bash
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

See [`testing.md`](testing.md) before declaring release readiness.

## Adding a stopwatch-domain rule

Examples: a new transition rule, lap rule, duration representation, or timing calculation.

1. Implement the invariant in `shared/.../domain`.
2. Keep the API deterministic and clock-injected.
3. Add boundary tests before or with implementation.
4. Check `Long` overflow/underflow behavior explicitly.
5. If persistence representation changes, follow the schema migration workflow below.
6. If UI changes, map the domain rule into shared UI without duplicating the invariant there.
7. Update code/state docs and changelog.

Do not read `System.currentTimeMillis()` or platform clock APIs from domain code.

## Adding or changing a preference

1. Add the field to `AppPreferences` with a safe default.
2. Decide whether old JSON can decode through that default. If interpretation is incompatible, increment preference schema and add explicit migration support.
3. Add UI control in `SettingsScreen` if user-facing.
4. If the preference controls a platform side effect, expose/update a small capability hook in `TempoTrackDependencies`.
5. Apply side effect during startup from loaded preferences.
6. Ensure failed persistence rolls both visible preference and platform side effect back.
7. Add repository/codec tests for default/migration/save behavior.
8. Add localized strings.
9. Update `user-guide.md`, `code-reference.md`, `data-model-and-storage.md`, and relevant platform docs.

## Adding a saved-session field

First decide whether the field is:

- internal-only persistence detail;
- part of portable JSON backup;
- part of CSV export;
- user-visible UI data.

Then:

1. Update `StopwatchSession`.
2. Update validation.
3. Add serializer default if old portable/internal data should remain readable.
4. Update `SessionCodec` if portable JSON/CSV changes are intended.
5. Update `SessionImporter` validation/error behavior if needed.
6. Review internal session schema version; increment for incompatible interpretation.
7. Add current, legacy, future, malformed, round-trip, and restore tests as applicable.
8. Update all format documentation and release notes.

Avoid coupling portable user backup to the internal `SessionStoreEnvelope`.

## Changing active checkpoint schema

Active state is correctness-sensitive because it can change elapsed recovery.

1. Update `StopwatchCheckpoint` model with a safe default where possible.
2. Update `StopwatchCheckpointValidation`.
3. Update `StopwatchEngine.checkpoint()` semantics if the new field is produced there.
4. Increment `CURRENT_ACTIVE_STOPWATCH_SCHEMA_VERSION` if representation/interpretation changed.
5. Keep the immediately previous schema readable when migration is supported.
6. Keep bare legacy checkpoint decode only while intentionally supported.
7. Reject future versions.
8. Update platform recovery functions/entry points.
9. Add schema migration + recovery tests.
10. Update ADR 0005 if the safety model changes.
11. Exercise actual restart/reboot lifecycle tests before release.

Never make a new schema decoder silently interpret an unknown future version as current.

## Changing storage limits

Search for the shared constant and every aligned consumer.

Important alignments:

- session persistence count ↔ import session count;
- session store characters ↔ import character ceiling;
- session lap limit ↔ active checkpoint lap limit ↔ live engine lap ceiling;
- UI input length ↔ `SessionValidation.MAX_SESSION_NAME_LENGTH`.

Add a test that asserts aligned constants stay aligned. This prevents later drift.

## Changing JSON restore behavior

The correct order is:

```text
size -> decode -> count -> duplicate IDs -> validate sessions -> normalize -> explicit replacement -> repository
```

Do not:

- partially import only valid rows;
- expose raw exception text or user-provided JSON in errors;
- rewrite current history before the entire input is validated;
- reduce importer limits below what a valid self-export can contain without an intentional compatibility decision.

## Changing CSV export

Maintain the fixed seven-column schema unless an intentional format change is documented.

For any new text field:

- quote CSV correctly;
- escape embedded quotes;
- neutralize spreadsheet formula prefixes;
- add tests for commas, quotes, formula prefixes, empty-lap rows, and column count.

CSV is not the canonical restore format.

## Changing Android export/share

Review together:

- `AndroidExporter.kt`;
- `AndroidShareService.kt`;
- `AndroidStagingFiles.kt`;
- `AndroidManifest.xml`;
- `file_paths.xml`;
- backup/data-extraction XML if cache/durable paths change;
- Android staging tests;
- `PRIVACY.md`;
- `platforms.md`/`testing.md`.

Security rules:

- do not expose raw filesystem paths to another app;
- keep `FileProvider` non-exported;
- grant temporary read access only;
- keep provider path narrow;
- use unique staging files when previous share recipients may still read earlier URIs;
- delete incomplete MediaStore entries;
- do not overwrite an existing explicit legacy export without user intent.

## Changing Desktop behavior

For timer/restart changes remember that `System.nanoTime()` is process-local.

Never compare a persisted old JVM `nanoTime` reference to a new JVM reading.

If heartbeat frequency changes:

- document the new recovery-loss bound;
- review write frequency/performance;
- update tests/manual checks and performance docs.

For mini-window or keyboard changes, keep preference persistence and host side effects synchronized.

## Changing iOS native bridges

Native UIKit/Foundation code requires actual macOS/Xcode validation.

Review:

- main-thread presentation;
- object/delegate lifetime across suspension;
- cancellation dismissal;
- `NonCancellable` cleanup where cleanup must complete;
- iPad popover anchoring;
- unique temp directories;
- no premature deletion while native consumer still needs a file;
- containing app bundle version/signing ownership.

Add target tests when logic can execute on the simulator. Do not claim iOS release verification from source inspection alone.

## Adding a localization string

1. Add to Compose resources rather than hard-coding shared user-facing copy.
2. Use stable descriptive resource names.
3. Preserve parameter placeholders/types.
4. Keep shortcut/help text synchronized with actual behavior.
5. Check long text at compact/wide layouts and large font scale.
6. Update [`localization.md`](localization.md) if a new resource grouping/convention is introduced.

Generated resource references use the compiled package `in.sanskar.tempotrack.resources`, but Kotlin imports must spell `` `in`.sanskar.tempotrack.resources... ``.

## Adding an accessibility-relevant control

At minimum review:

- semantic label/content description;
- keyboard focus/activation where relevant;
- touch/click target size;
- large-control mode;
- long/localized text;
- whether meaning relies only on color;
- timer text semantics.

Add the manual case to [`accessibility.md`](accessibility.md) and [`testing.md`](testing.md).

## Adding a module/dependency

### Dependency

1. Add/update version in `gradle/libs.versions.toml`.
2. Reference the catalog alias in the narrowest module/source set.
3. Avoid duplicate independent version strings.
4. Run affected tests/builds.
5. Review dependency-review/CodeQL implications if relevant.
6. Update tech stack/setup docs when the dependency materially changes developer requirements.

### New module

1. Add to `settings.gradle.kts`.
2. Add plugin/dependency configuration.
3. Add ktlint/test/build tasks to CI and root `quality` where appropriate.
4. Update architecture, repository reference, setup, testing, release packaging if relevant.

## Changing the Gradle build tool version

Gradle is pinned outside the dependency catalog. Treat an upgrade as one coordinated change:

1. Update `gradle/wrapper/gradle-wrapper.properties` to the intended official binary distribution.
2. Pin the official SHA-256 and preserve URL validation plus bounded retry/backoff settings.
3. Update the exact fallback version in `gradlew` and `gradlew.bat`.
4. Update every Gradle-bearing workflow currently covered by `tools/check_gradle_version_alignment.py`.
5. Update the alignment guard if a new Gradle-bearing workflow exists.
6. Update README/setup/build/testing/release/troubleshooting/contributor guidance.
7. Run `python tools/check_gradle_version_alignment.py` before Gradle dependency resolution.
8. Run the full supported platform build/test matrix; alignment alone does not prove plugin/tool compatibility.

Do not fabricate `gradle-wrapper.jar`. Generate a standard wrapper binary only from a trusted Gradle installation/distribution chain.

## Version changes

Application versions currently come from `gradle.properties`:

```properties
appVersion=2.0.12
appVersionCode=20012
```

Android consumes both. Desktop consumes `appVersion` for package metadata/About runtime property.

Release workflow also validates semantic tag shape:

```text
vMAJOR.MINOR.PATCH
```

Before changing versions:

- decide whether this is a development bump or release bump;
- update changelog/release notes;
- keep Android versionCode monotonically increasing for store distribution;
- ensure tag version and Gradle application version agree according to the release workflow contract.

## Android release signing

Local optional signing uses:

```text
TEMPOTRACK_KEYSTORE_PATH
TEMPOTRACK_KEYSTORE_PASSWORD
TEMPOTRACK_KEY_ALIAS
TEMPOTRACK_KEY_PASSWORD
```

All four must be set or none. The path must point to a real file.

GitHub release builds use protected secrets, including a base64 keystore value decoded into runner temporary storage.

Never:

- commit a keystore;
- commit passwords;
- add secrets to `.env.example` values;
- print secrets in CI logs;
- broaden workflow permissions unnecessarily.

## Release preparation

Use [`release.md`](release.md) as the canonical procedure. A maintainer should observe:

- Gradle alignment plus namespace/documentation guards;
- common tests;
- Android unit/lint/release assembly + bundle;
- Desktop test/compile/package on intended hosts;
- iOS simulator tests and framework links on macOS;
- manual lifecycle/data portability/accessibility checks;
- protected Android signing configuration;
- release artifact checksums;
- real screenshots only from verified builds.

An unsigned Android APK may prove compilation but is not a production-signed release artifact.

## Documentation update matrix

| Change | Documentation that usually changes |
|---|---|
| User-visible feature | README, user guide, changelog, testing |
| New/removed file | repository reference |
| Domain/API behavior | code reference, architecture/state docs |
| Persistence/schema | data model/storage, ADR if architectural, changelog, testing |
| Platform adapter | platforms, privacy/security as applicable, testing |
| Build/tool version | setup, testing, README tech stack, build/CI, release, troubleshooting, contributor/PR guidance |
| Release process | release, GitHub automation, contributing/PR template if checks change |
| Accessibility | accessibility, user guide/testing |
| Localization | localization, resource reference/test guidance |

## Handoff discipline

`what_changed.md` is a durable project continuation log. After a substantial continuation:

- append the new checkpoint rather than deleting prior history;
- list concrete files/behaviors changed;
- list commit messages/SHAs when available;
- record observed verification separately from configured CI;
- record failed/blocked verification honestly;
- identify environment-gated remaining tasks;
- give exact next commands/tasks.

This file is not a substitute for changelog/user docs; it is an engineering handoff.

## Common review checklist

Before merging/releasing a change, ask:

- Does live timing remain monotonic-only?
- Can this change overflow a `Long` or accept negative duration?
- Does a new persistence field migrate old data safely?
- Are future schema versions rejected?
- Could corrupt history be silently destroyed?
- Does cancellation propagate?
- Can a UI action be submitted twice while persistence is active?
- Could two coroutines overlap storage writes?
- Does a platform file path escape the intended private/staging scope?
- Does a shared URI have only temporary required permission?
- Does the change introduce network/telemetry/privacy behavior?
- Are localized strings/accessibility semantics updated?
- Are automated tests and manual test cases updated?
- Is every new tracked file in `repository-reference.md`?
- Does `tools/check_gradle_version_alignment.py` pass after build-tool/workflow changes?
- Are claims in documentation based on observed verification?
