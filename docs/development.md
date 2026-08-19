# Development

This is the concise day-to-day development workflow. For change-by-change maintenance recipes, schema guidance, platform rules, and release ownership, see [`maintainer-guide.md`](maintainer-guide.md).

## Principles

- Keep elapsed-time rules in shared domain code.
- Do not read wall time to advance the stopwatch.
- Prefer injected interfaces/capability hooks at platform boundaries.
- Keep Android/Swing/UIKit/Foundation APIs out of `commonMain`.
- Validate data before persistence or migration rewrites.
- Preserve coroutine cancellation.
- Keep files cohesive and extract pure/testable logic from platform adapters where practical.
- Never log imported/exported/session content by default.
- Add a regression test for timing, persistence, migration, or concurrency bugs.
- Update behavioral documentation in the same change series.

## Kotlin namespace rule

The compiled package is `in.sanskar.tempotrack...`, but Kotlin source must escape the keyword `in`:

```kotlin
package `in`.sanskar.tempotrack.domain
import `in`.sanskar.tempotrack.data.SessionRepository
```

Run the deterministic guard:

```bash
python tools/check_kotlin_package_keywords.py
```

## Formatting

Use Kotlin's official style. IDE formatting should respect `.editorconfig`.

```bash
./gradlew :shared:ktlintCheck :androidApp:ktlintCheck :desktopApp:ktlintCheck
```

Do not manually format generated Compose resource accessors or build output.

## Fast pre-commit checks

For documentation/tooling-only work:

```bash
python tools/check_kotlin_package_keywords.py
python tools/check_markdown_links.py
```

For shared behavior changes:

```bash
./gradlew :shared:allTests :shared:ktlintCheck
```

For Android-related changes:

```bash
./gradlew :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug
```

For Desktop-related changes:

```bash
./gradlew :desktopApp:test :desktopApp:compileKotlin
```

For broad changes:

```bash
./gradlew quality
```

On macOS for iOS source changes:

```bash
./gradlew :shared:iosSimulatorArm64Test :shared:linkDebugFrameworkIosSimulatorArm64
```

A command is only considered passing when it actually ran successfully in the current/recorded environment.

## Running applications

Desktop:

```bash
./gradlew :desktopApp:run
```

Android debug APK:

```bash
./gradlew :androidApp:assembleDebug
```

The Android module can also be opened/run through Android Studio once SDK 37 is installed.

iOS is a framework/Compose controller integration, not a standalone Gradle-owned app target. Build the framework on macOS, then integrate/run it through a containing Xcode application as documented in [`ios.md`](ios.md).

## Adding a feature

1. Identify whether the rule belongs to domain, data, shared UI, or a platform adapter.
2. Add/adjust the closest regression tests.
3. Implement the smallest cohesive behavior.
4. Run deterministic/static guards.
5. Run the closest module tests/lint/build.
6. Run platform-specific/manual verification where the behavior crosses OS APIs.
7. Update user/architecture/platform/data/testing docs as applicable.
8. Update `repository-reference.md` if files were added/removed/renamed.
9. Add a focused Conventional Commit.

## Persistence changes

Serialization uses `ignoreUnknownKeys = true`, but that alone is **not** a migration strategy.

For additive compatible fields:

- give the model a safe default when older data should remain readable;
- add tests proving old serialized data gets the intended default.

For an incompatible interpretation/shape:

- increment the relevant internal schema version;
- explicitly decode supported previous version(s);
- reject future versions;
- validate before rewriting a migrated value;
- add round-trip/migration/future/malformed tests;
- update `data-model-and-storage.md` and ADR if architectural.

Saved-session portable JSON is intentionally separate from the internal store envelope.

## Concurrency changes

Repository mutexes protect read-modify-write storage operations. UI single-flight flags protect user experience and avoid queued duplicate operations.

When adding a persistence-backed action, ask both:

- can another coroutine race this repository operation?
- can the user submit the same/conflicting action twice before the first finishes?

Use repository serialization for correctness and UI operation state for interaction control.

## Platform adapters

Keep adapter responsibilities narrow:

- clock conversion;
- private storage;
- native export/share destination UI;
- platform-specific restart/reboot recovery;
- Desktop host-only mini-window/keyboard behavior.

Return typed shared errors instead of leaking raw exceptions to common UI.

Preserve `CancellationException` rather than converting it to a normal failure.

## Desktop shortcuts

When enabled:

- `Space`: start/pause/resume;
- `L`: record lap;
- `R`: reset.

The shortcut preference and help resource strings must remain synchronized with the actual Desktop host code.

## Documentation ownership

Use [`docs/README.md`](README.md) as the documentation index. The most important engineering references are:

- [`architecture.md`](architecture.md)
- [`code-reference.md`](code-reference.md)
- [`state-and-recovery.md`](state-and-recovery.md)
- [`data-model-and-storage.md`](data-model-and-storage.md)
- [`platforms.md`](platforms.md)
- [`build-and-ci.md`](build-and-ci.md)
- [`security-model.md`](security-model.md)
- [`maintainer-guide.md`](maintainer-guide.md)
- [`testing.md`](testing.md)

If you add a tracked file, update [`repository-reference.md`](repository-reference.md).
