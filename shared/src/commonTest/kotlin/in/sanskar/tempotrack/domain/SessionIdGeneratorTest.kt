package `in`.sanskar.tempotrack.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionIdGeneratorTest {
    @Test
    fun generatedIdContainsStableMetadataAndFixedRandomSuffix() {
        val id = SessionIdGenerator.generate(
            createdAtEpochMillis = 1_700_000_000_000L,
            durationNanos = 5_000_000_000L,
            randomLong = { 0x1234L },
        )

        assertEquals(
            "1700000000000-5000000000-0000000000001234",
            id,
        )
        assertTrue(id.length <= SessionValidation.MAX_SESSION_ID_LENGTH)
    }

    @Test
    fun differentRandomValuesProduceDifferentIdsForSameSaveMoment() {
        val first = SessionIdGenerator.generate(100L, 200L) { 1L }
        val second = SessionIdGenerator.generate(100L, 200L) { 2L }

        assertNotEquals(first, second)
    }

    @Test
    fun negativeRandomValueStillProducesFilenameSafeUnsignedHexSuffix() {
        val id = SessionIdGenerator.generate(100L, 200L) { -1L }

        assertTrue(id.endsWith("-ffffffffffffffff"))
        assertTrue(id.all { it.isLetterOrDigit() || it == '-' })
    }
}
