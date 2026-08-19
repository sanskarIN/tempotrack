package in.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StopwatchCheckpointValidationTest {
    @Test
    fun checkpointLapLimitMatchesSavedSessionLimit() {
        assertEquals(SessionValidation.MAX_LAPS_PER_SESSION, StopwatchCheckpointValidation.MAX_LAPS)
    }

    @Test
    fun acceptsValidRunningCheckpointWithLaps() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 0L,
            startedAtNanos = 100L,
            laps = listOf(
                Lap(index = 1, splitNanos = 10L, totalNanos = 10L),
                Lap(index = 2, splitNanos = 15L, totalNanos = 25L),
            ),
        )

        assertTrue(StopwatchCheckpointValidation.isValid(checkpoint))
    }

    @Test
    fun rejectsRunningCheckpointWithoutStartTimestamp() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 10L,
            startedAtNanos = null,
        )

        assertFalse(StopwatchCheckpointValidation.isValid(checkpoint))
    }

    @Test
    fun rejectsPausedCheckpointWhenLapsExceedElapsedTime() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.PAUSED,
            accumulatedNanos = 10L,
            laps = listOf(Lap(index = 1, splitNanos = 20L, totalNanos = 20L)),
        )

        assertFalse(StopwatchCheckpointValidation.isValid(checkpoint))
    }

    @Test
    fun rejectsNonSequentialLapIndexes() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.PAUSED,
            accumulatedNanos = 20L,
            laps = listOf(Lap(index = 2, splitNanos = 20L, totalNanos = 20L)),
        )

        assertFalse(StopwatchCheckpointValidation.isValid(checkpoint))
    }

    @Test
    fun rejectsIdleCheckpointWithRecordedState() {
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.IDLE,
            accumulatedNanos = 1L,
            startedAtNanos = 1L,
            laps = listOf(Lap(index = 1, splitNanos = 1L, totalNanos = 1L)),
        )

        assertFalse(StopwatchCheckpointValidation.isValid(checkpoint))
    }
}
