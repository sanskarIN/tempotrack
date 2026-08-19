package in.sanskar.tempotrack.desktop

import in.sanskar.tempotrack.data.ExportError
import in.sanskar.tempotrack.data.ExportFileName
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.ExportResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopExporter : Exporter {
    override suspend fun export(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ExportResult {
        val target = try {
            chooseTarget(suggestedFileName, mimeType)
        } catch (error: CancellationException) {
            throw error
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            return ExportResult.Failure(ExportError.PLATFORM_EXPORT_UNAVAILABLE)
        } catch (_: Exception) {
            return ExportResult.Failure(ExportError.PLATFORM_EXPORT_UNAVAILABLE)
        }

        if (target == null) {
            return ExportResult.Failure(ExportError.USER_CANCELLED)
        }

        return withContext(Dispatchers.IO) {
            try {
                target.parent?.let(Files::createDirectories)
                Files.writeString(target, content, StandardCharsets.UTF_8)
                ExportResult.Success(target.toAbsolutePath().toString())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ExportResult.Failure(ExportError.WRITE_FAILED)
            }
        }
    }

    private fun chooseTarget(
        suggestedFileName: String,
        mimeType: String,
    ): Path? {
        val safeName = ExportFileName.sanitize(suggestedFileName)
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
