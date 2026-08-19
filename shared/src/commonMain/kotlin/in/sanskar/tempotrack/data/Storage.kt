package in.sanskar.tempotrack.data

interface StringStorage {
    suspend fun read(): String?
    suspend fun write(content: String)
    suspend fun clear()
}

interface Exporter {
    suspend fun export(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ExportResult
}

enum class ExportError {
    WRITE_FAILED,
    PLATFORM_EXPORT_UNAVAILABLE,
    USER_CANCELLED,
}

sealed interface ExportResult {
    data class Success(val destination: String) : ExportResult
    data class Failure(val error: ExportError) : ExportResult
}
