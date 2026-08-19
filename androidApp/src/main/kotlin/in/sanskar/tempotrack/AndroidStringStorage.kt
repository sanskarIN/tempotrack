package in.sanskar.tempotrack

import in.sanskar.tempotrack.data.StringStorage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidStringStorage(
    private val file: File,
) : StringStorage {
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (file.exists()) file.readText() else null
    }

    override suspend fun write(content: String) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        val target = file.toPath()
        val temp = File(file.parentFile, "${file.name}.tmp").toPath()
        Files.writeString(temp, content)
        runCatching {
            Files.move(
                temp,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.recoverCatching {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
        }.getOrThrow()
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        Files.deleteIfExists(file.toPath())
        Files.deleteIfExists(File(file.parentFile, "${file.name}.tmp").toPath())
        Unit
    }
}
