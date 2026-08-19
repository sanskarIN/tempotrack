package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.Lap
import in.sanskar.tempotrack.domain.StopwatchCheckpoint
import in.sanskar.tempotrack.domain.StopwatchStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ActiveStopwatchRepositoryTest {
    @Test
    fun validCheckpointPersistsAndLoads() = runTest {
        val storage = ActiveTestStorage()
        val repository = JsonActiveStopwatchRepository(storage)
        val checkpoint = validCheckpoint()

        repository.save(checkpoint)

        assertEquals(checkpoint, repository.load())
        assertTrue(storage.value.orEmpty().contains("\"schemaVersion\":1"))
    }

    @Test
    fun invalidCheckpointIsRejectedBeforeSave() = runTest {
        val repository = JsonActiveStopwatchRepository(ActiveTestStorage())
        val invalid = StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = 10L,
            startedAtNanos = null,
        )

        assertFailsWith<IllegalArgumentException> {
            repository.save(invalid)
        }
    }

    @Test
    fun invalidStoredCheckpointFailsClosed() = runTest {
        val invalid = StopwatchCheckpoint(
            status = StopwatchStatus.PAUSED,
            accumulatedNanos = 5L,
            laps = listOf(Lap(index = 1, splitNanos = 10L, totalNanos = 10L)),
        )
        val storage = ActiveTestStorage(ActiveStopwatchStoreCodec().encode(invalid))
        val repository = JsonActiveStopwatchRepository(storage)

        assertNull(repository.load())
    }

    @Test
    fun legacyCheckpointMigratesOnRead() = runTest {
        val checkpoint = validCheckpoint()
        val legacy = Json.encodeToString(StopwatchCheckpoint.serializer(), checkpoint)
        val storage = ActiveTestStorage(legacy)
        val repository = JsonActiveStopwatchRepository(storage)

        assertNotNull(repository.load())
        assertTrue(storage.value.orEmpty().contains("\"schemaVersion\":1"))
    }

    private fun validCheckpoint(): StopwatchCheckpoint = StopwatchCheckpoint(
        status = StopwatchStatus.PAUSED,
        accumulatedNanos = 20L,
        laps = listOf(
            Lap(index = 1, splitNanos = 10L, totalNanos = 10L),
            Lap(index = 2, splitNanos = 10L, totalNanos = 20L),
        ),
    )
}

private class ActiveTestStorage(
    var value: String? = null,
) : StringStorage {
    override suspend fun read(): String? = value

    override suspend fun write(content: String) {
        value = content
    }

    override suspend fun clear() {
        value = null
    }
}
