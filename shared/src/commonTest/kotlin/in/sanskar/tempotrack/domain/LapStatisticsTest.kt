package in.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LapStatisticsTest {
    @Test
    fun roundsHalfNanosecondUpForNonNegativeSplits() {
        val statistics = LapStatistics.from(
            listOf(
                Lap(index = 1, splitNanos = 1L, totalNanos = 1L),
                Lap(index = 2, splitNanos = 2L, totalNanos = 3L),
            ),
        )

        assertEquals(2L, statistics.averageSplitNanos)
    }

    @Test
    fun averagesMaximumDurationsWithoutOverflow() {
        val statistics = LapStatistics.from(
            listOf(
                Lap(index = 1, splitNanos = Long.MAX_VALUE, totalNanos = Long.MAX_VALUE),
                Lap(index = 2, splitNanos = Long.MAX_VALUE, totalNanos = Long.MAX_VALUE),
            ),
        )

        assertEquals(Long.MAX_VALUE, statistics.averageSplitNanos)
    }

    @Test
    fun mixedNearMaximumValuesRoundWithoutOverflow() {
        val statistics = LapStatistics.from(
            listOf(
                Lap(index = 1, splitNanos = Long.MAX_VALUE, totalNanos = Long.MAX_VALUE),
                Lap(index = 2, splitNanos = Long.MAX_VALUE - 1L, totalNanos = Long.MAX_VALUE),
            ),
        )

        assertEquals(Long.MAX_VALUE, statistics.averageSplitNanos)
    }
}
