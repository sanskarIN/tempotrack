# GitHub Repository Operations

## Recommended branch protection for `main`

Enable branch protection or a ruleset with these safeguards:

- require pull requests for changes when more than one maintainer is collaborating;
- require the CI Android and shared/Desktop jobs to pass;
- require secret scanning and CodeQL checks when available;
- require conversation resolution before merge;
- block force pushes and branch deletion;
- dismiss stale approvals when important code changes after review;
- allow Dependabot pull requests to use the same checks as human changes.

Do not make a status check required until that workflow has successfully reported at least once, otherwise GitHub can leave pull requests permanently waiting for a check name that does not exist.

## Actions secrets for signed Android releases

The tag release workflow intentionally refuses to publish Android release artifacts until production signing material is configured through encrypted GitHub Actions secrets.

Configure these repository or protected-environment secrets:

- `TEMPOTRACK_KEYSTORE_BASE64` — base64 representation of the production Android keystore;
- `TEMPOTRACK_KEYSTORE_PASSWORD`;
- `TEMPOTRACK_KEY_ALIAS`;
- `TEMPOTRACK_KEY_PASSWORD`.

Do not place any real value in source files, workflow YAML, issue text, pull-request comments, logs, `.env.example`, or documentation. Limit permission to edit Actions secrets and release environments to trusted maintainers.

For a stricter release process, place the secrets in a protected GitHub Environment and require manual approval for tag-release jobs before that environment becomes available.

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

Create milestones only when work is actively planned; avoid placeholder dates that imply a commitment.

## Discussions

If GitHub Discussions is enabled, useful categories are:

- Announcements — maintainers only;
- Ideas — feature exploration before an issue is created;
- Q&A — usage and development questions;
- Show and tell — integrations, screenshots, and community examples.

Security reports must not be posted to Discussions; use `SECURITY.md`.

## Releases

Tag only after the release gate in `docs/release.md` succeeds. The Android tag job requires signing secrets and produces both APK and AAB artifacts; Desktop runners package their native installers; the macOS job packages the iOS framework. The publish job collects supported artifacts, creates checksums, and attaches them to the GitHub Release.

Base release notes on `CHANGELOG.md` plus `docs/release-notes-template.md`. Never mark an artifact or platform as verified unless its build/test job actually completed successfully.
