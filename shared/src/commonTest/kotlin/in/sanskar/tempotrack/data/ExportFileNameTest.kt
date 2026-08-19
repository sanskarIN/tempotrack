package `in`.sanskar.tempotrack.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportFileNameTest {
    @Test
    fun replacesUnsafeCharacters() {
        assertEquals(
            "tempo_track_sessions_.json",
            ExportFileName.sanitize("tempo track/sessions?.json"),
        )
    }

    @Test
    fun removesLeadingTraversalLikePunctuation() {
        assertEquals("backup.json", ExportFileName.sanitize("../backup.json"))
    }

    @Test
    fun fallsBackWhenNothingSafeRemains() {
        assertEquals("tempotrack-export", ExportFileName.sanitize("...___"))
    }

    @Test
    fun limitsGeneratedFilenameLength() {
        val result = ExportFileName.sanitize("a".repeat(200) + ".json")
        assertTrue(result.length <= 120)
    }
}
