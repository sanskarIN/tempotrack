package `in`.sanskar.tempotrack.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal const val CURRENT_PREFERENCES_SCHEMA_VERSION = 1

@Serializable
internal data class PreferencesStoreEnvelope(
    val schemaVersion: Int = CURRENT_PREFERENCES_SCHEMA_VERSION,
    val preferences: AppPreferences = AppPreferences(),
)

internal data class DecodedPreferencesStore(
    val preferences: AppPreferences,
    val needsMigration: Boolean,
)

internal class PreferencesStoreCodec(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    fun decode(raw: String): DecodedPreferencesStore? {
        if (raw.isBlank()) return null

        decodeEnvelope(raw)?.let { envelope ->
            if (envelope.schemaVersion != CURRENT_PREFERENCES_SCHEMA_VERSION) return null
            return DecodedPreferencesStore(envelope.preferences, needsMigration = false)
        }

        val legacy = decodeLegacy(raw) ?: return null
        return DecodedPreferencesStore(legacy, needsMigration = true)
    }

    fun encode(preferences: AppPreferences): String = json.encodeToString(
        PreferencesStoreEnvelope.serializer(),
        PreferencesStoreEnvelope(preferences = preferences),
    )

    private fun decodeEnvelope(raw: String): PreferencesStoreEnvelope? = try {
        json.decodeFromString(PreferencesStoreEnvelope.serializer(), raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decodeLegacy(raw: String): AppPreferences? = try {
        json.decodeFromString(AppPreferences.serializer(), raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
