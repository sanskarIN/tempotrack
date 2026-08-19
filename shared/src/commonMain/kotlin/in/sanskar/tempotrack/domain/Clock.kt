package `in`.sanskar.tempotrack.domain

/**
 * Monotonic time is intentionally injected so stopwatch correctness does not
 * depend on wall-clock changes and can be tested deterministically.
 */
fun interface MonotonicClock {
    fun nowNanos(): Long
}

fun interface WallClock {
    fun nowEpochMillis(): Long
}
