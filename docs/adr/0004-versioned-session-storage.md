# ADR 0004: Version local session persistence

- Status: Accepted
- Date: 2026-08-19

## Context

TempoTrack originally persisted session history as a bare JSON array. That format is simple but provides no explicit schema version, making future data migrations ambiguous.

## Decision

Persist session history inside a small envelope containing `schemaVersion` and `sessions`. Version 1 is the first envelope format.

The repository continues to read the original bare-array format as a legacy representation. On a successful read, valid legacy records are normalized and immediately rewritten using the current envelope. Unknown future schema versions fail closed instead of being interpreted as a known format.

Exported JSON remains the portable session-list representation so existing backups remain straightforward and independent of internal storage metadata. The restore path validates every imported session before replacement.

## Consequences

- Future persistence changes can have explicit migration paths.
- Existing installations migrate without manual action.
- Corrupt, inconsistent, duplicate, or unsupported data is not silently persisted.
- Migration behavior requires regression tests whenever the schema version changes.
