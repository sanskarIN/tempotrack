package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.Lap
import in.sanskar.tempotrack.domain.StopwatchCheckpoint
import in.sanskar.tempotrack.domain.StopwatchStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class ActiveStopwatchStoreCodecTest {
    private val codec = ActiveStopwatchStoreCodec()

    @Test
    fun versionedCheckpointRoundTrips() {
        val checkpoint = sampleCheckpoint()
        val decoded = requireNotNull(codec.decode(codec.encode(checkpoint)))

        assertEquals(checkpoint, decoded.checkpoint)
        assertFalse(decoded.needsMigration)
    }

    @Test
    fun versionOneEnvelopeMigratesToCurrentSchema() {
        val versionOne = """
            {
              "schemaVersion": 1,
              "checkpoint": {
                "status": "PAUSED",
                "accumulatedNanos": 20,
                "laps": []
              }
            }
        """.trimIndent()

        val decoded = requireNotNull(codec.decode(versionOne))

        assertEquals(StopwatchStatus.PAUSED, decoded.checkpoint.status)
        assertEquals(20L, decoded.checkpoint.accumulatedNanos)
        assertEquals(null, decoded.checkpoint.savedAtEpochMillis)
        assertTrue(decoded.needsMigration)
    }

    @Test
    fun legacyCheckpointIsDetectedForMigration() {
        val checkpoint = sampleCheckpoint()
        val legacy = Json.encodeToString(StopwatchCheckpoint.serializer(), checkpoint)
        val decoded = requireNotNull(codec.decode(legacy))

        assertEquals(checkpoint, decoded.checkpoint)
        assertTrue(decoded.needsMigration)
    }

    @Test
    fun unsupportedFutureSchemaFailsClosed() {
        val future = """{"schemaVersion":999,"checkpoint":{"status":"IDLE","accumulatedNanos":0,"laps":[]}}"""

        assertNull(codec.decode(future))
    }

    private fun sampleCheckpoint(): StopwatchCheckpoint = StopwatchCheckpoint(
        status = StopwatchStatus.PAUSED,
        accumulatedNanos = 20L,
        savedAtEpochMillis = 1_700_000_000_000L,
        laps = listOf(
            Lap(index = 1, splitNanos = 10L, totalNanos = 10L),
            Lap(index = 2, splitNanos = 10L, totalNanos = 20L),
        ),
    )
}
