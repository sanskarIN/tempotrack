package `in`.sanskar.tempotrack.data

import `in`.sanskar.tempotrack.domain.StopwatchSession
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SessionCodecTest {
    @Test
    fun csvEscapesQuotesAndCommas() {
        val session = StopwatchSession(
            id = "id-1",
            name = "Intervals, \"hard\"",
            createdAtEpochMillis = 1L,
            durationNanos = 1_000_000_000L,
            laps = emptyList(),
        )

        val csv = SessionCodec.toCsv(listOf(session))

        assertContains(csv, "\"Intervals, \"\"hard\"\"\"")
    }

    @Test
    fun emptyLapCsvRowMatchesSevenColumnHeader() {
        val session = StopwatchSession(
            id = "id-empty",
            name = "No laps",
            createdAtEpochMillis = 10L,
            durationNanos = 1_000_000_000L,
            laps = emptyList(),
        )

        val lines = SessionCodec.toCsv(listOf(session)).lineSequence().filter(String::isNotBlank).toList()

        assertEquals(2, lines.size)
        assertEquals(6, lines[0].count { it == ',' })
        assertEquals(6, lines[1].count { it == ',' })
    }

    @Test
    fun jsonContainsSessionIdentity() {
        val session = StopwatchSession(
            id = "id-2",
            name = "Study sprint",
            createdAtEpochMillis = 2L,
            durationNanos = 2_000_000_000L,
            laps = emptyList(),
        )

        val json = SessionCodec.toJson(listOf(session))

        assertContains(json, "Study sprint")
        assertContains(json, "id-2")
    }

    @Test
    fun csvNeutralizesSpreadsheetFormulas() {
        val session = StopwatchSession(
            id = "id-3",
            name = "=SUM(1,1)",
            createdAtEpochMillis = 3L,
            durationNanos = 3_000_000_000L,
            laps = emptyList(),
        )

        val csv = SessionCodec.toCsv(listOf(session))

        assertContains(csv, "\"'=SUM(1,1)\"")
    }
}
