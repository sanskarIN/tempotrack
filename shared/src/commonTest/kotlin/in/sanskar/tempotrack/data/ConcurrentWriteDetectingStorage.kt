package `in`.sanskar.tempotrack.data

import kotlinx.coroutines.delay

internal class ConcurrentWriteDetectingStorage(
    var value: String? = null,
) : StringStorage {
    private var writing = false

    override suspend fun read(): String? = value

    override suspend fun write(content: String) {
        check(!writing) { "Concurrent storage write detected" }
        writing = true
        try {
            delay(1)
            value = content
        } finally {
            writing = false
        }
    }

    override suspend fun clear() {
        check(!writing) { "Concurrent storage mutation detected" }
        writing = true
        try {
            delay(1)
            value = null
        } finally {
            writing = false
        }
    }
}
