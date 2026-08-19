package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.StopwatchSession
import kotlin.test.Test
import kotlin.test.assertEquals
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
