# GitHub Repository Operations

This document covers repository settings and operational policy around the checked-in automation. For a job-by-job description of the workflow source, see [`build-and-ci.md`](build-and-ci.md).

## Recommended branch protection for `main`

Enable branch protection or a ruleset with these safeguards:

- require pull requests for changes when more than one maintainer is collaborating;
- require the CI Android, shared/Desktop, iOS-shared, and documentation jobs after each check name has successfully reported at least once;
- require secret scanning and CodeQL checks when available;
- require dependency review for pull requests that change dependencies when supported by the repository plan/settings;
- require conversation resolution before merge;
- block force pushes and branch deletion;
- dismiss stale approvals when important code changes after review;
- allow Dependabot pull requests to use the same checks as human changes.

Do not make a status check required until that workflow has successfully reported at least once. Otherwise GitHub can leave pull requests permanently waiting for a check name that does not yet exist.

## Main CI expectations

The checked-in CI matrix verifies four job areas:

- **shared-and-desktop** — shared/Desktop ktlint, shared tests, Desktop tests, Desktop compilation;
- **android** — Android ktlint, JVM unit tests, Android Lint, debug assembly;
- **ios-shared** — iOS simulator framework link and simulator target tests on macOS;
- **documentation** — Python tool compilation plus release-metadata, Gradle-alignment, Kotlin keyword-package, exhaustive tracked-file documentation, and local Markdown-link guards.

Superseded branch/PR CI runs are cancelled through workflow concurrency.

A configured workflow is not proof that a particular commit passed. Use the actual Actions/check result before marking work verified.

## Deterministic repository guard policy

The contributor-facing baseline is five repository-local Python guards:

```bash
python tools/check_release_metadata.py
python tools/check_gradle_version_alignment.py
python tools/check_kotlin_package_keywords.py
python tools/check_repository_reference.py
python tools/check_markdown_links.py
```

`check_release_metadata.py` keeps source version metadata and release-document markers synchronized. `check_gradle_version_alignment.py` prevents partial Gradle upgrades across wrapper metadata, launchers, and workflows. The remaining guards protect Kotlin keyword-package syntax, exhaustive tracked-file documentation, and repository-local Markdown navigation.

The pull-request template includes all five requirements explicitly.

## Repository documentation coverage policy

Every tracked file must be documented in [`repository-reference.md`](repository-reference.md).

CI runs:

```bash
python tools/check_repository_reference.py
```

The checker uses `git ls-files` and requires every tracked path to appear exactly in backticks in the reference. This means a PR that adds, renames, or removes a tracked file must update the reference in the same change.

## Kotlin namespace policy

The runtime package begins `in.sanskar...`; Kotlin source must escape the `in` keyword:

```kotlin
package `in`.sanskar.tempotrack
```

CI runs `tools/check_kotlin_package_keywords.py` to prevent a repository-wide compile regression from an accidental unescaped declaration/import.

## Security automation

### CodeQL

CodeQL analyzes Java/Kotlin on main pushes, pull requests, and its weekly schedule. The workflow builds Android debug and Desktop Kotlin before analysis so the analyzer sees project code/build context.

### Dependency review

Dependency review runs on pull requests targeting `main` and evaluates dependency changes separately from Dependabot update generation.

### Secret scan

The repository secret-scan workflow checks common credential/private-key patterns with full history checkout, read-only contents permission, timeout protection, and superseded-run cancellation.

Pattern scans are defense-in-depth, not permission to place sensitive data elsewhere. Real signing material, user data, or credentials must never be committed even if a simple scanner does not match them.

### Dependabot

Dependabot proposes dependency updates according to `.github/dependabot.yml`. Major build-tool changes should still be reviewed against Kotlin/Compose/AGP/Gradle/Kotlin-Native compatibility and the platform matrix.

## Actions secrets for signed Android releases

The tag release workflow intentionally refuses to publish Android release artifacts until production signing material is configured through encrypted GitHub Actions secrets.

Configure these repository or protected-environment secrets:

- `TEMPOTRACK_KEYSTORE_BASE64` — base64 representation of the production Android keystore;
- `TEMPOTRACK_KEYSTORE_PASSWORD`;
- `TEMPOTRACK_KEY_ALIAS`;
- `TEMPOTRACK_KEY_PASSWORD`.

Do not place any real value in source files, workflow YAML, issue text, pull-request comments, logs, `.env.example`, or documentation. Limit permission to edit Actions secrets and release environments to trusted maintainers.

For a stricter release process, place the secrets in a protected GitHub Environment and require manual approval for tag-release jobs before that environment becomes available.

## Release workflow permissions

Build jobs run with read-only repository permissions. The final publish job alone receives `contents: write` because it must create/update the GitHub Release and upload artifacts.

Keep this least-privilege separation when editing release automation.

## Release tag policy

The workflow trigger accepts `v*` so GitHub starts the workflow. The `validate-tag` job then checks out the exact tag and runs:

```bash
python tools/check_release_metadata.py --tag "$GITHUB_REF_NAME"
python tools/check_gradle_version_alignment.py
```

The release-metadata guard enforces canonical `vMAJOR.MINOR.PATCH` syntax, source/tag equality, the Android versionCode mapping, the README release marker, a dated changelog release section, and a matching roadmap section. The Gradle guard ensures the tagged commit has one consistent Gradle pin across wrapper metadata, launchers, and workflows.

Examples accepted when source metadata matches:

```text
v1.0.0
v2.12.4
```

Examples rejected:

```text
v1
v1.0
v1.0.0-beta
v02.12.4
release-1.0.0
```

If pre-release tags are desired later, change the release guard and documentation intentionally rather than weakening only the workflow trigger.

## Release artifacts

After successful build jobs, publishing collects supported outputs:

- Android APK;
- Android AAB;
- Linux DEB;
- macOS DMG;
- Windows MSI;
- iOS arm64 framework ZIP;
- `SHA256SUMS.txt`.

The iOS ZIP is a reusable framework artifact, not a signed App Store application/IPA.

## Recommended repository environments

For production release credentials, a protected GitHub Environment is preferable when the repository plan supports the desired controls.

Recommended controls:

- restrict who can deploy/use release secrets;
- require reviewer approval for production publishing;
- keep signing secrets scoped to the protected environment;
- use branch/tag policies where appropriate;
- do not expose production secrets to ordinary pull-request workflows.

## Suggested labels

- `bug`
- `enhancement`
- `accessibility`
- `android`
- `desktop`
- `ios`
- `documentation`
- `dependencies`
- `performance`
- `security`
- `testing`
- `release`

## Suggested milestones

- `1.0.0 — Reliable local stopwatch`
- `1.1.0 — UX and platform polish`
- `1.2.0 — Broader portability`
- `2.12.4 — Release hardening`

Create milestones only when work is actively planned; avoid placeholder dates that imply a commitment.

## Issue templates

The repository includes structured templates for:

- bug reports;
- feature requests;
- issue chooser/configuration.

Bug reports should request reproducible technical information without encouraging users to attach private history backups, credentials, or signing material.

Security reports must follow `SECURITY.md`, not a public issue template.

## Pull requests

The repository PR template asks contributors to confirm:

- all five deterministic repository guards;
- relevant tests/quality/builds;
- manual platform checks where affected;
- no secrets/private data/signing material;
- documentation/changelog updates;
- exhaustive repository-reference updates for tracked-file changes;
- accessibility consideration.

Limitations should be written explicitly rather than checking a box for work that could not run.

## Discussions

If GitHub Discussions is enabled, useful categories are:

- Announcements — maintainers only;
- Ideas — feature exploration before an issue is created;
- Q&A — usage and development questions;
- Show and tell — integrations, screenshots, and community examples.

Security reports must not be posted to Discussions; use `SECURITY.md`.

## Releases

Tag only after the release gate in [`release.md`](release.md) succeeds. The Android tag job requires signing secrets and produces both APK and AAB artifacts; Desktop runners package their native installers; the macOS job packages the iOS framework. The publish job collects supported artifacts, creates checksums, and attaches them to the GitHub Release.

Use the dated release section in `CHANGELOG.md` as the release-note source of truth. Never mark an artifact or platform as verified unless its build/test job actually completed successfully.

## Actions-result troubleshooting

If a status/check is missing:

1. confirm the workflow trigger includes the branch/event;
2. confirm the workflow file existed on the tested commit;
3. inspect Actions for skipped/cancelled workflow runs;
4. check repository Actions permissions/settings;
5. avoid requiring the missing check until GitHub has reported that exact check name at least once.

If the current API/tooling cannot expose a run status, record it as **not observed** rather than assuming success.

## Related documentation

- [`build-and-ci.md`](build-and-ci.md)
- [`testing.md`](testing.md)
- [`release.md`](release.md)
- [`security-model.md`](security-model.md)
- [`maintainer-guide.md`](maintainer-guide.md)
