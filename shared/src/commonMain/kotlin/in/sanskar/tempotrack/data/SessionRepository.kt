package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.SessionValidation
import in.sanskar.tempotrack.domain.StopwatchSession

interface SessionRepository {
    suspend fun all(): List<StopwatchSession>
    suspend fun upsert(session: StopwatchSession)
    suspend fun rename(id: String, newName: String): Boolean
    suspend fun delete(id: String)
    suspend fun replaceAll(sessions: List<StopwatchSession>)
}

class JsonSessionRepository(
    private val storage: StringStorage,
    private val storeCodec: SessionStoreCodec = SessionStoreCodec(),
) : SessionRepository {
    override suspend fun all(): List<StopwatchSession> {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val decoded = storeCodec.decode(raw) ?: return emptyList()
        val normalized = decoded.sessions
            .filter { SessionValidation.validate(it).isEmpty() }
            .distinctBy(StopwatchSession::id)
            .sortedByDescending(StopwatchSession::createdAtEpochMillis)

        if (decoded.needsMigration) {
            persist(normalized)
        }
        return normalized
    }

    override suspend fun upsert(session: StopwatchSession) {
        SessionValidation.requireValid(session)
        val updated = all()
            .filterNot { it.id == session.id }
            .plus(session)
            .sortedByDescending(StopwatchSession::createdAtEpochMillis)
        persist(updated)
    }

    override suspend fun rename(id: String, newName: String): Boolean {
        if (id.isBlank()) return false
        val normalizedName = newName.trim()
        val current = all()
        val existing = current.firstOrNull { it.id == id } ?: return false
        val renamed = existing.copy(name = normalizedName)
        SessionValidation.requireValid(renamed)
        persist(
            current
                .map { if (it.id == id) renamed else it }
                .sortedByDescending(StopwatchSession::createdAtEpochMillis),
        )
        return true
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
        storage.write(storeCodec.encode(sessions))
    }
}
