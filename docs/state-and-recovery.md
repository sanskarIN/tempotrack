# Stopwatch State and Recovery

TempoTrack separates **live timing correctness** from **persistence/restart recovery**. Live elapsed time is always derived from a monotonic clock. Wall-clock time is metadata used only to judge whether a persisted system-uptime reference is still safe after a process restart/reboot.

## State machine

```text
              start
      +--------------------+
      |                    v
   +------+             +---------+
   | IDLE |             | RUNNING |
   +------+             +---------+
      ^                    |   ^
      | reset              |   | resume
      |                    |   |
      |                  pause |
      |                    v   |
      |                 +--------+
      +-----------------| PAUSED |
                        +--------+
```

`lap()` is accepted only while `RUNNING`. `reset()` returns either active state to `IDLE` and clears accumulated duration/laps.

Calling an operation that does not apply to the current state is intentionally idempotent: the engine returns the current snapshot rather than inventing an invalid transition.

## Canonical state invariants

### IDLE

- `accumulatedNanos == 0`
- `startedAtNanos == null`
- no laps
- snapshot elapsed is zero

### RUNNING

- `startedAtNanos != null`
- current elapsed = `accumulatedNanos + max(0, nowMonotonic - startedAtNanos)` using saturating addition
- laps may exist
- wall time is not part of elapsed calculation

### PAUSED

- `startedAtNanos == null`
- snapshot elapsed equals `accumulatedNanos`
- laps may exist

## Start, pause, and resume arithmetic

Assume an engine starts with monotonic reading `M0`.

After `start()`:

```text
status = RUNNING
accumulated = 0
startedAt = M0
```

At monotonic `M1`, visible elapsed is:

```text
elapsed = max(0, M1 - M0)
```

When paused at `M1`, the engine moves visible elapsed into the durable baseline:

```text
status = PAUSED
accumulated = elapsed
startedAt = null
```

If resumed at `M2`:

```text
status = RUNNING
accumulated = previous paused elapsed
startedAt = M2
```

Paused time therefore does not contribute to elapsed duration.

## Overflow behavior

`Long` nanoseconds are large but finite. TempoTrack never allows elapsed accumulation to wrap from a large positive duration into a negative number.

When:

```text
accumulated + activeDelta > Long.MAX_VALUE
```

elapsed saturates at:

```text
Long.MAX_VALUE
```

This behavior is deterministic and unit tested.

## Lap semantics

A lap stores two related values:

```text
split = currentTotal - previousLapTotal
total = current stopwatch elapsed
```

For the first lap, previous total is zero.

Example:

```text
Timer start
2 seconds -> Lap 1: split 2s, total 2s
3 more seconds -> Lap 2: split 3s, total 5s
```

The engine stops accepting new laps at `SessionValidation.MAX_LAPS_PER_SESSION`. This prevents live state from growing beyond what saved-session and active-checkpoint persistence will accept.

## Why checkpoints are rebased

A naive running checkpoint might persist:

```text
accumulated = elapsed before current run segment
startedAt = old monotonic start
```

That makes most of the current run depend on the old monotonic origin being valid after restart.

TempoTrack instead **rebases at every persistence event**. If current running elapsed is `E` and current monotonic reading is `Msave`, the checkpoint becomes:

```text
status = RUNNING
accumulatedNanos = E
startedAtNanos = Msave
savedAtEpochMillis = wall time at save
```

This means `accumulatedNanos` is always a safe elapsed-at-save lower bound. If the monotonic origin later proves unusable, recovery can pause at `E` instead of falling back to a much older baseline.

## Active checkpoint schema

The current active-stopwatch store envelope is schema version 2.

Conceptually:

```json
{
  "schemaVersion": 2,
  "checkpoint": {
    "status": "RUNNING",
    "accumulatedNanos": 5000000000,
    "startedAtNanos": 123456789000,
    "savedAtEpochMillis": 1700000000000,
    "laps": []
  }
}
```

The exact numeric values are examples only.

Readable forms:

- current schema v2 envelope;
- schema v1 envelope (migrated after validation);
- original bare `StopwatchCheckpoint` JSON (migrated after validation).

Unknown/future envelope versions fail closed.

## Recovery policy by platform

| Platform | Monotonic source | Persisted running recovery |
|---|---|---|
| Android | `SystemClock.elapsedRealtimeNanos()` | May continue running if uptime and wall elapsed deltas remain consistent. |
| iOS | `NSProcessInfo.systemUptime` | May continue running if uptime and wall elapsed deltas remain consistent. |
| Desktop | `System.nanoTime()` | Always normalize persisted RUNNING to PAUSED across process restart. |

## Android/iOS system-uptime recovery

System uptime usually survives application process restarts but resets when the device reboots. TempoTrack therefore stores two references at checkpoint save:

- monotonic uptime reading (`startedAtNanos`, rebased at save);
- wall epoch timestamp (`savedAtEpochMillis`).

At restore:

```text
uptimeDeltaMillis = (currentUptime - savedUptime) / 1_000_000
wallDeltaMillis   = currentWall - savedWall
```

Recovery allows the timer to remain `RUNNING` only when:

- both stored/current values are nonnegative where required;
- current uptime is not before saved uptime;
- current wall time is not before saved wall time;
- `abs(uptimeDeltaMillis - wallDeltaMillis)` is within the configured tolerance.

Default tolerance: 120,000 ms (two minutes).

The tolerance is intentionally broad enough for ordinary wall-clock adjustment/noise while still catching obvious reboot/reference mismatches.

### Recovery failure behavior

If any required consistency check fails, TempoTrack calls `pauseRunningAtLastSavedElapsed`.

Safe elapsed becomes:

```text
max(checkpoint.accumulatedNanos, lastLap.totalNanos or 0)
```

Then:

```text
status = PAUSED
startedAtNanos = null
```

No unsafe elapsed delta is added.

## Legacy running checkpoint behavior

Older running checkpoints can lack `savedAtEpochMillis`. They may also carry an `accumulatedNanos` value older than the newest lap.

Legacy recovery therefore:

- does not pretend the uptime reference is safe;
- pauses the timer;
- preserves the greatest known elapsed lower bound from accumulated time or last lap;
- persists the normalized state after application initialization.

## Desktop recovery

`System.nanoTime()` is valid for measuring intervals within one JVM but its origin is process-local and unspecified. A value saved by one JVM process must not be compared to a value from a newly launched JVM process.

Therefore Desktop recovery is deliberately simpler:

```text
persisted RUNNING -> PAUSED at last safely persisted elapsed
```

To reduce lost time when a Desktop process is killed while the timer is running, the shared app root persists a rebased checkpoint every five seconds.

Expected worst-case recent elapsed loss after an abrupt process termination is therefore approximately one heartbeat interval plus scheduling/storage delay, not the entire current run segment.

## Persistence moments

Meaningful state changes persist immediately:

- start;
- pause;
- resume;
- lap;
- reset clears active checkpoint.

Desktop additionally persists the running heartbeat.

Display refresh is **not** a persistence trigger. The UI can refresh every ~16–33 ms without causing continuous disk writes.

## Application startup recovery sequence

`TempoTrackApp` performs recovery before displaying normal stopwatch content:

1. load preferences;
2. apply platform preference side effects;
3. load active checkpoint;
4. call the platform-provided recovery function;
5. if recovery changed the checkpoint, persist the normalized result once;
6. create `StopwatchEngine` from recovered checkpoint and platform clocks;
7. expose engine to the host (Desktop uses this for keyboard/mini-window controls);
8. mark startup loaded.

This makes normalization explicit and avoids repeatedly reinterpreting the same unsafe legacy state on every launch.

## Failure cases and expected outcomes

| Scenario | Expected outcome |
|---|---|
| User changes wall clock while timer is running | Live elapsed remains correct because monotonic clock drives timing. Recovery may pause if wall/uptime deltas become implausibly different. |
| Android/iOS app process is killed/restarted without reboot | Running timer can continue if wall/uptime deltas agree. |
| Android/iOS device reboot | Uptime resets; persisted running timer restores PAUSED at last safe elapsed. |
| Desktop app process restarts | Persisted running timer restores PAUSED at last heartbeat/action checkpoint. |
| Persisted start timestamp is ahead of current monotonic reading | Engine/recovery pauses rather than producing negative elapsed. |
| Active store is malformed/oversized/invalid | Repository returns no active checkpoint; app starts from safe idle state. |
| Future active schema version appears | Decode fails closed; data is not silently rewritten as if understood. |
| Elapsed addition approaches `Long.MAX_VALUE` | Result saturates; it never wraps negative. |

## Verification map

Automated coverage lives primarily in:

- `StopwatchEngineTest.kt` — transitions, pause exclusion, sleep-like jumps, rebased checkpoints, overflow, stale origins, lap ceiling, reset;
- `StopwatchCheckpointValidationTest.kt` — structural invariants;
- `StopwatchCheckpointRecoveryTest.kt` — uptime/wall agreement and safe pause fallback;
- `ActiveStopwatchStoreCodecTest.kt` — schema v2/v1/legacy/future behavior;
- `ActiveStopwatchRepositoryTest.kt` — validation/migration/concurrency;
- `StopwatchJourneyTest.kt` — checkpoint restart integration journey.

Manual lifecycle verification is still required on real Android/iOS/Desktop environments. See [`testing.md`](testing.md).

## Related decisions

- [`adr/0001-monotonic-time.md`](adr/0001-monotonic-time.md)
- [`adr/0005-platform-checkpoint-recovery.md`](adr/0005-platform-checkpoint-recovery.md)
- [`architecture.md`](architecture.md)
- [`performance.md`](performance.md)
