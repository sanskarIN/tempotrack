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
        laps = listOf(
            Lap(index = 1, splitNanos = 10L, totalNanos = 10L),
            Lap(index = 2, splitNanos = 10L, totalNanos = 20L),
        ),
    )
}
