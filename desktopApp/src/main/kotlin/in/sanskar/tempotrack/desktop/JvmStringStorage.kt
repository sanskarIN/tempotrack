package in.sanskar.tempotrack.desktop

import in.sanskar.tempotrack.data.StringStorage
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmStringStorage(
    private val path: Path,
) : StringStorage {
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (Files.exists(path)) Files.readString(path, StandardCharsets.UTF_8) else null
    }

    override suspend fun write(content: String) = withContext(Dispatchers.IO) {
        val parent = path.parent ?: throw IOException("Storage path must have a parent directory.")
        Files.createDirectories(parent)
        if (!Files.isDirectory(parent)) {
            throw IOException("Storage parent path is not a directory.")
        }

        val temp = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(temp, content, StandardCharsets.UTF_8)
        try {
            Files.move(
                temp,
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        Files.deleteIfExists(path)
        Files.deleteIfExists(path.resolveSibling("${path.fileName}.tmp"))
        Unit
    }
}
