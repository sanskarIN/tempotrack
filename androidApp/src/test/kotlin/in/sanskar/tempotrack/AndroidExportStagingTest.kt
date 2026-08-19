package in.sanskar.tempotrack

import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidExportStagingTest {
    @Test
    fun availableSuggestedNameIsReservedDirectly() = withTempDirectory { directory ->
        val target = AndroidStagingFiles.reserveUniqueExportTarget(directory, "sessions.json")

        assertEquals("sessions.json", target.name)
        assertTrue(target.isFile)
    }

    @Test
    fun existingExportGetsCounterBeforeExtension() = withTempDirectory { directory ->
        Files.writeString(directory.toPath().resolve("sessions.json"), "old")

        val target = AndroidStagingFiles.reserveUniqueExportTarget(directory, "sessions.json")

        assertEquals("sessions (1).json", target.name)
        assertEquals("old", directory.resolve("sessions.json").readText())
    }

    @Test
    fun namesWithoutExtensionsReceiveCollisionCounter() = withTempDirectory { directory ->
        Files.writeString(directory.toPath().resolve("backup"), "old")

        val target = AndroidStagingFiles.reserveUniqueExportTarget(directory, "backup")

        assertEquals("backup (1)", target.name)
    }

    @Test
    fun exhaustedReservationAttemptsFailWithoutOverwriting() = withTempDirectory { directory ->
        Files.writeString(directory.toPath().resolve("backup.json"), "first")
        Files.writeString(directory.toPath().resolve("backup (1).json"), "second")

        assertThrows(IOException::class.java) {
            AndroidStagingFiles.reserveUniqueExportTarget(
                directory = directory,
                safeName = "backup.json",
                maxAttempts = 2,
            )
        }
        assertEquals("first", directory.resolve("backup.json").readText())
        assertEquals("second", directory.resolve("backup (1).json").readText())
    }

    private fun withTempDirectory(block: (java.io.File) -> Unit) {
        val directory = Files.createTempDirectory("tempotrack-export-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
