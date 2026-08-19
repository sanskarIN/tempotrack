package `in`.sanskar.tempotrack.data

import `in`.sanskar.tempotrack.domain.StopwatchCheckpoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal const val CURRENT_ACTIVE_STOPWATCH_SCHEMA_VERSION = 2
private const val PREVIOUS_ACTIVE_STOPWATCH_SCHEMA_VERSION = 1

@Serializable
internal data class ActiveStopwatchStoreEnvelope(
    val schemaVersion: Int = CURRENT_ACTIVE_STOPWATCH_SCHEMA_VERSION,
    val checkpoint: StopwatchCheckpoint = StopwatchCheckpoint(),
)

internal data class DecodedActiveStopwatchStore(
    val checkpoint: StopwatchCheckpoint,
    val needsMigration: Boolean,
)

internal class ActiveStopwatchStoreCodec(
    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    fun decode(raw: String): DecodedActiveStopwatchStore? {
        if (raw.isBlank()) return null

        decodeEnvelope(raw)?.let { envelope ->
            return when (envelope.schemaVersion) {
                CURRENT_ACTIVE_STOPWATCH_SCHEMA_VERSION -> DecodedActiveStopwatchStore(
                    checkpoint = envelope.checkpoint,
                    needsMigration = false,
                )

                PREVIOUS_ACTIVE_STOPWATCH_SCHEMA_VERSION -> DecodedActiveStopwatchStore(
                    checkpoint = envelope.checkpoint,
                    needsMigration = true,
                )

                else -> null
            }
        }

        val legacy = decodeLegacy(raw) ?: return null
        return DecodedActiveStopwatchStore(legacy, needsMigration = true)
    }

    fun encode(checkpoint: StopwatchCheckpoint): String = json.encodeToString(
        ActiveStopwatchStoreEnvelope.serializer(),
        ActiveStopwatchStoreEnvelope(checkpoint = checkpoint),
    )

    private fun decodeEnvelope(raw: String): ActiveStopwatchStoreEnvelope? = try {
        json.decodeFromString(ActiveStopwatchStoreEnvelope.serializer(), raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decodeLegacy(raw: String): StopwatchCheckpoint? = try {
        json.decodeFromString(StopwatchCheckpoint.serializer(), raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
