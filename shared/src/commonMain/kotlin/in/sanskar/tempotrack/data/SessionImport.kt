package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.SessionValidation
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

sealed interface SessionImportResult {
    data class Success(val sessions: List<StopwatchSession>) : SessionImportResult
    data class Failure(val userMessage: String) : SessionImportResult
}

object SessionImporter {
    const val MAX_IMPORT_CHARACTERS = 5_000_000
    const val MAX_IMPORT_SESSIONS = 10_000

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        exceptionsWithDebugInfo = false
    }
    private val serializer = ListSerializer(StopwatchSession.serializer())

    fun fromJson(content: String): SessionImportResult {
        if (content.isBlank()) {
            return SessionImportResult.Failure("The selected backup is empty.")
        }
        if (content.length > MAX_IMPORT_CHARACTERS) {
            return SessionImportResult.Failure("The selected backup is too large to import safely.")
        }

        val sessions = try {
            json.decodeFromString(serializer, content)
        } catch (_: SerializationException) {
            return SessionImportResult.Failure("The selected file is not a valid TempoTrack JSON backup.")
        } catch (_: IllegalArgumentException) {
            return SessionImportResult.Failure("The selected file contains invalid data.")
        }

        if (sessions.size > MAX_IMPORT_SESSIONS) {
            return SessionImportResult.Failure("The backup contains too many sessions.")
        }
        if (sessions.map(StopwatchSession::id).distinct().size != sessions.size) {
            return SessionImportResult.Failure("The backup contains duplicate session ids.")
        }

        sessions.forEachIndexed { index, session ->
            val errors = SessionValidation.validate(session)
            if (errors.isNotEmpty()) {
                return SessionImportResult.Failure(
                    "Session ${index + 1} is invalid: ${errors.first()}",
                )
            }
        }

        return SessionImportResult.Success(
            sessions.sortedByDescending(StopwatchSession::createdAtEpochMillis),
        )
    }
}
