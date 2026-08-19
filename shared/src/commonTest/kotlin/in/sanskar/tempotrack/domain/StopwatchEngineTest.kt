package in.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StopwatchEngineTest {
    private val clock = FakeClock()
    private val engine = StopwatchEngine(clock)

    @Test
    fun pauseResumeDoesNotCountPausedTime() {
        engine.start()
        clock.advanceSeconds(5)
        engine.pause()

        clock.advanceSeconds(100)
        assertEquals(5_000L, engine.snapshot().elapsedMillis)

        engine.resume()
        clock.advanceSeconds(2)
        assertEquals(7_000L, engine.snapshot().elapsedMillis)
    }

    @Test
    fun elapsedTimeAdvancesAcrossDeviceSleepLikeClockJump() {
        engine.start()

        // Platform clocks are required to include sleep on Android
        // (SystemClock.elapsedRealtimeNanos), represented here as a jump.
        clock.advanceSeconds(3_600)

        assertEquals(3_600_000L, engine.snapshot().elapsedMillis)
    }

    @Test
    fun lapsUseSplitAndCumulativeTime() {
        engine.start()
        clock.advanceSeconds(2)
        val first = engine.lap().laps.single()

        clock.advanceSeconds(3)
        val second = engine.lap().laps.last()

        assertEquals(2_000L, first.splitNanos / NANOS_PER_MILLISECOND)
        assertEquals(2_000L, first.totalNanos / NANOS_PER_MILLISECOND)
        assertEquals(3_000L, second.splitNanos / NANOS_PER_MILLISECOND)
        assertEquals(5_000L, second.totalNanos / NANOS_PER_MILLISECOND)
    }

    @Test
    fun staleRunningCheckpointNeverCreatesNegativeDuration() {
        val stale = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 5 * NANOS_PER_SECOND,
            startedAtNanos = 999 * NANOS_PER_SECOND,
        )
        val rebootedClock = FakeClock(now = 2 * NANOS_PER_SECOND)
        val restored = StopwatchEngine(rebootedClock, stale)

        assertEquals(StopwatchStatus.PAUSED, restored.snapshot().status)
        assertTrue(restored.snapshot().elapsedNanos >= 5 * NANOS_PER_SECOND)

        restored.resume()
        rebootedClock.advanceSeconds(2)
        assertEquals(7_000L, restored.snapshot().elapsedMillis)
    }

    @Test
    fun resetClearsStateAndLaps() {
        engine.start()
        clock.advanceSeconds(1)
        engine.lap()
        val reset = engine.reset()

        assertEquals(StopwatchStatus.IDLE, reset.status)
        assertEquals(0L, reset.elapsedNanos)
        assertTrue(reset.laps.isEmpty())
    }
}

private class FakeClock(
    var now: Long = 0L,
) : MonotonicClock {
    override fun nowNanos(): Long = now

    fun advanceSeconds(seconds: Long) {
        now += seconds * NANOS_PER_SECOND
    }
}
