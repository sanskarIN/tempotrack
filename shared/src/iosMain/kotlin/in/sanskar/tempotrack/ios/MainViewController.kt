package `in`.sanskar.tempotrack.ios

import androidx.compose.ui.window.ComposeUIViewController
import `in`.sanskar.tempotrack.data.JsonActiveStopwatchRepository
import `in`.sanskar.tempotrack.data.JsonPreferencesRepository
import `in`.sanskar.tempotrack.data.JsonSessionRepository
import `in`.sanskar.tempotrack.domain.StopwatchCheckpointRecovery
import `in`.sanskar.tempotrack.ui.TempoTrackApp
import `in`.sanskar.tempotrack.ui.TempoTrackDependencies
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val sessionsStorage = IosStringStorage("tempotrack.sessions")
    val preferencesStorage = IosStringStorage("tempotrack.preferences")
    val activeStorage = IosStringStorage("tempotrack.active-stopwatch")
    val monotonicClock = iosMonotonicClock()
    val wallClock = iosWallClock()
    var hostController: UIViewController? = null

    val dependencies = TempoTrackDependencies(
        monotonicClock = monotonicClock,
        wallClock = wallClock,
        sessions = JsonSessionRepository(sessionsStorage),
        preferences = JsonPreferencesRepository(preferencesStorage),
        activeStopwatch = JsonActiveStopwatchRepository(activeStorage),
        exporter = IosDocumentExporter { requireNotNull(hostController) },
        shareService = IosShareService { requireNotNull(hostController) },
        platformName = "iOS",
        versionName = iosVersionName(),
        recoverCheckpoint = { checkpoint ->
            StopwatchCheckpointRecovery.recoverSystemUptimeCheckpoint(
                checkpoint = checkpoint,
                currentMonotonicNanos = monotonicClock.nowNanos(),
                currentEpochMillis = wallClock.nowEpochMillis(),
            )
        },
    )

    val controller = ComposeUIViewController {
        TempoTrackApp(dependencies = dependencies)
    }
    hostController = controller
    return controller
}

private fun iosVersionName(): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
        ?.takeIf(String::isNotBlank)
        ?: "1.0.0"
