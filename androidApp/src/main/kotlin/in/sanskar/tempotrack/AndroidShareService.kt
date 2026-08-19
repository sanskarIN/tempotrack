package in.sanskar.tempotrack

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import in.sanskar.tempotrack.data.ShareError
import in.sanskar.tempotrack.data.ShareResult
import in.sanskar.tempotrack.data.ShareService
import java.io.File
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
        runCatching {
            val safeName = suggestedFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val directory = File(context.cacheDir, "shared-exports").apply { mkdirs() }
            val target = File(directory, safeName)
            target.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                target,
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Share TempoTrack data").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
            ShareResult.Started
        }.getOrElse {
            ShareResult.Failure(ShareError.PREPARE_FAILED)
        }
    }
}
