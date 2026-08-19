# Roadmap

## 1.0 — Reliable local stopwatch

- [x] Monotonic stopwatch engine
- [x] Pause/resume/reset/laps
- [x] Lap statistics
- [x] Named local history
- [x] Search
- [x] CSV/JSON export
- [x] JSON restore with schema validation
- [x] Versioned local session persistence and legacy migration
- [x] Versioned preferences and active-stopwatch persistence
- [x] Android and Desktop entry points
- [x] Theme/accessibility settings
- [x] Desktop mini stopwatch
- [x] CI/security/documentation baseline

## 1.1 — UX and platform polish

- [ ] Add real release screenshots after tagged builds are captured.
- [ ] Add Android system share sheet for exported files.
- [ ] Add desktop native save-file chooser.
- [x] Add adaptive large-screen navigation.
- [x] Externalize shared UI strings for localization.
- [x] Add lap sorting controls without changing recorded order.
- [x] Add keyboard shortcut help overlay.
- [ ] Add configurable keyboard shortcuts.
- [x] Add undo deletion.
- [x] Add validated session rename.
- [x] Persist floating mini-stopwatch visibility.
- [x] Add branded Android splash treatment.

## 1.2 — Broader portability

- [x] Add Kotlin/Native iOS framework targets and a Compose iOS entry point.
- [x] Add iOS framework and simulator verification jobs to macOS CI.
- [x] Package an iOS arm64 framework on release tags.
- [ ] Add native iOS document/share-sheet export bridge.
- [ ] Add optional encrypted local backup using platform facilities where useful.

## Release engineering

- [x] Derive Android/Desktop package versions from release tags.
- [x] Produce checksummed tag artifacts and publish them to GitHub Releases.
- [ ] Configure production Android signing secrets before distributing Play-ready APK/AAB artifacts.

Roadmap items are not promises or release dates; priorities may change based on testing and feedback.
