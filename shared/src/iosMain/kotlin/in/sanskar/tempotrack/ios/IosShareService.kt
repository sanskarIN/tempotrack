package in.sanskar.tempotrack.ios

import in.sanskar.tempotrack.data.ExportFileName
import in.sanskar.tempotrack.data.ShareError
import in.sanskar.tempotrack.data.ShareResult
import in.sanskar.tempotrack.data.ShareService
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
internal class IosShareService(
    private val presenter: () -> UIViewController,
) : ShareService {
    override suspend fun share(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ShareResult = withContext(Dispatchers.Main) {
        try {
            val safeName = ExportFileName.sanitize(suggestedFileName)
            val temporaryDirectory = NSTemporaryDirectory().trimEnd('/')
            val path = "$temporaryDirectory/$safeName"
            val url = NSURL.fileURLWithPath(path)
            val written = (content as NSString).writeToURL(
                url = url,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
            if (!written) {
                return@withContext ShareResult.Failure(ShareError.PREPARE_FAILED)
            }

            val host = presenter()
            val activity = UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            )
            activity.popoverPresentationController?.let { popover ->
                val sourceView = host.view
                    ?: return@withContext ShareResult.Failure(ShareError.PLATFORM_SHARE_UNAVAILABLE)
                popover.sourceView = sourceView
                popover.sourceRect = sourceView.bounds
            }
            host.presentViewController(activity, animated = true, completion = null)
            ShareResult.Started
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ShareResult.Failure(ShareError.PLATFORM_SHARE_UNAVAILABLE)
        }
    }
}
