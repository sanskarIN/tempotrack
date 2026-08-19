package in.sanskar.tempotrack.desktop

import in.sanskar.tempotrack.data.ExportError
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.ExportResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopExporter : Exporter {
    override suspend fun export(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ExportResult {
        val target = chooseTarget(suggestedFileName, mimeType)
            ?: return ExportResult.Failure(ExportError.USER_CANCELLED)

        return withContext(Dispatchers.IO) {
            runCatching {
                target.parent?.let(Files::createDirectories)
                Files.writeString(target, content, StandardCharsets.UTF_8)
                ExportResult.Success(target.toAbsolutePath().toString())
            }.getOrElse {
                ExportResult.Failure(ExportError.WRITE_FAILED)
            }
        }
    }

    private fun chooseTarget(
        suggestedFileName: String,
        mimeType: String,
    ): Path? {
        val safeName = suggestedFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val selected = AtomicReference<Path?>(null)

        val showChooser = {
            val chooser = JFileChooser().apply {
                dialogTitle = "Export TempoTrack data"
                selectedFile = java.io.File(safeName)
                val extension = safeName.substringAfterLast('.', missingDelimiterValue = "")
                if (extension.isNotBlank()) {
                    fileFilter = FileNameExtensionFilter(
                        when (mimeType) {
                            "application/json" -> "JSON files"
                            "text/csv" -> "CSV files"
                            else -> "TempoTrack export"
                        },
                        extension,
                    )
                }
            }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                selected.set(chooser.selectedFile.toPath())
            }
        }

        if (SwingUtilities.isEventDispatchThread()) {
            showChooser()
        } else {
            SwingUtilities.invokeAndWait(showChooser)
        }
        return selected.get()
    }
}
