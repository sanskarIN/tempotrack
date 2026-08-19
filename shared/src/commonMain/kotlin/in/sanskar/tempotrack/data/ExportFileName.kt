package `in`.sanskar.tempotrack.data

object ExportFileName {
    private const val MAX_LENGTH = 120
    private val unsafeCharacters = Regex("[^A-Za-z0-9._-]")

    fun sanitize(suggestedFileName: String): String {
        val normalized = suggestedFileName
            .trim()
            .replace(unsafeCharacters, "_")
            .trim('.', '_')
            .take(MAX_LENGTH)

        return normalized.ifBlank { "tempotrack-export" }
    }
}
