package in.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StopwatchCheckpointRecoveryTest {
    @Test
    fun pausesRunningCheckpointAtPersistedElapsed() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 8 * NANOS_PER_SECOND,
            startedAtNanos = -100L,
        )

        val recovered = StopwatchCheckpointRecovery.pauseRunningAtLastSavedElapsed(checkpoint)

        assertEquals(StopwatchStatus.PAUSED, recovered.status)
        assertEquals(8 * NANOS_PER_SECOND, recovered.accumulatedNanos)
        assertEquals(null, recovered.startedAtNanos)
    }

    @Test
    fun legacyRunningCheckpointUsesLastLapAsSafeMinimum() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 0L,
            startedAtNanos = 500L,
            laps = listOf(
                Lap(
                    index = 1,
                    splitNanos = 5 * NANOS_PER_SECOND,
                    totalNanos = 5 * NANOS_PER_SECOND,
                ),
            ),
        )

        val recovered = StopwatchCheckpointRecovery.pauseRunningAtLastSavedElapsed(checkpoint)

        assertEquals(StopwatchStatus.PAUSED, recovered.status)
        assertEquals(5 * NANOS_PER_SECOND, recovered.accumulatedNanos)
        assertEquals(checkpoint.laps, recovered.laps)
    }

    @Test
    fun systemUptimeRecoveryKeepsConsistentRunningCheckpoint() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 5 * NANOS_PER_SECOND,
            startedAtNanos = 10 * NANOS_PER_SECOND,
            savedAtEpochMillis = 1_000_000L,
        )

        val recovered = StopwatchCheckpointRecovery.recoverSystemUptimeCheckpoint(
            checkpoint = checkpoint,
            currentMonotonicNanos = 40 * NANOS_PER_SECOND,
            currentEpochMillis = 1_030_000L,
        )

        assertEquals(checkpoint, recovered)
    }

    @Test
    fun systemUptimeRecoveryPausesWhenClockOriginsNoLongerAgree() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 5 * NANOS_PER_SECOND,
            startedAtNanos = 10 * NANOS_PER_SECOND,
            savedAtEpochMillis = 1_000_000L,
        )

        val recovered = StopwatchCheckpointRecovery.recoverSystemUptimeCheckpoint(
            checkpoint = checkpoint,
            currentMonotonicNanos = 20 * NANOS_PER_SECOND,
            currentEpochMillis = 2_000_000L,
        )

        assertEquals(StopwatchStatus.PAUSED, recovered.status)
        assertEquals(5 * NANOS_PER_SECOND, recovered.accumulatedNanos)
        assertEquals(null, recovered.startedAtNanos)
    }

    @Test
    fun systemUptimeRecoveryPausesLegacyCheckpointWithoutWallTimestamp() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 4 * NANOS_PER_SECOND,
            startedAtNanos = 10 * NANOS_PER_SECOND,
            savedAtEpochMillis = null,
        )

        val recovered = StopwatchCheckpointRecovery.recoverSystemUptimeCheckpoint(
            checkpoint = checkpoint,
            currentMonotonicNanos = 12 * NANOS_PER_SECOND,
            currentEpochMillis = 2_000_000L,
        )

        assertEquals(StopwatchStatus.PAUSED, recovered.status)
        assertEquals(4 * NANOS_PER_SECOND, recovered.accumulatedNanos)
    }

    @Test
    fun nonRunningCheckpointIsUnchanged() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.PAUSED,
            accumulatedNanos = 3 * NANOS_PER_SECOND,
        )

        assertEquals(checkpoint, StopwatchCheckpointRecovery.pauseRunningAtLastSavedElapsed(checkpoint))
        assertEquals(
            checkpoint,
            StopwatchCheckpointRecovery.recoverSystemUptimeCheckpoint(
                checkpoint = checkpoint,
                currentMonotonicNanos = 100L,
                currentEpochMillis = 100L,
            ),
        )
    }
}
