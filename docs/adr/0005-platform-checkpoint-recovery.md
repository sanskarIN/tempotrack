# ADR 0005 — Recover active timers according to monotonic clock lifetime

- Status: Accepted
- Date: 2026-08-19

## Context

TempoTrack calculates elapsed duration with monotonic clocks, but those clocks do not all have the same persistence lifetime.

Android `SystemClock.elapsedRealtimeNanos()` and iOS `NSProcessInfo.systemUptime` are system-uptime references. They survive ordinary process restarts on the same boot but reset when the operating system restarts.

Desktop uses JVM `System.nanoTime()`. Its origin is only meaningful for elapsed-time measurements inside the same JVM process and must not be interpreted as a durable cross-process timestamp.

Persisting only a raw monotonic start reading can therefore create incorrect recovery after a reboot or new Desktop JVM.

## Decision

1. Live elapsed-time calculations remain exclusively monotonic.
2. Every persisted RUNNING checkpoint is rebased at save time:
   - `accumulatedNanos` stores elapsed-at-save;
   - `startedAtNanos` stores the monotonic reading at that same save point;
   - `savedAtEpochMillis` stores a wall-clock save timestamp used only for recovery validation.
3. Active-checkpoint persistence uses schema version 2. Version 1 and original unversioned checkpoints migrate forward after validation.
4. Android and iOS compare monotonic elapsed since save with wall elapsed since save. If those deltas are reasonably consistent, the running checkpoint can continue. If not, recovery pauses at the last safely persisted elapsed value.
5. Legacy running checkpoints that lack wall metadata fail safely to PAUSED once rather than guessing whether the uptime origin is still valid.
6. Desktop always converts a persisted RUNNING checkpoint to PAUSED when a new process starts.
7. Desktop persists a rebased running checkpoint every five seconds so a forced process termination loses only a bounded recent interval before safe recovery.
8. Any recovery transformation is persisted during initialization so it is not repeatedly reinterpreted on future launches.

## Consequences

### Positive

- Live timing never depends on wall-clock adjustments.
- Android/iOS can preserve a running timer across ordinary process death on the same boot.
- Reboot/uptime resets cannot silently produce a huge, negative, or stalled elapsed duration.
- Desktop no longer compares `System.nanoTime()` readings from different JVM processes.
- Existing version-1 checkpoints remain readable.

### Trade-offs

- A legacy Android/iOS running checkpoint without `savedAtEpochMillis` is recovered as paused even when the device did not reboot.
- Desktop process restart intentionally changes RUNNING to PAUSED because exact cross-process continuation is not justified by the JVM monotonic-clock contract.
- A forced Desktop process exit can lose up to roughly one heartbeat interval of elapsed time.
- Wall-clock time is now stored as recovery metadata, but it is never used to calculate live elapsed duration.

## Verification

Common tests cover checkpoint rebasing, v1→v2 migration, same-boot uptime recovery, reset/reboot mismatch recovery, legacy safe pause, negative monotonic origins, stale-lap preservation, and elapsed overflow saturation.

Platform builds and lifecycle behavior must still be verified on the appropriate Android/iOS/Desktop hosts before a release is declared complete.
