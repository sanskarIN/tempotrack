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
- [x] Android and Desktop entry points
- [x] Theme/accessibility settings
- [x] Desktop mini stopwatch
- [x] CI/security/documentation baseline

## 1.1 — UX and platform polish

- [ ] Add real release screenshots after tagged builds are captured.
- [ ] Add Android system share sheet for exported files.
- [ ] Add desktop native save-file chooser.
- [x] Add lap sorting controls without changing recorded order.
- [ ] Add keyboard shortcut help overlay and configurable shortcuts.
- [x] Add undo deletion.
- [ ] Add session rename.

## 1.2 — Broader portability

- [x] Add Kotlin/Native iOS framework targets and a Compose iOS entry point.
- [ ] Validate the iOS framework and simulator tests in macOS CI.
- [ ] Add native iOS document/share-sheet export bridge.
- [ ] Add optional encrypted local backup using platform facilities where useful.

Roadmap items are not promises or release dates; priorities may change based on testing and feedback.
