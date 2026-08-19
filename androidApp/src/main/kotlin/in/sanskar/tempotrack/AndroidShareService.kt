package in.sanskar.tempotrack

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import in.sanskar.tempotrack.data.ExportFileName
import in.sanskar.tempotrack.data.ShareError
import in.sanskar.tempotrack.data.ShareResult
import in.sanskar.tempotrack.data.ShareService
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidShareService(
    private val context: Context,
) : ShareService {
    override suspend fun share(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ShareResult = withContext(Dispatchers.IO) {
        val shareIntent = try {
            val safeName = ExportFileName.sanitize(suggestedFileName)
            val directory = File(context.cacheDir, "shared-exports")
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Could not create share cache directory.")
            }
            if (!directory.isDirectory) {
                throw IOException("Share cache path is not a directory.")
            }
            val target = File(directory, safeName)
            target.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                target,
            )

            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    clipData = ClipData.newRawUri("TempoTrack export", uri)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Share TempoTrack data",
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@withContext ShareResult.Failure(ShareError.PREPARE_FAILED)
        }

        try {
            withContext(Dispatchers.Main) {
                context.startActivity(shareIntent)
            }
            ShareResult.Started
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ShareResult.Failure(ShareError.PLATFORM_SHARE_UNAVAILABLE)
        }
    }
}
