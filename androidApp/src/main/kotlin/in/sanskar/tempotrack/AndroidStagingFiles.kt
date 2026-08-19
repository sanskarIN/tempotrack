package in.sanskar.tempotrack

import java.io.File
import java.io.IOException

internal object AndroidStagingFiles {
    private const val MAX_EXPORT_NAME_ATTEMPTS = 10_000

    fun createUniqueShareFile(directory: File, safeName: String): File {
        val extension = safeName.substringAfterLast('.', missingDelimiterValue = "")
        val suffix = extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ".tmp"
        val stem = safeName
            .removeSuffix(suffix)
            .take(60)
            .ifBlank { "tempotrack-export" }
        val prefix = "$stem-".let { if (it.length >= 3) it else "tt-$it" }
        return File.createTempFile(prefix, suffix, directory)
    }

    fun reserveUniqueExportTarget(
        directory: File,
        safeName: String,
        maxAttempts: Int = MAX_EXPORT_NAME_ATTEMPTS,
    ): File {
        require(maxAttempts > 0) { "maxAttempts must be positive." }

        val extensionStart = safeName.lastIndexOf('.').takeIf { it in 1 until safeName.lastIndex }
        val stem = extensionStart?.let { safeName.substring(0, it) } ?: safeName
        val extension = extensionStart?.let { safeName.substring(it) }.orEmpty()

        repeat(maxAttempts) { index ->
            val candidateName = if (index == 0) safeName else "$stem ($index)$extension"
            val candidate = File(directory, candidateName)
            if (candidate.createNewFile()) return candidate
        }
        throw IOException("Could not reserve a unique TempoTrack export filename.")
    }
}
