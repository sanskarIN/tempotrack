package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.SessionValidation
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

enum class SessionImportError {
    EMPTY_BACKUP,
    BACKUP_TOO_LARGE,
    INVALID_JSON,
    INVALID_DATA,
    TOO_MANY_SESSIONS,
    DUPLICATE_SESSION_IDS,
    INVALID_SESSION,
}

sealed interface SessionImportResult {
    data class Success(val sessions: List<StopwatchSession>) : SessionImportResult

    data class Failure(
        val error: SessionImportError,
        val invalidSessionNumber: Int? = null,
    ) : SessionImportResult
}

object SessionImporter {
    const val MAX_IMPORT_CHARACTERS = MAX_SESSION_STORE_CHARACTERS
    const val MAX_IMPORT_SESSIONS = MAX_STORED_SESSIONS

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val serializer = ListSerializer(StopwatchSession.serializer())

    fun fromJson(content: String): SessionImportResult {
        if (content.isBlank()) {
            return SessionImportResult.Failure(SessionImportError.EMPTY_BACKUP)
        }
        if (content.length > MAX_IMPORT_CHARACTERS) {
            return SessionImportResult.Failure(SessionImportError.BACKUP_TOO_LARGE)
        }

        val sessions = try {
            json.decodeFromString(serializer, content)
        } catch (_: SerializationException) {
            return SessionImportResult.Failure(SessionImportError.INVALID_JSON)
        } catch (_: IllegalArgumentException) {
            return SessionImportResult.Failure(SessionImportError.INVALID_DATA)
        }

        if (sessions.size > MAX_IMPORT_SESSIONS) {
            return SessionImportResult.Failure(SessionImportError.TOO_MANY_SESSIONS)
        }
        if (sessions.map(StopwatchSession::id).distinct().size != sessions.size) {
            return SessionImportResult.Failure(SessionImportError.DUPLICATE_SESSION_IDS)
        }

        sessions.forEachIndexed { index, session ->
            if (SessionValidation.validate(session).isNotEmpty()) {
                return SessionImportResult.Failure(
                    error = SessionImportError.INVALID_SESSION,
                    invalidSessionNumber = index + 1,
                )
            }
        }

        return SessionImportResult.Success(
            sessions.sortedByDescending(StopwatchSession::createdAtEpochMillis),
        )
    }
}
