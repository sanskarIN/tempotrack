package in.sanskar.tempotrack.domain

class StopwatchEngine(
    private val clock: MonotonicClock,
    checkpoint: StopwatchCheckpoint = StopwatchCheckpoint(),
) {
    private var status: StopwatchStatus = checkpoint.status
    private var accumulatedNanos: Long = checkpoint.accumulatedNanos.coerceAtLeast(0L)
    private var startedAtNanos: Long? = checkpoint.startedAtNanos
    private val laps: MutableList<Lap> = checkpoint.laps.toMutableList()
    private var immutableLaps: List<Lap> = checkpoint.laps.toList()

    init {
        if (status == StopwatchStatus.RUNNING) {
            val started = startedAtNanos
            val now = clock.nowNanos()
            if (started == null || started > now) {
                startedAtNanos = null
                status = StopwatchStatus.PAUSED
            }
        }
    }

    fun start(): StopwatchSnapshot {
        if (status == StopwatchStatus.IDLE) {
            accumulatedNanos = 0L
            laps.clear()
            immutableLaps = emptyList()
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
        immutableLaps = emptyList()
        return snapshot()
    }

    fun lap(): StopwatchSnapshot {
        if (status != StopwatchStatus.RUNNING) return snapshot()

        val total = elapsedAt(clock.nowNanos())
        val previousTotal = laps.lastOrNull()?.totalNanos ?: 0L
        val split = (total - previousTotal).coerceAtLeast(0L)
        laps += Lap(
            index = laps.size + 1,
            splitNanos = split,
            totalNanos = total,
        )
        immutableLaps = laps.toList()
        return snapshot()
    }

    fun snapshot(): StopwatchSnapshot {
        val now = if (status == StopwatchStatus.RUNNING) clock.nowNanos() else 0L
        val elapsed = if (status == StopwatchStatus.RUNNING) elapsedAt(now) else accumulatedNanos
        return StopwatchSnapshot(
            status = status,
            elapsedNanos = elapsed.coerceAtLeast(0L),
            laps = immutableLaps,
        )
    }

    fun checkpoint(): StopwatchCheckpoint = StopwatchCheckpoint(
        status = status,
        accumulatedNanos = accumulatedNanos,
        startedAtNanos = startedAtNanos,
        laps = immutableLaps,
    )

    private fun elapsedAt(nowNanos: Long): Long {
        val started = startedAtNanos ?: return accumulatedNanos
        val activeDelta = (nowNanos - started).coerceAtLeast(0L)
        return accumulatedNanos + activeDelta
    }
}
