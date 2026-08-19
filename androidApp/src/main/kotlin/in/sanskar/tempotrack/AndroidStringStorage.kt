package in.sanskar.tempotrack

import in.sanskar.tempotrack.data.StringStorage
import java.io.File
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
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(content)
        if (!temp.renameTo(file)) {
            file.writeText(content)
            temp.delete()
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete()
        Unit
    }
}
