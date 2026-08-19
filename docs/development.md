# Development

## Principles

- Keep elapsed-time rules in `shared/domain`.
- Do not read wall time to advance the stopwatch.
- Prefer injected interfaces at platform boundaries.
- Keep files small and cohesive.
- Never log exported/session content by default.
- Add a regression test for timing bugs.

## Formatting

Use Kotlin's official style. IDE formatting should respect `.editorconfig`. Run `./gradlew :shared:ktlintCheck :androidApp:ktlintCheck :desktopApp:ktlintCheck` before committing Kotlin changes.

## Useful tasks

```bash
./gradlew quality
./gradlew :shared:allTests
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
```

## New persistence fields

Serialization uses `ignoreUnknownKeys = true`. Prefer additive fields with defaults. For breaking changes, add an explicit migration layer and tests before changing stored formats.

## Feature work

1. Write or update tests for the behavior.
2. Implement the smallest cohesive change.
3. Run the closest verification task.
4. Update docs if behavior/setup changed.
5. Commit atomically with a Conventional Commit message.

## Desktop shortcuts

- `Space`: start/pause/resume
- `L`: record lap
- `R`: reset
