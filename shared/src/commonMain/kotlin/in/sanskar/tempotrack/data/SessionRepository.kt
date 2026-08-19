package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.SessionValidation
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val mutex = Mutex()

    override suspend fun all(): List<StopwatchSession> = mutex.withLock {
        loadUnlocked()
    }

    override suspend fun upsert(session: StopwatchSession) = mutex.withLock {
        SessionValidation.requireValid(session)
        val updated = loadUnlocked()
            .filterNot { it.id == session.id }
            .plus(session)
            .sortedByDescending(StopwatchSession::createdAtEpochMillis)
        persistUnlocked(updated)
    }

    override suspend fun rename(id: String, newName: String): Boolean = mutex.withLock {
        if (id.isBlank()) return@withLock false
        val normalizedName = newName.trim()
        val current = loadUnlocked()
        val existing = current.firstOrNull { it.id == id } ?: return@withLock false
        val renamed = existing.copy(name = normalizedName)
        SessionValidation.requireValid(renamed)
        persistUnlocked(
            current
                .map { if (it.id == id) renamed else it }
                .sortedByDescending(StopwatchSession::createdAtEpochMillis),
        )
        true
    }

    override suspend fun delete(id: String) = mutex.withLock {
        if (id.isBlank()) return@withLock
        persistUnlocked(loadUnlocked().filterNot { it.id == id })
    }

    override suspend fun replaceAll(sessions: List<StopwatchSession>) = mutex.withLock {
        require(sessions.map(StopwatchSession::id).distinct().size == sessions.size) {
            "Session ids must be unique."
        }
        sessions.forEach(SessionValidation::requireValid)
        persistUnlocked(sessions.sortedByDescending(StopwatchSession::createdAtEpochMillis))
    }

    private suspend fun loadUnlocked(): List<StopwatchSession> {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val decoded = storeCodec.decode(raw) ?: return emptyList()
        val normalized = decoded.sessions
            .filter { SessionValidation.validate(it).isEmpty() }
            .distinctBy(StopwatchSession::id)
            .sortedByDescending(StopwatchSession::createdAtEpochMillis)

        if (decoded.needsMigration) {
            persistUnlocked(normalized)
        }
        return normalized
    }

    private suspend fun persistUnlocked(sessions: List<StopwatchSession>) {
        storage.write(storeCodec.encode(sessions))
    }
}
