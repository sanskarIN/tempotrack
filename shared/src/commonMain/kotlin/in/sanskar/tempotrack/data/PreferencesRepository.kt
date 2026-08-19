package in.sanskar.tempotrack.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

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
)

interface PreferencesRepository {
    suspend fun load(): AppPreferences
    suspend fun save(preferences: AppPreferences)
}

class JsonPreferencesRepository(
    private val storage: StringStorage,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : PreferencesRepository {
    override suspend fun load(): AppPreferences {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return AppPreferences()
        return try {
            json.decodeFromString(AppPreferences.serializer(), raw)
        } catch (_: SerializationException) {
            AppPreferences()
        } catch (_: IllegalArgumentException) {
            AppPreferences()
        }
    }

    override suspend fun save(preferences: AppPreferences) {
        storage.write(json.encodeToString(AppPreferences.serializer(), preferences))
    }
}
