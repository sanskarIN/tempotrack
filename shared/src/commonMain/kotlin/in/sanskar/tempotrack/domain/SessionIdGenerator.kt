package in.sanskar.tempotrack.domain

import kotlin.random.Random

object SessionIdGenerator {
    fun generate(
        createdAtEpochMillis: Long,
        durationNanos: Long,
        randomLong: () -> Long = Random.Default::nextLong,
    ): String {
        val randomSuffix = randomLong().toULong().toString(radix = 16).padStart(16, '0')
        return "$createdAtEpochMillis-$durationNanos-$randomSuffix"
            .take(SessionValidation.MAX_SESSION_ID_LENGTH)
    }
}
