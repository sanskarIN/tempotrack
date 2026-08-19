package `in`.sanskar.tempotrack.domain

object DurationFormatter {
    fun formatNanos(nanos: Long, showMillis: Boolean = true): String {
        val safeNanos = nanos.coerceAtLeast(0L)
        val totalMillis = safeNanos / NANOS_PER_MILLISECOND
        val millis = totalMillis % 1_000
        val totalSeconds = totalMillis / 1_000
        val seconds = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val minutes = totalMinutes % 60
        val hours = totalMinutes / 60

        return if (showMillis) {
            "${hours.two()}:${minutes.two()}:${seconds.two()}.${millis.three()}"
        } else {
            "${hours.two()}:${minutes.two()}:${seconds.two()}"
        }
    }

    private fun Long.two(): String = toString().padStart(2, '0')
    private fun Long.three(): String = toString().padStart(3, '0')
}
