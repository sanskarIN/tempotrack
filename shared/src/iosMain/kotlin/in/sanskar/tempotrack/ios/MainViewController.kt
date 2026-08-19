package in.sanskar.tempotrack.ios

import androidx.compose.ui.window.ComposeUIViewController
import in.sanskar.tempotrack.data.ExportError
import in.sanskar.tempotrack.data.ExportResult
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.JsonActiveStopwatchRepository
import in.sanskar.tempotrack.data.JsonPreferencesRepository
import in.sanskar.tempotrack.data.JsonSessionRepository
import in.sanskar.tempotrack.ui.TempoTrackApp
import in.sanskar.tempotrack.ui.TempoTrackDependencies
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val sessionsStorage = IosStringStorage("tempotrack.sessions")
    val preferencesStorage = IosStringStorage("tempotrack.preferences")
    val activeStorage = IosStringStorage("tempotrack.active-stopwatch")

    val dependencies = TempoTrackDependencies(
        monotonicClock = iosMonotonicClock(),
        wallClock = iosWallClock(),
        sessions = JsonSessionRepository(sessionsStorage),
        preferences = JsonPreferencesRepository(preferencesStorage),
        activeStopwatch = JsonActiveStopwatchRepository(activeStorage),
        exporter = IosHostExporter,
        platformName = "iOS",
        versionName = "1.0.0",
    )

    return ComposeUIViewController {
        TempoTrackApp(dependencies = dependencies)
    }
}

private object IosHostExporter : Exporter {
    override suspend fun export(
        suggestedFileName: String,
        mimeType: String,
        content: String,
    ): ExportResult = ExportResult.Failure(ExportError.PLATFORM_EXPORT_UNAVAILABLE)
}
