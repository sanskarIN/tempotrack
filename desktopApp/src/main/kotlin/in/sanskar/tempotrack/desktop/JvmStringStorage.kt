package in.sanskar.tempotrack.desktop

import in.sanskar.tempotrack.data.StringStorage
import java.nio.charset.StandardCharsets
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
        path.parent?.let(Files::createDirectories)
        val temp = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(temp, content, StandardCharsets.UTF_8)
        runCatching {
            Files.move(
                temp,
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.recoverCatching {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }.getOrThrow()
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        Files.deleteIfExists(path)
        Unit
    }
}
