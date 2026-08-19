# ADR 0001: Use monotonic time for elapsed duration

- Status: Accepted
- Date: 2026-08-19

## Context

Wall clocks can jump due to network time, manual changes, timezone settings, or daylight-saving adjustments. A stopwatch must not change elapsed duration because calendar time changes.

## Decision

`StopwatchEngine` accepts a `MonotonicClock`.

- Android supplies `SystemClock.elapsedRealtimeNanos()`, which includes deep sleep.
- Desktop supplies `System.nanoTime()`.
- Tests supply a deterministic fake clock.

Wall time is used only for saved-session metadata.

## Consequences

The engine is deterministic and sleep-safe on Android. Checkpoints can survive same-boot process recreation. Because monotonic epochs can reset on OS reboot, a stale restored running checkpoint is converted to paused so the accumulated duration remains valid and can be resumed explicitly.
