package in.sanskar.tempotrack.domain

object StopwatchCheckpointRecovery {
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
}
