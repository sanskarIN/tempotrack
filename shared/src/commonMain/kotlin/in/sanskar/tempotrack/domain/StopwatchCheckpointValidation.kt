package in.sanskar.tempotrack.domain

object StopwatchCheckpointValidation {
    const val MAX_LAPS: Int = SessionValidation.MAX_LAPS_PER_SESSION

    fun validate(checkpoint: StopwatchCheckpoint): List<String> {
        val errors = mutableListOf<String>()

        if (checkpoint.accumulatedNanos < 0L) {
            errors += "accumulatedNanos must be non-negative"
        }
        if (checkpoint.laps.size > MAX_LAPS) {
            errors += "lap count exceeds $MAX_LAPS"
        }

        when (checkpoint.status) {
            StopwatchStatus.IDLE -> {
                if (checkpoint.accumulatedNanos != 0L) errors += "idle checkpoint must have zero accumulated time"
                if (checkpoint.startedAtNanos != null) errors += "idle checkpoint cannot have a start timestamp"
                if (checkpoint.laps.isNotEmpty()) errors += "idle checkpoint cannot contain laps"
            }

            StopwatchStatus.RUNNING -> {
                val started = checkpoint.startedAtNanos
                if (started == null) {
                    errors += "running checkpoint requires a start timestamp"
                } else if (started < 0L) {
                    errors += "start timestamp must be non-negative"
                }
            }

            StopwatchStatus.PAUSED -> {
                if (checkpoint.startedAtNanos != null) errors += "paused checkpoint cannot have a start timestamp"
            }
        }

        var expectedTotal = 0L
        checkpoint.laps.forEachIndexed { position, lap ->
            val expectedIndex = position + 1
            if (lap.index != expectedIndex) errors += "lap indexes must be sequential"
            if (lap.splitNanos < 0L) errors += "lap split must be non-negative"
            if (lap.totalNanos < 0L) errors += "lap total must be non-negative"

            if (lap.splitNanos >= 0L && expectedTotal <= Long.MAX_VALUE - lap.splitNanos) {
                expectedTotal += lap.splitNanos
                if (lap.totalNanos != expectedTotal) errors += "lap totals must equal cumulative splits"
            } else if (lap.splitNanos >= 0L) {
                errors += "lap totals overflow supported duration"
            }
        }

        if (checkpoint.status != StopwatchStatus.RUNNING && expectedTotal > checkpoint.accumulatedNanos) {
            errors += "lap total cannot exceed accumulated duration"
        }

        return errors.distinct()
    }

    fun isValid(checkpoint: StopwatchCheckpoint): Boolean = validate(checkpoint).isEmpty()
}
