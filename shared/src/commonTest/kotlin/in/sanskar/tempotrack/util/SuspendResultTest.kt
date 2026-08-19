package in.sanskar.tempotrack.util

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class SuspendResultTest {
    @Test
    fun rethrowsCoroutineCancellation() = runTest {
        assertFailsWith<CancellationException> {
            suspendResult<Unit> { throw CancellationException("cancel") }
        }
    }

    @Test
    fun capturesOrdinaryFailures() = runTest {
        val result = suspendResult<Unit> { error("boom") }

        assertTrue(result.isFailure)
    }

    @Test
    fun returnsSuccessfulValue() = runTest {
        val result = suspendResult { 42 }

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == 42)
    }
}
