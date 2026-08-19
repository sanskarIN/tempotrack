package in.sanskar.tempotrack

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import in.sanskar.tempotrack.data.JsonActiveStopwatchRepository
import in.sanskar.tempotrack.data.JsonPreferencesRepository
import in.sanskar.tempotrack.data.JsonSessionRepository
import in.sanskar.tempotrack.domain.MonotonicClock
import in.sanskar.tempotrack.domain.WallClock
import in.sanskar.tempotrack.ui.TempoTrackApp
import in.sanskar.tempotrack.ui.TempoTrackDependencies

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionsStorage = AndroidStringStorage(filesDir.resolve("sessions.json"))
        val preferencesStorage = AndroidStringStorage(filesDir.resolve("preferences.json"))
        val activeStorage = AndroidStringStorage(filesDir.resolve("active-stopwatch.json"))

        val dependencies = TempoTrackDependencies(
            monotonicClock = MonotonicClock { SystemClock.elapsedRealtimeNanos() },
            wallClock = WallClock { System.currentTimeMillis() },
            sessions = JsonSessionRepository(sessionsStorage),
            preferences = JsonPreferencesRepository(preferencesStorage),
            activeStopwatch = JsonActiveStopwatchRepository(activeStorage),
            exporter = AndroidExporter(this),
            platformName = "Android",
            versionName = BuildConfig.VERSION_NAME,
            shareService = AndroidShareService(this),
        )

        setContent {
            TempoTrackApp(dependencies = dependencies)
        }
    }
}
