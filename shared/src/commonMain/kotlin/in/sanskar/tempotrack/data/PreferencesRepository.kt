package in.sanskar.tempotrack.data

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
    override suspend fun load(): AppPreferences {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return AppPreferences()
        val decoded = codec.decode(raw) ?: return AppPreferences()
        if (decoded.needsMigration) {
            storage.write(codec.encode(decoded.preferences))
        }
        return decoded.preferences
    }

    override suspend fun save(preferences: AppPreferences) {
        storage.write(codec.encode(preferences))
    }
}
