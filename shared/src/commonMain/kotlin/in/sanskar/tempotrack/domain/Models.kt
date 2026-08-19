package in.sanskar.tempotrack.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

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
                averageSplitNanos = laps.map(Lap::splitNanos).average().roundToLong(),
            )
        }
    }
}

const val NANOS_PER_MILLISECOND: Long = 1_000_000L
const val NANOS_PER_SECOND: Long = 1_000_000_000L
