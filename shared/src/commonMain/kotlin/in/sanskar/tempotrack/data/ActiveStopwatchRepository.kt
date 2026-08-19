package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.StopwatchCheckpoint
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface ActiveStopwatchRepository {
    suspend fun load(): StopwatchCheckpoint?
    suspend fun save(checkpoint: StopwatchCheckpoint)
    suspend fun clear()
}

class JsonActiveStopwatchRepository(
    private val storage: StringStorage,
    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : ActiveStopwatchRepository {
    override suspend fun load(): StopwatchCheckpoint? {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return null
        return try {
            json.decodeFromString(StopwatchCheckpoint.serializer(), raw)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override suspend fun save(checkpoint: StopwatchCheckpoint) {
        storage.write(json.encodeToString(StopwatchCheckpoint.serializer(), checkpoint))
    }

    override suspend fun clear() {
        storage.clear()
    }
}
