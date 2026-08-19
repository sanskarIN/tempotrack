# TempoTrack User Guide

TempoTrack is a local-first stopwatch for precise timing, lap tracking, saved history, and portable backups. It does not require an account or network connection for its core features.

## First launch

The onboarding screen explains the product's local/privacy-first behavior. Selecting the continue/start action saves the onboarding-completed preference before entering the main application.

If that preference cannot be saved, onboarding remains visible and an error is shown rather than pretending setup succeeded.

## Main navigation

TempoTrack has four destinations:

1. **Stopwatch** — timing, laps, lap statistics, and saving a named session.
2. **History** — search, rename, delete/undo, JSON/CSV export/share, and JSON restore.
3. **Settings** — appearance, accessibility, and supported Desktop options.
4. **About** — version/platform, project links, funding, and contact routes.

On compact layouts these appear in bottom navigation. On larger widths they appear in a navigation rail.

## Stopwatch

### Start

From idle, choose **Start**. The timer begins using a platform monotonic clock, so ordinary wall-clock changes do not alter live elapsed time.

### Pause

Choose **Pause** while running. Paused time is excluded from the final elapsed duration.

### Resume

Choose **Resume** from paused state. The timer continues from the accumulated paused duration.

### Reset

Choose **Reset** from paused state to return to zero and clear recorded laps. The transient active-timer checkpoint is also cleared.

### Laps

Choose **Lap** while the timer is running.

Each lap contains:

- a lap number;
- split duration for that lap;
- cumulative total at the lap moment.

Example:

```text
Lap 1 after 2 seconds -> split 00:00:02.000, total 00:00:02.000
Lap 2 after 3 more seconds -> split 00:00:03.000, total 00:00:05.000
```

TempoTrack also shows fastest, average, and slowest lap values. You can view laps in recorded order, fastest-first, or slowest-first. Sorting the view does not change the stored lap order.

## Saving a session

When elapsed time is greater than zero, the Stopwatch screen offers a session-name field and save action.

- Name length is bounded by the shared validation rule.
- A blank name is replaced with a generated default using the save timestamp.
- Saving captures a final stopwatch snapshot and stores it in local history.
- The save action is single-flight so repeated taps cannot queue duplicate writes while the current save is running.
- Saved feedback is cleared if the timer or session name changes, preventing stale success text from describing a modified state.

The active timer and saved history are separate. Saving a session does not automatically reset the timer.

## History

### Search

The search field filters saved sessions by name. Filtering happens in memory against the current bounded local history.

### Rename

Choose **Rename** on a session, edit the name, and save.

- Blank names cannot be submitted.
- The shared maximum name length is enforced.
- The dialog is temporarily locked while persistence is running.
- Renaming to the same normalized name succeeds without rewriting storage.

### Delete and undo

Choose **Delete** to remove a saved session. The most recently deleted session is offered as an undo action.

- Delete is persisted locally.
- Undo re-upserts the deleted session.
- Missing-ID delete requests are storage no-ops.
- Delete/undo/rename cannot overlap one another.

### Operation locking

History deliberately prevents conflicting actions from overlapping:

- export/share preparation cannot overlap restore or mutation;
- restore cannot be submitted twice;
- delete/undo/rename are single-flight;
- relevant controls disable while an operation is in progress.

This is visible UI behavior, not only an internal storage mutex.

## Export JSON

JSON is the lossless TempoTrack backup format.

A JSON export contains the saved session list: IDs, names, creation timestamps, durations, and laps. It does not contain preferences or the transient active timer.

Platform destination behavior:

- **Android 10+** — saved through MediaStore Downloads under `Downloads/TempoTrack`.
- **Older Android** — app-specific Documents/TempoTrack with collision-safe filenames.
- **Desktop** — native save chooser lets you select the destination.
- **iOS** — native document picker lets you choose a destination through the containing app/system.

## Export CSV

CSV is intended for spreadsheet/data analysis. It contains:

```text
session_id
session_name
created_at_epoch_ms
duration
lap_number
split
total
```

Sessions without laps still produce a row with the same seven-column schema.

For safety, text fields beginning with spreadsheet formula characters are neutralized before CSV quoting.

CSV is not the restore format; use JSON when you need a TempoTrack backup that can be restored.

## Share JSON / Share CSV

Where a platform share service is available, History can pass a generated file to the operating-system share UI.

### Android

- Creates a unique temporary cache file per share.
- Uses a restricted, non-exported `FileProvider`.
- Sends a `content://` URI with temporary read permission.
- The operating-system chooser controls which installed destination receives the file.

### iOS

- Creates a unique temporary staging directory/file.
- Opens the native activity sheet.
- Cleans temporary staging when the native operation completes/dismisses/fails.

TempoTrack does not automatically choose a recipient or upload the file itself.

## Restore JSON backup

History includes a restore flow for TempoTrack JSON backups.

Process:

1. open restore;
2. paste/provide the JSON content requested by the current UI;
3. TempoTrack checks size and parses it;
4. it rejects duplicate IDs or semantically invalid sessions;
5. valid sessions are normalized newest-first;
6. confirm **Replace**;
7. current saved history is replaced by the validated backup.

Restore is replacement, not merge. Export a fresh JSON backup before replacement if you need to preserve the current history elsewhere.

A valid backup that is already identical to current normalized history does not cause an unnecessary storage rewrite.

## Settings

### Theme

Choose:

- System;
- Light;
- Dark.

System follows the platform's current dark/light appearance.

### Large controls

Increases main stopwatch control sizing for easier touch/click targets and readability.

### Reduced motion

Persists the user's reduced-motion preference. UI work should respect this preference as motion behavior expands.

### Desktop mini stopwatch

When Desktop support is available, the setting can show/hide an always-on-top mini stopwatch window using the same active engine.

Closing the mini window persists the hidden state so it stays closed after restart.

### Desktop keyboard shortcuts

When enabled:

| Shortcut | Action |
|---|---|
| Space | Start / pause / resume |
| L | Lap |
| R | Reset |

The setting persists. When disabled, these bindings do not perform stopwatch actions.

## Active timer recovery

TempoTrack persists active timer checkpoints on meaningful actions. Desktop additionally saves a running checkpoint approximately every five seconds.

### Android/iOS

A running timer can continue after an ordinary same-boot process restart only when saved uptime and wall-time references remain consistent.

If the device rebooted or the references are unsafe/legacy, TempoTrack restores the timer **paused** at the last safely known elapsed duration instead of calculating from an incompatible clock origin.

### Desktop

A saved `System.nanoTime()` reference is not reused across JVM processes. After a Desktop process restart, a persisted running timer restores paused at the latest saved heartbeat/action checkpoint.

This safety choice may lose a small amount of time after an abrupt Desktop process kill, but avoids inventing an incorrect duration.

## Privacy expectations

TempoTrack's product code has no required account, ads, analytics SDK, authentication backend, or app-managed cloud synchronization.

Data may leave private app storage only when:

- the operating system performs configured backup/device transfer; or
- you explicitly export/share data.

Read [`../PRIVACY.md`](../PRIVACY.md) for the detailed platform behavior.

## Accessibility

Implemented support includes:

- Material controls/labels;
- large controls preference;
- elapsed-time semantic description on main and mini timers;
- fastest/slowest meaning not conveyed by color alone;
- Desktop keyboard controls and help;
- localization-ready strings.

Manual screen-reader/font-scale/keyboard checks remain part of release verification. See [`accessibility.md`](accessibility.md).

## If an operation fails

TempoTrack favors safe failure:

- a failed preference write rolls visible/platform state back;
- corrupt saved history is not silently rewritten as empty/partial history;
- invalid active checkpoints are discarded rather than used for unsafe elapsed calculations;
- cancelled native export is shown as cancellation rather than write failure where the platform reports it;
- export/share errors do not include raw imported content or storage exception text.

For setup/runtime diagnosis see [`troubleshooting.md`](troubleshooting.md).
