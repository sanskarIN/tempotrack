package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.StopwatchSession
import kotlin.test.Test
import kotlin.test.assertContains

class SessionCodecTest {
    @Test
    fun csvEscapesQuotesAndCommas() {
        val session = session(name = "Intervals, \"hard\"")

        val csv = SessionCodec.toCsv(listOf(session))

        assertContains(csv, "\"Intervals, \"\"hard\"\"\"")
    }

    @Test
    fun jsonContainsSessionIdentityAndUnicode() {
        val session = session(
            id = "id-2",
            name = "Study sprint — अध्ययन",
        )

        val json = SessionCodec.toJson(listOf(session))

        assertContains(json, "Study sprint — अध्ययन")
        assertContains(json, "id-2")
    }

    @Test
    fun csvNeutralizesCommonSpreadsheetFormulaPrefixes() {
        val riskyNames = listOf(
            "=SUM(1,1)",
            "+1+1",
            "-2+3",
            "@SUM(A1:A2)",
            "   =HYPERLINK(\"https://example.invalid\")",
        )

        riskyNames.forEachIndexed { index, name ->
            val csv = SessionCodec.toCsv(listOf(session(id = "formula-$index", name = name)))
            val escaped = ("'$name").replace("\"", "\"\"")
            assertContains(csv, "\"$escaped\"")
        }
    }

    @Test
    fun csvKeepsBenignLeadingTextUnchanged() {
        val csv = SessionCodec.toCsv(listOf(session(name = "  normal session")))

        assertContains(csv, "\"  normal session\"")
    }

    private fun session(
        id: String = "id-1",
        name: String,
    ) = StopwatchSession(
        id = id,
        name = name,
        createdAtEpochMillis = 1L,
        durationNanos = 1_000_000_000L,
        laps = emptyList(),
    )
}
