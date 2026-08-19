package in.sanskar.tempotrack.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

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
            miniStopwatchVisible = true,
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
    fun oversizedPreferencesFallBackToDefaultsWithoutRewrite() = runTest {
        val oversized = "x".repeat(MAX_PREFERENCES_STORE_CHARACTERS + 1)
        val storage = PreferencesTestStorage(oversized)
        val repository = JsonPreferencesRepository(storage)

        assertEquals(AppPreferences(), repository.load())
        assertEquals(oversized, storage.value)
    }

    @Test
    fun legacyPreferencesMigrateOnReadAndDefaultNewFields() = runTest {
        val legacy = """{"theme":"LIGHT","largeControls":false,"reducedMotion":false,"onboardingCompleted":true}"""
        val storage = PreferencesTestStorage(legacy)
        val repository = JsonPreferencesRepository(storage)

        val loaded = repository.load()

        assertEquals(ThemePreference.LIGHT, loaded.theme)
        assertTrue(loaded.onboardingCompleted)
        assertFalse(loaded.miniStopwatchVisible)
        assertTrue(storage.value.orEmpty().contains("\"schemaVersion\": 1"))
    }

    @Test
    fun concurrentSavesDoNotOverlapStorageWrites() = runTest {
        val storage = ConcurrentWriteDetectingStorage()
        val repository = JsonPreferencesRepository(storage)

        List(20) { index ->
            launch {
                repository.save(
                    AppPreferences(
                        theme = ThemePreference.entries[index % ThemePreference.entries.size],
                        largeControls = index % 2 == 0,
                        onboardingCompleted = true,
                    ),
                )
            }
        }.joinAll()

        assertNotNull(PreferencesStoreCodec().decode(requireNotNull(storage.value)))
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
