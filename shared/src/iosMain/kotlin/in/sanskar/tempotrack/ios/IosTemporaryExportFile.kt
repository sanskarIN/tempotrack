package `in`.sanskar.tempotrack.ios

import `in`.sanskar.tempotrack.data.ExportFileName
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID

internal data class IosTemporaryExportFile(
    val url: NSURL,
    val directoryPath: String,
)

@OptIn(ExperimentalForeignApi::class)
internal fun writeIosTemporaryExportFile(
    suggestedFileName: String,
    content: String,
): IosTemporaryExportFile? {
    val safeName = ExportFileName.sanitize(suggestedFileName)
    val temporaryRoot = NSTemporaryDirectory().trimEnd('/')
    val directoryPath = "$temporaryRoot/tempotrack-${NSUUID().UUIDString}"
    val fileManager = NSFileManager.defaultManager
    val directoryCreated = fileManager.createDirectoryAtPath(
        path = directoryPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    if (!directoryCreated) return null

    val url = NSURL.fileURLWithPath("$directoryPath/$safeName")
    val written = (content as NSString).writeToURL(
        url = url,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null,
    )
    if (!written) {
        fileManager.removeItemAtPath(directoryPath, error = null)
        return null
    }

    return IosTemporaryExportFile(url = url, directoryPath = directoryPath)
}

@OptIn(ExperimentalForeignApi::class)
internal fun removeIosTemporaryExportFile(file: IosTemporaryExportFile) {
    NSFileManager.defaultManager.removeItemAtPath(file.directoryPath, error = null)
}
