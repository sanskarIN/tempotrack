package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.Lap
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SessionStoreCodecTest {
    private val codec = SessionStoreCodec()
    private val session = StopwatchSession(
        id = "session-1",
        name = "Intervals",
        createdAtEpochMillis = 123L,
        durationNanos = 1_000L,
        laps = listOf(Lap(1, 1_000L, 1_000L)),
    )

    @Test
    fun roundTripUsesCurrentSchema() {
        val decoded = codec.decode(codec.encode(listOf(session)))

        assertNotNull(decoded)
        assertFalse(decoded.needsMigration)
        assertEquals(listOf(session), decoded.sessions)
    }

    @Test
    fun legacyListIsAcceptedAndMarkedForMigration() {
        val legacy = Json.encodeToString(
            ListSerializer(StopwatchSession.serializer()),
            listOf(session),
        )
        val decoded = codec.decode(legacy)

        assertNotNull(decoded)
        assertTrue(decoded.needsMigration)
        assertEquals(listOf(session), decoded.sessions)
    }

    @Test
    fun unknownFutureSchemaFailsClosed() {
        val future = """{"schemaVersion":999,"sessions":[]}"""
        assertNull(codec.decode(future))
    }
}
