package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.StopwatchCheckpoint
import in.sanskar.tempotrack.domain.StopwatchSession
import in.sanskar.tempotrack.domain.StopwatchStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RepositoryIntegrationTest {
    @Test
    fun sessionRepositoryRoundTripsAndSortsNewestFirst() = runTest {
        val storage = MemoryStorage()
        val repository = JsonSessionRepository(storage)

        repository.upsert(session(id = "old", createdAt = 10L))
        repository.upsert(session(id = "new", createdAt = 20L))

        assertEquals(listOf("new", "old"), repository.all().map(StopwatchSession::id))
    }

    @Test
    fun corruptSessionStorageFailsClosed() = runTest {
        val repository = JsonSessionRepository(MemoryStorage("{not valid json"))

        assertTrue(repository.all().isEmpty())
    }

    @Test
    fun preferencesRoundTripAndCorruptionReturnsDefaults() = runTest {
        val storage = MemoryStorage()
        val repository = JsonPreferencesRepository(storage)
        val expected = AppPreferences(
            theme = ThemePreference.DARK,
            largeControls = true,
            reducedMotion = true,
            onboardingCompleted = true,
        )

        repository.save(expected)
        assertEquals(expected, repository.load())

        storage.write("not-json")
        assertEquals(AppPreferences(), repository.load())
    }

    @Test
    fun activeCheckpointRoundTripsClearsAndRejectsCorruption() = runTest {
        val storage = MemoryStorage()
        val repository = JsonActiveStopwatchRepository(storage)
        val checkpoint = StopwatchCheckpoint(
            status = StopwatchStatus.PAUSED,
            accumulatedNanos = 123_456_789L,
            startedAtNanos = null,
        )

        repository.save(checkpoint)
        assertEquals(checkpoint, repository.load())

        repository.clear()
        assertNull(repository.load())

        storage.write("broken-checkpoint")
        assertNull(repository.load())
    }

    private fun session(id: String, createdAt: Long) = StopwatchSession(
        id = id,
        name = id,
        createdAtEpochMillis = createdAt,
        durationNanos = createdAt,
        laps = emptyList(),
    )
}

private class MemoryStorage(
    private var value: String? = null,
) : StringStorage {
    override suspend fun read(): String? = value

    override suspend fun write(content: String) {
        value = content
    }

    override suspend fun clear() {
        value = null
    }
}
