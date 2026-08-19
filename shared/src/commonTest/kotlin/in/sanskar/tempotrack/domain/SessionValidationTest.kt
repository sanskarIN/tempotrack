package in.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SessionValidationTest {
    @Test
    fun acceptsConsistentSession() {
        val session = StopwatchSession(
            id = "session-1",
            name = "Intervals",
            createdAtEpochMillis = 1_700_000_000_000L,
            durationNanos = 3_000_000_000L,
            laps = listOf(
                Lap(index = 1, splitNanos = 1_000_000_000L, totalNanos = 1_000_000_000L),
                Lap(index = 2, splitNanos = 2_000_000_000L, totalNanos = 3_000_000_000L),
            ),
        )

        assertTrue(SessionValidation.validate(session).isEmpty())
    }

    @Test
    fun rejectsInconsistentLapTotals() {
        val session = StopwatchSession(
            id = "session-2",
            name = "Broken",
            createdAtEpochMillis = 1L,
            durationNanos = 5_000L,
            laps = listOf(Lap(index = 1, splitNanos = 2_000L, totalNanos = 3_000L)),
        )

        assertTrue(SessionValidation.validate(session).isNotEmpty())
        assertFailsWith<IllegalArgumentException> {
            SessionValidation.requireValid(session)
        }
    }

    @Test
    fun rejectsLapBeyondSessionDuration() {
        val session = StopwatchSession(
            id = "session-3",
            name = "Broken",
            createdAtEpochMillis = 1L,
            durationNanos = 1_000L,
            laps = listOf(Lap(index = 1, splitNanos = 2_000L, totalNanos = 2_000L)),
        )

        val errors = SessionValidation.validate(session)
        assertTrue(errors.any { it.contains("exceeds session duration") })
    }
}
