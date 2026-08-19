package `in`.sanskar.tempotrack.data

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

interface ShareService {
    suspend fun share(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ShareResult
}

enum class ShareError {
    PREPARE_FAILED,
    PLATFORM_SHARE_UNAVAILABLE,
}

sealed interface ShareResult {
    data object Started : ShareResult
    data class Failure(val error: ShareError) : ShareResult
}
