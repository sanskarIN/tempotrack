# Performance

## Budget

TempoTrack is intentionally offline and lightweight.

Targets:

- Timer UI refresh: approximately 16–33 ms while visible/running.
- No busy loops.
- No network requests in the core product.
- History filtering is in-memory and should remain responsive for ordinary stopwatch use.
- Persistence happens on explicit state-changing actions, not every display tick.

## Timing performance

Display refresh cadence is not the source of truth. Elapsed time is computed from monotonic clock readings, so missed frames and UI suspension do not accumulate timing drift.

## Persistence

Session/preferences writes are dispatched off the main thread in Android/Desktop adapters. Desktop writes use an atomic-move attempt with a safe fallback.

## Future measurement

If history grows large enough to cause measurable UI latency, add paging/indexing only after profiling. Avoid premature database complexity.
