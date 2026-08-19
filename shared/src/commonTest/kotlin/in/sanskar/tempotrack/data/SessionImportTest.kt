package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.Lap
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SessionImportTest {
    @Test
    fun importsValidBackup() {
        val sessions = listOf(
            StopwatchSession(
                id = "one",
                name = "Morning",
                createdAtEpochMillis = 100L,
                durationNanos = 2_000L,
                laps = listOf(Lap(1, 2_000L, 2_000L)),
            ),
            StopwatchSession(
                id = "two",
                name = "Evening",
                createdAtEpochMillis = 200L,
                durationNanos = 1_000L,
                laps = emptyList(),
            ),
        )

        val result = SessionImporter.fromJson(SessionCodec.toJson(sessions))
        val success = assertIs<SessionImportResult.Success>(result)
        assertEquals(listOf("two", "one"), success.sessions.map(StopwatchSession::id))
    }

    @Test
    fun rejectsDuplicateIds() {
        val repeated = StopwatchSession(
            id = "duplicate",
            name = "Session",
            createdAtEpochMillis = 100L,
            durationNanos = 0L,
            laps = emptyList(),
        )
        val result = SessionImporter.fromJson(SessionCodec.toJson(listOf(repeated, repeated)))

        val failure = assertIs<SessionImportResult.Failure>(result)
        assertTrue(failure.userMessage.contains("duplicate"))
    }

    @Test
    fun rejectsMalformedJsonWithoutEchoingInput() {
        val secretLookingInput = "[{\"token\":\"do-not-repeat\""
        val result = SessionImporter.fromJson(secretLookingInput)

        val failure = assertIs<SessionImportResult.Failure>(result)
        assertTrue("do-not-repeat" !in failure.userMessage)
    }
}
