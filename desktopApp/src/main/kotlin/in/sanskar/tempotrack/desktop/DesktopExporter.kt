package in.sanskar.tempotrack.desktop

import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.ExportResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopExporter : Exporter {
    override suspend fun export(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = suggestedFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val directory = Paths.get(System.getProperty("user.home"), "TempoTrack Exports")
            Files.createDirectories(directory)
            val target = directory.resolve(safeName)
            Files.writeString(target, content, StandardCharsets.UTF_8)
            ExportResult.Success(target.toAbsolutePath().toString())
        }.getOrElse {
            ExportResult.Failure("Could not write the export file.")
        }
    }
}
