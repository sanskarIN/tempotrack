package `in`.sanskar.tempotrack.domain

class StopwatchEngine(
    private val clock: MonotonicClock,
    checkpoint: StopwatchCheckpoint = StopwatchCheckpoint(),
    private val wallClock: WallClock? = null,
) {
    private var status: StopwatchStatus = checkpoint.status
    private var accumulatedNanos: Long = checkpoint.accumulatedNanos.coerceAtLeast(0L)
    private var startedAtNanos: Long? = checkpoint.startedAtNanos
    private val laps: MutableList<Lap> = checkpoint.laps.toMutableList()

    init {
        if (status == StopwatchStatus.RUNNING) {
            val started = startedAtNanos
            val now = clock.nowNanos()
            if (started == null || started > now) {
                accumulatedNanos = maxOf(accumulatedNanos, laps.lastOrNull()?.totalNanos ?: 0L)
                startedAtNanos = null
                status = StopwatchStatus.PAUSED
            }
        }
    }

    fun start(): StopwatchSnapshot {
        if (status == StopwatchStatus.IDLE) {
            accumulatedNanos = 0L
            laps.clear()
            startedAtNanos = clock.nowNanos()
            status = StopwatchStatus.RUNNING
        }
        return snapshot()
    }

    fun pause(): StopwatchSnapshot {
        if (status == StopwatchStatus.RUNNING) {
            val now = clock.nowNanos()
            accumulatedNanos = elapsedAt(now)
            startedAtNanos = null
            status = StopwatchStatus.PAUSED
        }
        return snapshot()
    }

    fun resume(): StopwatchSnapshot {
        if (status == StopwatchStatus.PAUSED) {
            startedAtNanos = clock.nowNanos()
            status = StopwatchStatus.RUNNING
        }
        return snapshot()
    }

    fun reset(): StopwatchSnapshot {
        status = StopwatchStatus.IDLE
        accumulatedNanos = 0L
        startedAtNanos = null
        laps.clear()
        return snapshot()
    }

    fun lap(): StopwatchSnapshot {
        if (status != StopwatchStatus.RUNNING) return snapshot()
        if (laps.size >= SessionValidation.MAX_LAPS_PER_SESSION) return snapshot()

        val total = elapsedAt(clock.nowNanos())
        val previousTotal = laps.lastOrNull()?.totalNanos ?: 0L
        val split = (total - previousTotal).coerceAtLeast(0L)
        laps += Lap(
            index = laps.size + 1,
            splitNanos = split,
            totalNanos = total,
        )
        return snapshot()
    }

    fun snapshot(): StopwatchSnapshot {
        val now = if (status == StopwatchStatus.RUNNING) clock.nowNanos() else 0L
        val elapsed = if (status == StopwatchStatus.RUNNING) elapsedAt(now) else accumulatedNanos
        return StopwatchSnapshot(
            status = status,
            elapsedNanos = elapsed.coerceAtLeast(0L),
            laps = laps.toList(),
        )
    }

    fun checkpoint(savedAtEpochMillis: Long? = wallClock?.nowEpochMillis()): StopwatchCheckpoint {
        if (status != StopwatchStatus.RUNNING) {
            return StopwatchCheckpoint(
                status = status,
                accumulatedNanos = accumulatedNanos,
                startedAtNanos = startedAtNanos,
                savedAtEpochMillis = savedAtEpochMillis,
                laps = laps.toList(),
            )
        }

        val now = clock.nowNanos()
        val elapsedAtSave = maxOf(
            elapsedAt(now),
            laps.lastOrNull()?.totalNanos ?: 0L,
        )
        return StopwatchCheckpoint(
            status = StopwatchStatus.RUNNING,
            accumulatedNanos = elapsedAtSave,
            startedAtNanos = now,
            savedAtEpochMillis = savedAtEpochMillis,
            laps = laps.toList(),
        )
    }

    private fun elapsedAt(nowNanos: Long): Long {
        val started = startedAtNanos ?: return accumulatedNanos
        val activeDelta = (nowNanos - started).coerceAtLeast(0L)
        return saturatingAddNonNegative(accumulatedNanos, activeDelta)
    }

    private fun saturatingAddNonNegative(left: Long, right: Long): Long =
        if (right > Long.MAX_VALUE - left) Long.MAX_VALUE else left + right
}
