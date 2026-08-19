package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.Lap
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionImportTest {
    @Test
    fun restoreLimitsMatchPersistenceLimits() {
        assertEquals(MAX_SESSION_STORE_CHARACTERS, SessionImporter.MAX_IMPORT_CHARACTERS)
        assertEquals(MAX_STORED_SESSIONS, SessionImporter.MAX_IMPORT_SESSIONS)
    }

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
        assertEquals(SessionImportError.DUPLICATE_SESSION_IDS, failure.error)
    }

    @Test
    fun rejectsMalformedJsonWithoutRetainingInput() {
        val result = SessionImporter.fromJson("[{\"token\":\"do-not-repeat\"")

        val failure = assertIs<SessionImportResult.Failure>(result)
        assertEquals(SessionImportError.INVALID_JSON, failure.error)
        assertEquals(null, failure.invalidSessionNumber)
    }

    @Test
    fun reportsInvalidSessionPositionWithoutExposingValidationDetails() {
        val invalid = StopwatchSession(
            id = "invalid",
            name = "Broken",
            createdAtEpochMillis = 100L,
            durationNanos = 1L,
            laps = listOf(Lap(index = 2, splitNanos = 1L, totalNanos = 1L)),
        )

        val failure = assertIs<SessionImportResult.Failure>(
            SessionImporter.fromJson(SessionCodec.toJson(listOf(invalid))),
        )

        assertEquals(SessionImportError.INVALID_SESSION, failure.error)
        assertEquals(1, failure.invalidSessionNumber)
    }
}
