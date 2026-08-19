package in.sanskar.tempotrack.domain

object StopwatchCheckpointRecovery {
    const val DEFAULT_UPTIME_WALL_TOLERANCE_MILLIS = 120_000L

    fun pauseRunningAtLastSavedElapsed(checkpoint: StopwatchCheckpoint): StopwatchCheckpoint {
        if (checkpoint.status != StopwatchStatus.RUNNING) return checkpoint

        val knownElapsed = maxOf(
            checkpoint.accumulatedNanos.coerceAtLeast(0L),
            checkpoint.laps.lastOrNull()?.totalNanos ?: 0L,
        )
        return checkpoint.copy(
            status = StopwatchStatus.PAUSED,
            accumulatedNanos = knownElapsed,
            startedAtNanos = null,
        )
    }

    fun recoverSystemUptimeCheckpoint(
        checkpoint: StopwatchCheckpoint,
        currentMonotonicNanos: Long,
        currentEpochMillis: Long,
        toleranceMillis: Long = DEFAULT_UPTIME_WALL_TOLERANCE_MILLIS,
    ): StopwatchCheckpoint {
        if (checkpoint.status != StopwatchStatus.RUNNING) return checkpoint
        require(toleranceMillis >= 0L) { "Recovery tolerance must be non-negative." }

        val savedMonotonicNanos = checkpoint.startedAtNanos
            ?: return pauseRunningAtLastSavedElapsed(checkpoint)
        val savedEpochMillis = checkpoint.savedAtEpochMillis
            ?: return pauseRunningAtLastSavedElapsed(checkpoint)

        if (
            savedMonotonicNanos < 0L ||
            currentMonotonicNanos < savedMonotonicNanos ||
            savedEpochMillis < 0L ||
            currentEpochMillis < savedEpochMillis
        ) {
            return pauseRunningAtLastSavedElapsed(checkpoint)
        }

        val monotonicDeltaMillis =
            (currentMonotonicNanos - savedMonotonicNanos) / NANOS_PER_MILLISECOND
        val wallDeltaMillis = currentEpochMillis - savedEpochMillis
        val deltaDifferenceMillis = if (monotonicDeltaMillis >= wallDeltaMillis) {
            monotonicDeltaMillis - wallDeltaMillis
        } else {
            wallDeltaMillis - monotonicDeltaMillis
        }

        return if (deltaDifferenceMillis <= toleranceMillis) {
            checkpoint
        } else {
            pauseRunningAtLastSavedElapsed(checkpoint)
        }
    }
}
