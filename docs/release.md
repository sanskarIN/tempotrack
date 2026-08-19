# Release

## Versioning

TempoTrack follows semantic versioning.

Update:

- Android `versionCode` and `versionName`;
- Desktop `packageVersion`;
- `CHANGELOG.md`;
- `what_changed.md`.

## Pre-release gate

From a clean checkout:

```bash
./gradlew quality :androidApp:assembleDebug
```

Also run the current-OS Desktop package task:

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

## Android signing

Do not commit keystores, passwords, or signing properties. Configure release signing locally or through encrypted CI secrets.

## Tag

Create an annotated `vMAJOR.MINOR.PATCH` tag only after the quality gate is green. The release workflow builds unsigned/debug Android output plus Desktop artifacts where supported.

## Release notes

Start from `docs/release-notes-template.md`, copy the relevant `CHANGELOG.md` section, and include known limitations. Do not claim a platform has been tested unless it was actually built/run.
