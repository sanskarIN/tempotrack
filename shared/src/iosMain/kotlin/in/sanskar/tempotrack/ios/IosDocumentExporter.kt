package in.sanskar.tempotrack.ios

import in.sanskar.tempotrack.data.ExportError
import in.sanskar.tempotrack.data.ExportResult
import in.sanskar.tempotrack.data.Exporter
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class IosDocumentExporter(
    private val presenter: () -> UIViewController,
) : Exporter {
    private val mutex = Mutex()
    private var activeDelegate: UIDocumentPickerDelegateProtocol? = null

    override suspend fun export(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ExportResult {
        mutex.lock()
        try {
            val temporaryFile = try {
                withContext(Dispatchers.Default) {
                    writeIosTemporaryExportFile(suggestedFileName, content)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            } ?: return ExportResult.Failure(ExportError.WRITE_FAILED)

            return try {
                withContext(Dispatchers.Main) {
                    awaitDocumentPicker(temporaryFile)
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Default) {
                    removeIosTemporaryExportFile(temporaryFile)
                }
            }
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun awaitDocumentPicker(file: IosTemporaryExportFile): ExportResult =
        suspendCancellableCoroutine { continuation ->
            val picker = UIDocumentPickerViewController(
                forExportingURLs = listOf(file.url),
                asCopy = true,
            )
            picker.shouldShowFileExtensions = true

            fun finish(result: ExportResult) {
                activeDelegate = null
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL,
                ) {
                    finish(ExportResult.Success(destinationLabel(didPickDocumentAtURL)))
                }

                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>,
                ) {
                    val destination = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    if (destination == null) {
                        finish(ExportResult.Failure(ExportError.WRITE_FAILED))
                    } else {
                        finish(ExportResult.Success(destinationLabel(destination)))
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    finish(ExportResult.Failure(ExportError.USER_CANCELLED))
                }
            }

            activeDelegate = delegate
            picker.delegate = delegate

            continuation.invokeOnCancellation {
                CoroutineScope(Dispatchers.Main).launch {
                    picker.dismissViewControllerAnimated(true, completion = null)
                    if (activeDelegate === delegate) {
                        activeDelegate = null
                    }
                }
            }

            try {
                presenter().presentViewController(
                    picker,
                    animated = true,
                    completion = null,
                )
            } catch (_: Exception) {
                activeDelegate = null
                if (continuation.isActive) {
                    continuation.resume(ExportResult.Failure(ExportError.PLATFORM_EXPORT_UNAVAILABLE))
                }
            }
        }

    private fun destinationLabel(url: NSURL): String =
        url.path?.takeIf(String::isNotBlank)
            ?: url.absoluteString?.takeIf(String::isNotBlank)
            ?: "iOS document"
}
