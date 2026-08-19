package in.sanskar.tempotrack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import in.sanskar.tempotrack.data.AppPreferences
import in.sanskar.tempotrack.domain.StopwatchCheckpoint
import in.sanskar.tempotrack.domain.StopwatchEngine
import in.sanskar.tempotrack.resources.Res
import in.sanskar.tempotrack.ui.screens.AboutScreen
import in.sanskar.tempotrack.ui.screens.HistoryScreen
import in.sanskar.tempotrack.ui.screens.OnboardingScreen
import in.sanskar.tempotrack.ui.screens.SettingsScreen
import in.sanskar.tempotrack.ui.screens.StopwatchScreen
import org.jetbrains.compose.resources.stringResource

private enum class Destination(val glyph: String) {
    STOPWATCH("⏱"),
    HISTORY("◷"),
    SETTINGS("⚙"),
    ABOUT("ⓘ"),
}

@Composable
private fun Destination.label(): String = when (this) {
    Destination.STOPWATCH -> stringResource(Res.string.nav_stopwatch)
    Destination.HISTORY -> stringResource(Res.string.nav_history)
    Destination.SETTINGS -> stringResource(Res.string.nav_settings)
    Destination.ABOUT -> stringResource(Res.string.nav_about)
}

@Composable
fun TempoTrackApp(
    dependencies: TempoTrackDependencies,
    wideLayout: Boolean = false,
    onEngineReady: (StopwatchEngine) -> Unit = {},
) {
    var loaded by remember { mutableStateOf(false) }
    var preferences by remember { mutableStateOf(AppPreferences()) }
    var engine by remember { mutableStateOf<StopwatchEngine?>(null) }
    var destination by remember { mutableStateOf(Destination.STOPWATCH) }

    LaunchedEffect(dependencies) {
        preferences = runCatching { dependencies.preferences.load() }.getOrDefault(AppPreferences())
        val checkpoint = runCatching { dependencies.activeStopwatch.load() }.getOrNull()
        engine = StopwatchEngine(dependencies.monotonicClock, checkpoint ?: StopwatchCheckpoint())
        onEngineReady(requireNotNull(engine))
        loaded = true
    }

    TempoTrackTheme(preference = preferences.theme) {
        if (!loaded || engine == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@TempoTrackTheme
        }

        if (!preferences.onboardingCompleted) {
            OnboardingScreen(
                onContinue = {
                    preferences = preferences.copy(onboardingCompleted = true)
                },
                onPersist = { next -> runCatching { dependencies.preferences.save(next) } },
                preferences = preferences,
            )
            return@TempoTrackTheme
        }

        if (wideLayout) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    Destination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Text(item.glyph) },
                            label = { Text(item.label()) },
                        )
                    }
                }
                Column(Modifier.weight(1f).fillMaxSize()) {
                    ScreenContent(
                        destination = destination,
                        engine = requireNotNull(engine),
                        dependencies = dependencies,
                        preferences = preferences,
                        onPreferencesChanged = { preferences = it },
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Destination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Text(item.glyph) },
                                label = { Text(item.label()) },
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ScreenContent(
                        destination = destination,
                        engine = requireNotNull(engine),
                        dependencies = dependencies,
                        preferences = preferences,
                        onPreferencesChanged = { preferences = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    destination: Destination,
    engine: StopwatchEngine,
    dependencies: TempoTrackDependencies,
    preferences: AppPreferences,
    onPreferencesChanged: (AppPreferences) -> Unit,
) {
    when (destination) {
        Destination.STOPWATCH -> StopwatchScreen(
            engine = engine,
            wallClock = dependencies.wallClock,
            sessions = dependencies.sessions,
            activeStopwatch = dependencies.activeStopwatch,
            largeControls = preferences.largeControls,
        )

        Destination.HISTORY -> HistoryScreen(
            sessions = dependencies.sessions,
            exporter = dependencies.exporter,
        )

        Destination.SETTINGS -> SettingsScreen(
            preferences = preferences,
            repository = dependencies.preferences,
            onPreferencesChanged = onPreferencesChanged,
            miniStopwatchSupported = dependencies.miniStopwatchSupported,
            setMiniStopwatchVisible = dependencies.setMiniStopwatchVisible,
            keyboardShortcutsSupported = dependencies.keyboardShortcutsSupported,
        )

        Destination.ABOUT -> AboutScreen(
            platformName = dependencies.platformName,
            versionName = dependencies.versionName,
        )
    }
}
