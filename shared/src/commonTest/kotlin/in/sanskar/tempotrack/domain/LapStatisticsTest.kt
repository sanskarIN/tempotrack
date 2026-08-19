package in.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LapStatisticsTest {
    @Test
    fun emptyLapsProduceEmptyStatistics() {
        val statistics = LapStatistics.from(emptyList())

        assertNull(statistics.fastest)
        assertNull(statistics.slowest)
        assertEquals(0L, statistics.averageSplitNanos)
    }

    @Test
    fun statisticsIdentifyFastestSlowestAndAverage() {
        val laps = listOf(
            Lap(index = 1, splitNanos = 3_000L, totalNanos = 3_000L),
            Lap(index = 2, splitNanos = 1_000L, totalNanos = 4_000L),
            Lap(index = 3, splitNanos = 2_000L, totalNanos = 6_000L),
        )

        val statistics = LapStatistics.from(laps)

        assertEquals(2, statistics.fastest?.index)
        assertEquals(1, statistics.slowest?.index)
        assertEquals(2_000L, statistics.averageSplitNanos)
    }

    @Test
    fun singleLapIsBothFastestAndSlowest() {
        val lap = Lap(index = 1, splitNanos = 9_000L, totalNanos = 9_000L)

        val statistics = LapStatistics.from(listOf(lap))

        assertEquals(lap, statistics.fastest)
        assertEquals(lap, statistics.slowest)
        assertEquals(9_000L, statistics.averageSplitNanos)
    }
}
