package `in`.sanskar.tempotrack.util

import kotlinx.coroutines.CancellationException

suspend fun <T> suspendResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    Result.failure(error)
}
