package in.sanskar.tempotrack

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import in.sanskar.tempotrack.data.ExportError
import in.sanskar.tempotrack.data.ExportFileName
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.ExportResult
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidExporter(
    private val context: Context,
) : Exporter {
    override suspend fun export(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val safeName = ExportFileName.sanitize(suggestedFileName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportWithMediaStore(safeName, mimeType, content)
            } else {
                exportToAppDocuments(safeName, content)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ExportResult.Failure(ExportError.WRITE_FAILED)
        }
    }

    private fun exportWithMediaStore(
        safeName: String,
        mimeType: String,
        content: String,
    ): ExportResult {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TempoTrack")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = requireNotNull(
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
        )

        try {
            context.contentResolver.openOutputStream(uri, "w").use { output ->
                requireNotNull(output).writer(Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            val updatedRows = context.contentResolver.update(uri, values, null, null)
            if (updatedRows != 1) {
                throw IOException("Could not finalize MediaStore export.")
            }
            return ExportResult.Success(uri.toString())
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }

    private fun exportToAppDocuments(
        safeName: String,
        content: String,
    ): ExportResult {
        val baseDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IOException("External app documents directory is unavailable.")
        val directory = File(baseDirectory, "TempoTrack")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create TempoTrack export directory.")
        }
        if (!directory.isDirectory) {
            throw IOException("TempoTrack export path is not a directory.")
        }

        val target = AndroidStagingFiles.reserveUniqueExportTarget(directory, safeName)
        try {
            target.writeText(content, Charsets.UTF_8)
        } catch (error: Exception) {
            target.delete()
            throw error
        }
        return ExportResult.Success(target.absolutePath)
    }
}
