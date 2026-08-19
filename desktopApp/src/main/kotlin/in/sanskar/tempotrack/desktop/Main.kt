package in.sanskar.tempotrack.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import in.sanskar.tempotrack.data.JsonActiveStopwatchRepository
import in.sanskar.tempotrack.data.JsonPreferencesRepository
import in.sanskar.tempotrack.data.JsonSessionRepository
import in.sanskar.tempotrack.domain.MonotonicClock
import in.sanskar.tempotrack.domain.StopwatchEngine
import in.sanskar.tempotrack.domain.WallClock
import in.sanskar.tempotrack.ui.MiniStopwatch
import in.sanskar.tempotrack.ui.TempoTrackApp
import in.sanskar.tempotrack.ui.TempoTrackDependencies
import java.nio.file.Paths
import kotlinx.coroutines.launch

fun main() = application {
    val home = Paths.get(System.getProperty("user.home"), ".tempotrack")
    val sessionsStorage = remember { JvmStringStorage(home.resolve("sessions.json")) }
    val preferencesStorage = remember { JvmStringStorage(home.resolve("preferences.json")) }
    val activeStorage = remember { JvmStringStorage(home.resolve("active-stopwatch.json")) }
    val monotonicClock = remember { MonotonicClock { System.nanoTime() } }
    var miniVisible by remember { mutableStateOf(false) }
    var sharedEngine by remember { mutableStateOf<StopwatchEngine?>(null) }
    val scope = rememberCoroutineScope()

    val dependencies = remember {
        TempoTrackDependencies(
            monotonicClock = monotonicClock,
            wallClock = WallClock { System.currentTimeMillis() },
            sessions = JsonSessionRepository(sessionsStorage),
            preferences = JsonPreferencesRepository(preferencesStorage),
            activeStopwatch = JsonActiveStopwatchRepository(activeStorage),
            exporter = DesktopExporter(),
            platformName = "Desktop",
            versionName = "1.0.0",
            miniStopwatchSupported = true,
            setMiniStopwatchVisible = { miniVisible = it },
            keyboardShortcutsSupported = true,
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "TempoTrack",
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else {
                val engine = sharedEngine
                when {
                    engine == null -> false
                    event.key == Key.Spacebar -> {
                        when (engine.snapshot().status) {
                            in.sanskar.tempotrack.domain.StopwatchStatus.IDLE -> engine.start()
                            in.sanskar.tempotrack.domain.StopwatchStatus.RUNNING -> engine.pause()
                            in.sanskar.tempotrack.domain.StopwatchStatus.PAUSED -> engine.resume()
                        }
                        scope.launch { runCatching { dependencies.activeStopwatch.save(engine.checkpoint()) } }
                        true
                    }
                    event.key == Key.L -> {
                        engine.lap()
                        scope.launch { runCatching { dependencies.activeStopwatch.save(engine.checkpoint()) } }
                        true
                    }
                    event.key == Key.R -> {
                        engine.reset()
                        scope.launch { runCatching { dependencies.activeStopwatch.clear() } }
                        true
                    }
                    else -> false
                }
            }
        },
    ) {
        TempoTrackApp(
            dependencies = dependencies,
            wideLayout = window.width >= 900,
            onEngineReady = { sharedEngine = it },
        )
    }

    if (miniVisible && sharedEngine != null) {
        Window(
            onCloseRequest = { miniVisible = false },
            title = "TempoTrack Mini",
            alwaysOnTop = true,
            resizable = false,
        ) {
            window.setSize(360, 180)
            MiniStopwatch(
                engine = requireNotNull(sharedEngine),
                activeStopwatch = dependencies.activeStopwatch,
            )
        }
    }
}
