# Contributing to TempoTrack

Thanks for helping improve TempoTrack.

## Development setup

Read `docs/setup.md` and `docs/development.md`.

Configure the requested project commit identity locally:

```bash
git config user.email "sanskarin@outlook.in"
```

Use your own GitHub-associated name unless you are the project maintainer.

## Before opening a pull request

Run:

```bash
./gradlew quality
```

Also build the platform you changed when practical.

## Commit style

Prefer small Conventional Commits, for example:

- `feat: add lap sorting`
- `fix: preserve elapsed time after pause`
- `test: cover restored running checkpoint`
- `docs: clarify Android setup`

Do not create empty or artificial commits.

## Pull requests

- Explain user-visible behavior changes.
- Include regression tests for bug fixes.
- Keep secrets and personal data out of fixtures.
- Update documentation for new behavior.
- Add screenshots for meaningful visual changes.

By contributing, you agree that your contribution is licensed under the repository's MIT License.
