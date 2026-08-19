# Performance

## Budget

TempoTrack is intentionally offline and lightweight.

Targets:

- Timer UI refresh: approximately 16–33 ms while visible/running.
- No busy loops.
- No network requests in the core product.
- History filtering remains in-memory for the current bounded session store.
- Large JSON/CSV serialization and restore parsing run off the UI dispatcher.
- Persistence never runs on every display tick.

## Timing performance

Display refresh cadence is not the source of truth. Elapsed time is computed from monotonic clock readings, so missed frames and UI suspension do not accumulate timing drift.

Lap-statistics averages use integer quotient/remainder arithmetic instead of summing every split into one potentially overflowing `Long` or converting durations through `Double`.

## Persistence

Session/preferences writes are dispatched off the main thread by platform storage adapters. Android/Desktop file adapters attempt atomic replacement and only fall back when the filesystem specifically reports atomic moves unsupported.

Session-history persistence avoids redundant rewrites when an upsert is byte-for-byte equivalent at the model level, a rename normalizes to the existing name, or a requested delete does not match a stored session. This reduces unnecessary serialization and filesystem replacement without changing observable history behavior.

Most active-stopwatch writes happen on meaningful state changes such as start/pause/resume/lap/reset. Desktop additionally persists a rebased RUNNING checkpoint every five seconds because a JVM `System.nanoTime()` reference is not used across processes. This heartbeat is deliberately far slower than UI refresh and bounds recent elapsed-time loss after a forced Desktop process termination.

The heartbeat writes only while the engine is RUNNING. It stops when the timer is paused/idle or when the containing composition is disposed.

## Large history

The saved-session store and import path are bounded by explicit session/character/lap limits. Export/share encoding snapshots the current history and performs JSON/CSV serialization on `Dispatchers.Default`; validated restore parsing also runs there before the repository replacement step.

History mutations are serialized at the UI action boundary as well as inside the repository. Delete, undo, and rename therefore do not queue competing user actions or overlap export/share/restore preparation while their current mutation is in flight.

If bounded history still causes measurable UI latency on target devices, profile before introducing paging or a database. Avoid adding storage complexity without measured need.
