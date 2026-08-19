package in.sanskar.tempotrack.ui

import in.sanskar.tempotrack.data.ActiveStopwatchRepository
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.PreferencesRepository
import in.sanskar.tempotrack.data.SessionRepository
import in.sanskar.tempotrack.domain.MonotonicClock
import in.sanskar.tempotrack.domain.WallClock

data class TempoTrackDependencies(
    val monotonicClock: MonotonicClock,
    val wallClock: WallClock,
    val sessions: SessionRepository,
    val preferences: PreferencesRepository,
    val activeStopwatch: ActiveStopwatchRepository,
    val exporter: Exporter,
    val platformName: String,
    val versionName: String,
    val miniStopwatchSupported: Boolean = false,
    val setMiniStopwatchVisible: (Boolean) -> Unit = {},
    val strings: TempoTrackStrings = EnglishTempoTrackStrings,
)
