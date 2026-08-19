# Data Model, Persistence, and Portability

TempoTrack is local-first. It stores stopwatch history, preferences, and one active-timer checkpoint locally; it does not require an account, hosted database, analytics backend, or cloud synchronization service.

This document separates **internal persistence** from **portable user exports** because they intentionally have different compatibility contracts.

## Logical data stores

TempoTrack has three durable logical stores:

1. **saved session history**;
2. **application preferences**;
3. **active stopwatch checkpoint**.

Each platform provides a `StringStorage` implementation. The shared repositories own JSON encoding/decoding, validation, schema migration, limits, sorting, and concurrency.

## Core data models

### Lap

```text
index: Int
splitNanos: Long
totalNanos: Long
```

Invariants:

- index starts at 1 and increments by one;
- split and total are nonnegative;
- total is cumulative;
- split equals current total minus previous total;
- saved-session total cannot exceed session duration.

### StopwatchSession

```text
id: String
name: String
createdAtEpochMillis: Long
durationNanos: Long
laps: List<Lap>
```

The ID is generated at save time from creation timestamp, duration, and a random unsigned-hex suffix. The human-readable name is independent of identity and can be renamed later.

### StopwatchCheckpoint

```text
status: IDLE | RUNNING | PAUSED
accumulatedNanos: Long
startedAtNanos: Long?
savedAtEpochMillis: Long?
laps: List<Lap>
```

A running checkpoint is rebased at persistence time so `accumulatedNanos` already contains elapsed duration through the save moment. See [`state-and-recovery.md`](state-and-recovery.md).

### AppPreferences

```text
theme: SYSTEM | LIGHT | DARK
largeControls: Boolean
reducedMotion: Boolean
onboardingCompleted: Boolean
miniStopwatchVisible: Boolean
keyboardShortcutsEnabled: Boolean
```

Preference defaults provide forward compatibility when a new field is added to an older stored object.

## Shared persistence limits

| Limit | Value | Applies to |
|---|---:|---|
| `MAX_STORED_SESSIONS` | 10,000 | Saved history and restore count. |
| `MAX_SESSION_STORE_CHARACTERS` | 20,000,000 | Encoded saved-history store and JSON import character ceiling. |
| `MAX_SESSION_NAME_LENGTH` | 80 | Session names and rename UI. |
| `MAX_SESSION_ID_LENGTH` | 160 | Session IDs. |
| `MAX_LAPS_PER_SESSION` | 100,000 | Live engine, saved sessions, active checkpoints. |
| `MAX_PREFERENCES_STORE_CHARACTERS` | 100,000 | Preferences payload. |
| `MAX_ACTIVE_STOPWATCH_STORE_CHARACTERS` | same as session store | Active checkpoint payload. |
| Export filename length | 120 | Sanitized platform filename. |

Limits are part of the compatibility/security/performance contract. If one is changed, review importer, repository, active-checkpoint validation, tests, UI input bounds, and documentation together.

## Saved-session internal store

Current internal session schema version: **1**.

Conceptual envelope:

```json
{
  "schemaVersion": 1,
  "sessions": [
    {
      "id": "1700000000000-5000000000-0000000000001234",
      "name": "Study sprint",
      "createdAtEpochMillis": 1700000000000,
      "durationNanos": 5000000000,
      "laps": []
    }
  ]
}
```

The actual JSON is produced by Kotlinx Serialization. The example demonstrates structure, not a byte-for-byte fixture.

### Read algorithm

`JsonSessionRepository.loadUnlocked` effectively performs:

1. `storage.read()`;
2. blank/missing → empty history;
3. oversized raw content → `SessionStoreCorruptionException`;
4. decode current envelope or recognized legacy form;
5. too many sessions → corruption failure;
6. duplicate IDs → corruption failure;
7. any invalid session → corruption failure;
8. normalize newest-first;
9. if legacy, persist normalized current envelope;
10. return normalized list.

The important rule is **validate before migration rewrite**. A corrupt legacy file is not silently rewritten into a partial/empty current file.

### Legacy compatibility

The original session storage format was a bare JSON list of `StopwatchSession`. `SessionStoreCodec` still recognizes that representation, marks it as requiring migration, and rewrites it only after repository-level validation succeeds.

Unknown future schema versions fail closed.

## Active-stopwatch internal store

Current active schema version: **2**.

Conceptual envelope:

```json
{
  "schemaVersion": 2,
  "checkpoint": {
    "status": "PAUSED",
    "accumulatedNanos": 5000000000,
    "savedAtEpochMillis": 1700000000000,
    "laps": []
  }
}
```

Recognized forms:

- v2 envelope — current;
- v1 envelope — migrated;
- original bare checkpoint — migrated.

Unknown/future schema versions return no checkpoint rather than being guessed at.

Before save, checkpoint validation is mandatory. After decode, invalid checkpoints are rejected.

## Preferences internal store

Current preferences schema version: **1**.

Conceptual envelope:

```json
{
  "schemaVersion": 1,
  "preferences": {
    "theme": "SYSTEM",
    "largeControls": false,
    "reducedMotion": false,
    "onboardingCompleted": false,
    "miniStopwatchVisible": false,
    "keyboardShortcutsEnabled": true
  }
}
```

Legacy bare `AppPreferences` JSON remains readable and is rewritten as the current envelope.

Malformed, oversized, or unsupported preference storage falls back to `AppPreferences()` defaults. This differs from session history: preferences are safely reconstructable configuration, while history corruption must not masquerade as an intentional empty history.

## Session write behavior

`JsonSessionRepository` serializes all reads/writes with a coroutine `Mutex`.

### Upsert

- validate incoming session;
- remove any existing session with the same ID;
- add incoming value;
- sort newest-first;
- enforce count limit;
- skip write if the normalized list equals the current list;
- otherwise encode + enforce encoded-size limit + write.

### Rename

- blank ID → false;
- trim requested name;
- missing ID → false;
- same normalized name → true without write;
- copy session with new name;
- validate full updated session;
- preserve newest-first order and persist.

Blank/too-long names are rejected by shared validation rather than special-cased differently in storage.

### Delete

- blank ID → no-op;
- missing ID → no-op without write;
- existing ID → remove and persist.

### Replace all

Used by validated restore:

- enforce count limit;
- enforce unique IDs;
- validate every session;
- normalize newest-first;
- compare with current validated history;
- skip persistence if identical;
- otherwise replace atomically at the repository/storage boundary.

## Portable JSON backup

`SessionCodec.toJson` exports a **plain list of sessions**, not the internal `schemaVersion` envelope.

Why:

- internal persistence can evolve independently;
- backups represent user data, not implementation metadata;
- restore validates the semantic session objects before replacing local history.

Example shape:

```json
[
  {
    "id": "session-id",
    "name": "Morning intervals",
    "createdAtEpochMillis": 1700000000000,
    "durationNanos": 5000000000,
    "laps": [
      {
        "index": 1,
        "splitNanos": 2000000000,
        "totalNanos": 2000000000
      }
    ]
  }
]
```

## JSON restore pipeline

The History restore dialog never directly writes parsed JSON into storage.

Pipeline:

```text
user text
  -> character-size check
  -> JSON decode
  -> session-count check
  -> duplicate-ID check
  -> per-session validation
  -> newest-first normalized list
  -> explicit Replace confirmation path
  -> SessionRepository.replaceAll
```

Typed restore errors:

- empty backup;
- backup too large;
- invalid JSON;
- invalid data;
- too many sessions;
- duplicate session IDs;
- invalid session (with one-based session number only).

Raw imported content and raw exception messages are not echoed to user-facing error text.

## CSV export format

Header:

```csv
session_id,session_name,created_at_epoch_ms,duration,lap_number,split,total
```

A session with laps produces one row per lap. A session without laps still produces exactly seven columns, leaving lap number/split/total empty.

Human-readable duration fields use `HH:mm:ss.SSS`.

### CSV safety

Text values are quoted and embedded quotes are doubled.

If a trimmed text field begins with a spreadsheet formula trigger:

```text
= + - @
```

TempoTrack prefixes the original value with `'` before CSV quoting. This reduces formula execution when the CSV is opened in spreadsheet software.

CSV is intended for analysis/interchange. JSON is the lossless restore format.

## Export filename policy

A suggested name is sanitized before platform use:

- only ASCII letters, digits, `.`, `_`, `-` survive directly;
- other characters become `_`;
- leading/trailing dots/underscores are trimmed;
- name is limited to 120 characters;
- blank result becomes `tempotrack-export`.

This policy protects filename handling but does not grant filesystem access. Platform adapters still constrain directories or display an OS destination picker.

## Platform storage locations

### Android

Internal logical stores use application-private `filesDir`:

```text
sessions.json
preferences.json
active-stopwatch.json
```

Android share staging uses:

```text
cache/shared-exports/<unique temporary filename>
```

`FileProvider` exposes only the configured share-cache subtree.

Android 10+ durable explicit exports use MediaStore Downloads under `Downloads/TempoTrack`. Older supported Android uses app-specific external Documents/TempoTrack and reserves collision-safe filenames.

### Desktop

Private logical stores live under:

```text
~/.tempotrack/sessions.json
~/.tempotrack/preferences.json
~/.tempotrack/active-stopwatch.json
```

Explicit history export uses a user-selected path from the native Swing file chooser.

### iOS

Logical string stores use `NSUserDefaults` keys:

```text
tempotrack.sessions
tempotrack.preferences
tempotrack.active-stopwatch
```

Document export/share content is staged in a unique directory below `NSTemporaryDirectory()` and cleaned by the native bridge after the operation completes/cancels/fails.

The containing iOS app and operating system own final document destination/storage behavior.

## Atomic private-file writes

Android/Desktop private `StringStorage` implementations use a sibling temporary file:

```text
<store>.tmp
```

Then attempt an atomic replacement move. If the filesystem specifically reports that atomic moves are unsupported, they fall back to replace-existing move.

They do not broadly catch every move exception and silently retry, because permission/path/filesystem failures should remain failures.

`clear()` also removes stale `.tmp` sidecars.

## Concurrency model

Repositories serialize mutation with `Mutex` because multiple UI/platform coroutines can request persistence.

The UI adds a second protection layer where duplicate user actions would be confusing or expensive:

- stopwatch history save is single-flight;
- preference writes are single-flight;
- history export/share preparation is single-flight;
- restore is single-flight;
- history delete/undo/rename is single-flight and mutually excludes data portability work.

Repository mutexes provide storage correctness. UI single-flight state provides predictable user behavior and avoids unnecessary queued work.

## Data corruption philosophy

### Saved history

History is user data. If internal stored history is malformed, unsupported, duplicate-ID, or semantically invalid, TempoTrack raises/propagates a controlled corruption failure rather than silently dropping only the bad records and rewriting the rest.

### Preferences

Preferences are reconstructable configuration. Invalid/oversized/unsupported storage safely falls back to defaults.

### Active checkpoint

Active state is transient. Invalid/oversized/unsupported active persistence is discarded and startup safely falls back to no active checkpoint.

These differing policies are intentional because the cost of silent loss is different for durable user history versus reconstructable/transient state.

## Android backup/device transfer

Android platform backup/device-transfer rules are explicit resource files. Saved sessions/preferences may be eligible according to the platform rules; transient active state and share/export cache paths are excluded as documented in [`../PRIVACY.md`](../PRIVACY.md).

Do not broaden backup scope without reviewing privacy, stale-active-timer semantics, and the backup XML files together.

## Migration checklist

When changing a durable data model:

1. Decide whether the field belongs to internal storage only, portable backup, or both.
2. Add safe serializer defaults when older data must remain readable.
3. If interpretation changes incompatibly, increment the relevant internal schema version.
4. Keep the previous schema explicitly readable if migration is supported.
5. Reject unknown future versions.
6. Validate decoded data before migration rewrite.
7. Add current round-trip, previous-version migration, legacy, malformed, and future-version tests.
8. Update this document, architecture/ADR if semantics changed, changelog, and `repository-reference.md` if files changed.
9. Exercise a real upgrade path on platform storage before release.

## Related documentation

- [`architecture.md`](architecture.md)
- [`state-and-recovery.md`](state-and-recovery.md)
- [`platforms.md`](platforms.md)
- [`testing.md`](testing.md)
- [`../PRIVACY.md`](../PRIVACY.md)
- [`adr/0002-local-json-storage.md`](adr/0002-local-json-storage.md)
- [`adr/0004-versioned-session-storage.md`](adr/0004-versioned-session-storage.md)
- [`adr/0005-platform-checkpoint-recovery.md`](adr/0005-platform-checkpoint-recovery.md)
