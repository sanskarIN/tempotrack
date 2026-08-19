package in.sanskar.tempotrack

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import in.sanskar.tempotrack.data.ExportError
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.ExportResult
import java.io.File
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
        runCatching {
            val safeName = suggestedFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                        requireNotNull(output).writer(Charsets.UTF_8).use { it.write(content) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    ExportResult.Success(uri.toString())
                } catch (error: Throwable) {
                    context.contentResolver.delete(uri, null, null)
                    throw error
                }
            } else {
                val directory = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "TempoTrack",
                ).apply { mkdirs() }
                val target = File(directory, safeName)
                target.writeText(content)
                ExportResult.Success(target.absolutePath)
            }
        }.getOrElse {
            ExportResult.Failure(ExportError.WRITE_FAILED)
        }
    }
}
