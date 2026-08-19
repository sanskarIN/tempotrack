package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.DurationFormatter
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object SessionCodec {
    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    fun toJson(sessions: List<StopwatchSession>): String =
        json.encodeToString(ListSerializer(StopwatchSession.serializer()), sessions)

    fun toCsv(sessions: List<StopwatchSession>): String = buildString {
        appendLine("session_id,session_name,created_at_epoch_ms,duration,lap_number,split,total")
        sessions.forEach { session ->
            if (session.laps.isEmpty()) {
                append(csv(session.id)).append(',')
                append(csv(session.name)).append(',')
                append(session.createdAtEpochMillis).append(',')
                append(csv(DurationFormatter.formatNanos(session.durationNanos))).append(',')
                append(",,")
                appendLine()
            } else {
                session.laps.forEach { lap ->
                    append(csv(session.id)).append(',')
                    append(csv(session.name)).append(',')
                    append(session.createdAtEpochMillis).append(',')
                    append(csv(DurationFormatter.formatNanos(session.durationNanos))).append(',')
                    append(lap.index).append(',')
                    append(csv(DurationFormatter.formatNanos(lap.splitNanos))).append(',')
                    append(csv(DurationFormatter.formatNanos(lap.totalNanos)))
                    appendLine()
                }
            }
        }
    }

    private fun csv(value: String): String {
        val trimmed = value.trimStart()
        val safe = if (trimmed.firstOrNull() in setOf('=', '+', '-', '@')) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
