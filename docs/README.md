# TempoTrack Documentation

This directory is the maintainer and contributor reference for TempoTrack. The root `README.md` is the product-facing overview; the documents here explain how the repository is built, how the stopwatch remains correct across lifecycle events, how data is stored and moved, how each platform differs, and how to verify/release changes.

## Start here

| Goal | Read |
|---|---|
| Understand the product quickly | [`../README.md`](../README.md) |
| Set up a development machine | [`setup.md`](setup.md) |
| Learn the module and dependency architecture | [`architecture.md`](architecture.md) |
| Understand every tracked repository file | [`repository-reference.md`](repository-reference.md) |
| Understand classes, interfaces, and source responsibilities | [`code-reference.md`](code-reference.md) |
| Understand stopwatch states and recovery behavior | [`state-and-recovery.md`](state-and-recovery.md) |
| Understand persistence, schemas, import/export, and limits | [`data-model-and-storage.md`](data-model-and-storage.md) |
| Understand Android/Desktop/iOS differences | [`platforms.md`](platforms.md) |
| Develop and maintain the repository | [`maintainer-guide.md`](maintainer-guide.md) |
| Run tests and quality gates | [`testing.md`](testing.md) |
| Package and publish releases | [`release.md`](release.md) |
| Diagnose common problems | [`troubleshooting.md`](troubleshooting.md) |
| Review accessibility expectations | [`accessibility.md`](accessibility.md) |
| Add or review translations | [`localization.md`](localization.md) |
| Review performance contracts | [`performance.md`](performance.md) |
| Understand GitHub automation | [`github.md`](github.md) |
| Integrate the iOS framework | [`ios.md`](ios.md) |

## Architecture decisions

Architecture Decision Records document decisions that should not be casually reversed:

- [`adr/0001-monotonic-time.md`](adr/0001-monotonic-time.md) — monotonic clocks are the live elapsed-time source of truth.
- [`adr/0002-local-json-storage.md`](adr/0002-local-json-storage.md) — local JSON persistence for the current bounded product scope.
- [`adr/0003-agp9-module-layout.md`](adr/0003-agp9-module-layout.md) — Android Gradle Plugin 9 module layout.
- [`adr/0004-versioned-session-storage.md`](adr/0004-versioned-session-storage.md) — versioned local session envelopes and migration.
- [`adr/0005-platform-checkpoint-recovery.md`](adr/0005-platform-checkpoint-recovery.md) — platform-specific recovery for persisted running timers.

## Product and project policy documents

The following live at repository root because GitHub and external users expect them there:

- [`../PRIVACY.md`](../PRIVACY.md) — local data, platform backup, export/share, and temporary staging behavior.
- [`../SECURITY.md`](../SECURITY.md) — vulnerability reporting and security scope.
- [`../CONTRIBUTING.md`](../CONTRIBUTING.md) — contribution workflow and quality expectations.
- [`../CODE_OF_CONDUCT.md`](../CODE_OF_CONDUCT.md) — community conduct.
- [`../SUPPORT.md`](../SUPPORT.md) — support channels.
- [`../CHANGELOG.md`](../CHANGELOG.md) — user-visible and engineering changes.
- [`../ROADMAP.md`](../ROADMAP.md) — completed, planned, optional, and externally gated work.
- [`../LICENSE`](../LICENSE) — MIT license.

## Repository tooling references

Two deterministic repository-local Python checks are intentionally independent of Gradle:

```bash
python tools/check_kotlin_package_keywords.py
python tools/check_markdown_links.py
```

The first protects the Kotlin source spelling of the runtime namespace `in.sanskar...`: because `in` is a Kotlin keyword, source declarations/imports must use `` `in`.sanskar... ``. The second validates repository-local Markdown destinations.

The primary Gradle quality task is:

```bash
./gradlew quality
```

See [`testing.md`](testing.md) for the full platform matrix and verification-integrity rules.

## Documentation maintenance rule

When a change modifies a public feature, persistence schema, recovery rule, platform adapter, build/release process, security/privacy behavior, or repository structure, update the relevant document in the same change series. If a tracked file is added, renamed, or removed, also update [`repository-reference.md`](repository-reference.md).

Do not claim a platform build, signing operation, CI job, simulator/device check, or release artifact as verified unless that result was actually observed. Environment-gated work remains explicitly documented as unverified until it runs on the required toolchain.
