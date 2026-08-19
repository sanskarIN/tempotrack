package `in`.sanskar.tempotrack.ios

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
class IosTemporaryExportFileTest {
    @Test
    fun createsSanitizedFileInIsolatedDirectoryAndRemovesIt() {
        val temporaryFile = requireNotNull(
            writeIosTemporaryExportFile(
                suggestedFileName = "../unsafe report.csv",
                content = "session_id,session_name\n1,Study",
            ),
        )
        val path = requireNotNull(temporaryFile.url.path)
        val fileManager = NSFileManager.defaultManager

        assertTrue(path.endsWith("/unsafe_report.csv"))
        assertTrue(fileManager.fileExistsAtPath(path))
        assertTrue(fileManager.fileExistsAtPath(temporaryFile.directoryPath))

        removeIosTemporaryExportFile(temporaryFile)

        assertFalse(fileManager.fileExistsAtPath(path))
        assertFalse(fileManager.fileExistsAtPath(temporaryFile.directoryPath))
    }

    @Test
    fun separateOperationsUseDifferentDirectories() {
        val first = requireNotNull(writeIosTemporaryExportFile("backup.json", "[]"))
        val second = requireNotNull(writeIosTemporaryExportFile("backup.json", "[]"))

        try {
            assertTrue(first.directoryPath != second.directoryPath)
            assertTrue(first.url.path != second.url.path)
        } finally {
            removeIosTemporaryExportFile(first)
            removeIosTemporaryExportFile(second)
        }
    }
}
