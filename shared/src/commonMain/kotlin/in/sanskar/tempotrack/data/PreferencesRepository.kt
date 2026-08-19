package in.sanskar.tempotrack.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

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
        val decoded = codec.decode(raw) ?: return@withLock AppPreferences()
        if (decoded.needsMigration) {
            storage.write(codec.encode(decoded.preferences))
        }
        decoded.preferences
    }

    override suspend fun save(preferences: AppPreferences) = mutex.withLock {
        storage.write(codec.encode(preferences))
    }
}
