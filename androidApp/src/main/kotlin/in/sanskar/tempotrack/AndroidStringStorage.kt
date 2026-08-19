package `in`.sanskar.tempotrack

import `in`.sanskar.tempotrack.data.StringStorage
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidStringStorage(
    private val file: File,
) : StringStorage {
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    override suspend fun write(content: String) = withContext(Dispatchers.IO) {
        val parent = file.parentFile ?: throw IOException("Storage file must have a parent directory.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Could not create storage directory.")
        }
        if (!parent.isDirectory) {
            throw IOException("Storage parent path is not a directory.")
        }

        val target = file.toPath()
        val temp = File(parent, "${file.name}.tmp").toPath()
        Files.write(temp, content.toByteArray(Charsets.UTF_8))
        try {
            Files.move(
                temp,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
        }
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        Files.deleteIfExists(file.toPath())
        file.parentFile?.let { parent ->
            Files.deleteIfExists(File(parent, "${file.name}.tmp").toPath())
        }
        Unit
    }
}
