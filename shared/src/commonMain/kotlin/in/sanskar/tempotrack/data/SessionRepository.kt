package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.SessionValidation
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface SessionRepository {
    suspend fun all(): List<StopwatchSession>
    suspend fun upsert(session: StopwatchSession)
    suspend fun delete(id: String)
    suspend fun replaceAll(sessions: List<StopwatchSession>)
}

class JsonSessionRepository(
    private val storage: StringStorage,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
        exceptionsWithDebugInfo = false
    },
) : SessionRepository {
    private val serializer = ListSerializer(StopwatchSession.serializer())

    override suspend fun all(): List<StopwatchSession> {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
                .filter { SessionValidation.validate(it).isEmpty() }
                .distinctBy(StopwatchSession::id)
                .sortedByDescending(StopwatchSession::createdAtEpochMillis)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    override suspend fun upsert(session: StopwatchSession) {
        SessionValidation.requireValid(session)
        val updated = all()
            .filterNot { it.id == session.id }
            .plus(session)
            .sortedByDescending(StopwatchSession::createdAtEpochMillis)
        persist(updated)
    }

    override suspend fun delete(id: String) {
        if (id.isBlank()) return
        persist(all().filterNot { it.id == id })
    }

    override suspend fun replaceAll(sessions: List<StopwatchSession>) {
        require(sessions.map(StopwatchSession::id).distinct().size == sessions.size) {
            "Session ids must be unique."
        }
        sessions.forEach(SessionValidation::requireValid)
        persist(sessions.sortedByDescending(StopwatchSession::createdAtEpochMillis))
    }

    private suspend fun persist(sessions: List<StopwatchSession>) {
        storage.write(json.encodeToString(serializer, sessions))
    }
}
