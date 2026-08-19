package `in`.sanskar.tempotrack.util

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class SuspendResultTest {
    @Test
    fun rethrowsCoroutineCancellation() = runTest {
        try {
            suspendResult<Unit> { throw CancellationException("cancel") }
            fail("CancellationException should be rethrown")
        } catch (_: CancellationException) {
            // Expected: cancellation must never be converted into Result.failure.
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
