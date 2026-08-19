package in.sanskar.tempotrack

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AndroidShareStagingTest {
    @Test
    fun repeatedSharesUseDifferentFilesWithSameExtension() = withTempDirectory { directory ->
        val first = AndroidStagingFiles.createUniqueShareFile(directory, "sessions.json")
        val second = AndroidStagingFiles.createUniqueShareFile(directory, "sessions.json")

        assertNotEquals(first.name, second.name)
        assertTrue(first.name.endsWith(".json"))
        assertTrue(second.name.endsWith(".json"))
        assertTrue(first.isFile)
        assertTrue(second.isFile)
    }

    @Test
    fun shortExtensionlessNameStillProducesValidTempFile() = withTempDirectory { directory ->
        val target = AndroidStagingFiles.createUniqueShareFile(directory, "a")

        assertTrue(target.name.startsWith("tt-a-"))
        assertTrue(target.name.endsWith(".tmp"))
        assertTrue(target.isFile)
    }

    private fun withTempDirectory(block: (java.io.File) -> Unit) {
        val directory = Files.createTempDirectory("tempotrack-share-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
