package `in`.sanskar.tempotrack.domain

import kotlinx.serialization.Serializable

@Serializable
enum class StopwatchStatus {
    IDLE,
    RUNNING,
    PAUSED,
}

@Serializable
data class Lap(
    val index: Int,
    val splitNanos: Long,
    val totalNanos: Long,
)

@Serializable
data class StopwatchSnapshot(
    val status: StopwatchStatus = StopwatchStatus.IDLE,
    val elapsedNanos: Long = 0L,
    val laps: List<Lap> = emptyList(),
) {
    val elapsedMillis: Long get() = elapsedNanos / NANOS_PER_MILLISECOND
}

@Serializable
data class StopwatchCheckpoint(
    val status: StopwatchStatus = StopwatchStatus.IDLE,
    val accumulatedNanos: Long = 0L,
    val startedAtNanos: Long? = null,
    val savedAtEpochMillis: Long? = null,
    val laps: List<Lap> = emptyList(),
)

@Serializable
data class StopwatchSession(
    val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val durationNanos: Long,
    val laps: List<Lap>,
)

data class LapStatistics(
    val fastest: Lap?,
    val slowest: Lap?,
    val averageSplitNanos: Long,
) {
    companion object {
        fun from(laps: List<Lap>): LapStatistics {
            if (laps.isEmpty()) return LapStatistics(null, null, 0L)
            return LapStatistics(
                fastest = laps.minByOrNull(Lap::splitNanos),
                slowest = laps.maxByOrNull(Lap::splitNanos),
                averageSplitNanos = roundedAverageNonNegative(laps.map(Lap::splitNanos)),
            )
        }

        private fun roundedAverageNonNegative(values: List<Long>): Long {
            val count = values.size.toLong()
            var quotientSum = 0L
            var remainderSum = 0L

            values.forEach { rawValue ->
                val value = rawValue.coerceAtLeast(0L)
                quotientSum += value / count
                remainderSum += value % count
            }

            val wholeRemainder = remainderSum / count
            val fractionalRemainder = remainderSum % count
            val averageFloor = quotientSum + wholeRemainder
            val roundUp = fractionalRemainder >= (count + 1L) / 2L
            return if (roundUp && averageFloor < Long.MAX_VALUE) averageFloor + 1L else averageFloor
        }
    }
}

const val NANOS_PER_MILLISECOND: Long = 1_000_000L
const val NANOS_PER_SECOND: Long = 1_000_000_000L
