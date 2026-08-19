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

## Suggested labels

- `bug`
- `enhancement`
- `accessibility`
- `android`
- `desktop`
- `documentation`
- `dependencies`
- `performance`
- `security`
- `testing`

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

Tag only after the release gate in `docs/release.md` succeeds. Attach artifacts produced by the release workflow and base notes on `CHANGELOG.md` plus `docs/release-notes-template.md`.
