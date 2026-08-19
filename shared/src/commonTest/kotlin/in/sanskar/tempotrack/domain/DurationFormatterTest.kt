package `in`.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatterTest {
    @Test
    fun formatsHoursMinutesSecondsAndMillis() {
        val nanos = (
            1 * 60 * 60 * NANOS_PER_SECOND +
                2 * 60 * NANOS_PER_SECOND +
                3 * NANOS_PER_SECOND +
                456 * NANOS_PER_MILLISECOND
            )

        assertEquals("01:02:03.456", DurationFormatter.formatNanos(nanos))
    }

    @Test
    fun clampsNegativeInput() {
        assertEquals("00:00:00.000", DurationFormatter.formatNanos(-1L))
    }
}
