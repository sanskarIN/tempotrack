package `in`.sanskar.tempotrack.data

import `in`.sanskar.tempotrack.domain.SessionValidation
import `in`.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val MAX_STORED_SESSIONS: Int = 10_000
const val MAX_SESSION_STORE_CHARACTERS: Int = 20_000_000

class SessionStoreCorruptionException : IllegalStateException("Saved session history is invalid or unsupported.")

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
        val current = loadUnlocked()
        val updated = current
            .filterNot { it.id == session.id }
            .plus(session)
            .sortedByDescending(StopwatchSession::createdAtEpochMillis)
        require(updated.size <= MAX_STORED_SESSIONS) { "Too many saved sessions." }
        if (updated == current) return@withLock
        persistUnlocked(updated)
    }

    override suspend fun rename(id: String, newName: String): Boolean = mutex.withLock {
        if (id.isBlank()) return@withLock false
        val normalizedName = newName.trim()
        val current = loadUnlocked()
        val existing = current.firstOrNull { it.id == id } ?: return@withLock false
        if (existing.name == normalizedName) return@withLock true
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
        val current = loadUnlocked()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return@withLock
        persistUnlocked(updated)
    }

    override suspend fun replaceAll(sessions: List<StopwatchSession>) = mutex.withLock {
        require(sessions.size <= MAX_STORED_SESSIONS) { "Too many saved sessions." }
        require(sessions.map(StopwatchSession::id).distinct().size == sessions.size) {
            "Session ids must be unique."
        }
        sessions.forEach(SessionValidation::requireValid)
        val normalized = sessions.sortedByDescending(StopwatchSession::createdAtEpochMillis)
        if (normalized == loadUnlocked()) return@withLock
        persistUnlocked(normalized)
    }

    private suspend fun loadUnlocked(): List<StopwatchSession> {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (raw.length > MAX_SESSION_STORE_CHARACTERS) throw SessionStoreCorruptionException()

        val decoded = storeCodec.decode(raw) ?: throw SessionStoreCorruptionException()
        if (decoded.sessions.size > MAX_STORED_SESSIONS) throw SessionStoreCorruptionException()
        if (decoded.sessions.map(StopwatchSession::id).distinct().size != decoded.sessions.size) {
            throw SessionStoreCorruptionException()
        }
        if (decoded.sessions.any { SessionValidation.validate(it).isNotEmpty() }) {
            throw SessionStoreCorruptionException()
        }

        val normalized = decoded.sessions.sortedByDescending(StopwatchSession::createdAtEpochMillis)
        if (decoded.needsMigration) {
            persistUnlocked(normalized)
        }
        return normalized
    }

    private suspend fun persistUnlocked(sessions: List<StopwatchSession>) {
        val encoded = storeCodec.encode(sessions)
        require(encoded.length <= MAX_SESSION_STORE_CHARACTERS) { "Saved session history is too large." }
        storage.write(encoded)
    }
}
