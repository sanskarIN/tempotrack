package `in`.sanskar.tempotrack.ios

import `in`.sanskar.tempotrack.data.ShareError
import `in`.sanskar.tempotrack.data.ShareResult
import `in`.sanskar.tempotrack.data.ShareService
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
internal class IosShareService(
    private val presenter: () -> UIViewController,
) : ShareService {
    private var activeActivity: UIActivityViewController? = null

    override suspend fun share(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ShareResult {
        val temporaryFile = try {
            withContext(Dispatchers.Default) {
                writeIosTemporaryExportFile(suggestedFileName, content)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        } ?: return ShareResult.Failure(ShareError.PREPARE_FAILED)

        return withContext(Dispatchers.Main) {
            if (activeActivity != null) {
                removeIosTemporaryExportFile(temporaryFile)
                return@withContext ShareResult.Failure(ShareError.PREPARE_FAILED)
            }

            try {
                val host = presenter()
                val activity = UIActivityViewController(
                    activityItems = listOf(temporaryFile.url),
                    applicationActivities = null,
                )
                activeActivity = activity
                activity.completionWithItemsHandler = { _, _, _, _ ->
                    removeIosTemporaryExportFile(temporaryFile)
                    activeActivity = null
                }
                activity.popoverPresentationController?.let { popover ->
                    val sourceView = host.view
                        ?: return@withContext sharePresentationFailure(temporaryFile)
                    popover.sourceView = sourceView
                    popover.sourceRect = sourceView.bounds
                }
                host.presentViewController(activity, animated = true, completion = null)
                ShareResult.Started
            } catch (error: CancellationException) {
                removeIosTemporaryExportFile(temporaryFile)
                activeActivity = null
                throw error
            } catch (_: Exception) {
                sharePresentationFailure(temporaryFile)
            }
        }
    }

    private fun sharePresentationFailure(file: IosTemporaryExportFile): ShareResult {
        removeIosTemporaryExportFile(file)
        activeActivity = null
        return ShareResult.Failure(ShareError.PLATFORM_SHARE_UNAVAILABLE)
    }
}
