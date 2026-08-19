package `in`.sanskar.tempotrack.data

import `in`.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal const val CURRENT_SESSION_SCHEMA_VERSION = 1

@Serializable
internal data class SessionStoreEnvelope(
    val schemaVersion: Int = CURRENT_SESSION_SCHEMA_VERSION,
    val sessions: List<StopwatchSession> = emptyList(),
)

internal data class DecodedSessionStore(
    val sessions: List<StopwatchSession>,
    val needsMigration: Boolean,
)

internal class SessionStoreCodec(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    private val legacySerializer = ListSerializer(StopwatchSession.serializer())

    fun decode(raw: String): DecodedSessionStore? {
        if (raw.isBlank()) return DecodedSessionStore(emptyList(), needsMigration = false)

        val envelope = decodeEnvelope(raw)
        if (envelope != null) {
            if (envelope.schemaVersion != CURRENT_SESSION_SCHEMA_VERSION) return null
            return DecodedSessionStore(envelope.sessions, needsMigration = false)
        }

        val legacy = decodeLegacy(raw) ?: return null
        return DecodedSessionStore(legacy, needsMigration = true)
    }

    fun encode(sessions: List<StopwatchSession>): String = json.encodeToString(
        SessionStoreEnvelope.serializer(),
        SessionStoreEnvelope(sessions = sessions),
    )

    private fun decodeEnvelope(raw: String): SessionStoreEnvelope? = try {
        json.decodeFromString(SessionStoreEnvelope.serializer(), raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decodeLegacy(raw: String): List<StopwatchSession>? = try {
        json.decodeFromString(legacySerializer, raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
