package in.sanskar.tempotrack.ios

import in.sanskar.tempotrack.data.ExportFileName
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding

@OptIn(ExperimentalForeignApi::class)
internal fun writeIosTemporaryExportFile(
    suggestedFileName: String,
    content: String,
): NSURL? {
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
    return url.takeIf { written }
}

@OptIn(ExperimentalForeignApi::class)
internal fun removeIosTemporaryExportFile(url: NSURL) {
    url.path?.let { path ->
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}
