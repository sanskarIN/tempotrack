package in.sanskar.tempotrack.domain

object SessionValidation {
    const val MAX_SESSION_NAME_LENGTH = 80
    const val MAX_SESSION_ID_LENGTH = 160
    const val MAX_LAPS_PER_SESSION = 100_000

    fun validate(session: StopwatchSession): List<String> {
        val errors = mutableListOf<String>()
        if (session.id.isBlank()) errors += "Session id must not be blank."
        if (session.id.length > MAX_SESSION_ID_LENGTH) errors += "Session id is too long."
        if (session.name.isBlank()) errors += "Session name must not be blank."
        if (session.name.length > MAX_SESSION_NAME_LENGTH) errors += "Session name is too long."
        if (session.createdAtEpochMillis < 0L) errors += "Session creation time is invalid."
        if (session.durationNanos < 0L) errors += "Session duration must not be negative."
        if (session.laps.size > MAX_LAPS_PER_SESSION) errors += "Session contains too many laps."

        var previousTotal = 0L
        session.laps.forEachIndexed { position, lap ->
            val expectedIndex = position + 1
            if (lap.index != expectedIndex) errors += "Lap indices must be consecutive starting at 1."
            if (lap.splitNanos < 0L) errors += "Lap split duration must not be negative."
            if (lap.totalNanos < previousTotal) errors += "Lap totals must be monotonic."
            if (lap.totalNanos > session.durationNanos) errors += "Lap total exceeds session duration."
            if (lap.totalNanos - previousTotal != lap.splitNanos) {
                errors += "Lap split does not match its cumulative total."
            }
            previousTotal = lap.totalNanos
        }

        return errors.distinct()
    }

    fun requireValid(session: StopwatchSession) {
        val errors = validate(session)
        require(errors.isEmpty()) { errors.joinToString(" ") }
    }
}
