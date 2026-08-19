package in.sanskar.tempotrack.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class PreferencesRepositoryTest {
    @Test
    fun saveAndLoadRoundTrip() = runTest {
        val storage = PreferencesTestStorage()
        val repository = JsonPreferencesRepository(storage)
        val expected = AppPreferences(
            theme = ThemePreference.DARK,
            largeControls = true,
            reducedMotion = true,
            onboardingCompleted = true,
        )

        repository.save(expected)

        assertEquals(expected, repository.load())
        assertTrue(storage.value.orEmpty().contains("\"schemaVersion\": 1"))
    }

    @Test
    fun corruptedPreferencesFallBackToDefaults() = runTest {
        val repository = JsonPreferencesRepository(PreferencesTestStorage("{broken"))

        assertEquals(AppPreferences(), repository.load())
    }

    @Test
    fun legacyPreferencesMigrateOnRead() = runTest {
        val expected = AppPreferences(theme = ThemePreference.LIGHT, onboardingCompleted = true)
        val legacy = Json.encodeToString(AppPreferences.serializer(), expected)
        val storage = PreferencesTestStorage(legacy)
        val repository = JsonPreferencesRepository(storage)

        assertEquals(expected, repository.load())
        assertTrue(storage.value.orEmpty().contains("\"schemaVersion\": 1"))
    }
}

private class PreferencesTestStorage(
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
