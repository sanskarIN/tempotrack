package in.sanskar.tempotrack.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

const val MAX_PREFERENCES_STORE_CHARACTERS: Int = 100_000

@Serializable
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class AppPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val largeControls: Boolean = false,
    val reducedMotion: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val miniStopwatchVisible: Boolean = false,
    val keyboardShortcutsEnabled: Boolean = true,
)

interface PreferencesRepository {
    suspend fun load(): AppPreferences
    suspend fun save(preferences: AppPreferences)
}

class JsonPreferencesRepository(
    private val storage: StringStorage,
    private val codec: PreferencesStoreCodec = PreferencesStoreCodec(),
) : PreferencesRepository {
    private val mutex = Mutex()

    override suspend fun load(): AppPreferences = mutex.withLock {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return@withLock AppPreferences()
        if (raw.length > MAX_PREFERENCES_STORE_CHARACTERS) return@withLock AppPreferences()

        val decoded = codec.decode(raw) ?: return@withLock AppPreferences()
        if (decoded.needsMigration) {
            persistUnlocked(decoded.preferences)
        }
        decoded.preferences
    }

    override suspend fun save(preferences: AppPreferences) = mutex.withLock {
        persistUnlocked(preferences)
    }

    private suspend fun persistUnlocked(preferences: AppPreferences) {
        val encoded = codec.encode(preferences)
        require(encoded.length <= MAX_PREFERENCES_STORE_CHARACTERS) {
            "Preferences payload is too large."
        }
        storage.write(encoded)
    }
}
