package in.sanskar.tempotrack.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.data.AppPreferences
import in.sanskar.tempotrack.data.PreferencesRepository
import in.sanskar.tempotrack.data.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    repository: PreferencesRepository,
    onPreferencesChanged: (AppPreferences) -> Unit,
    miniStopwatchSupported: Boolean,
    setMiniStopwatchVisible: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var miniVisible by remember { mutableStateOf(false) }

    fun update(next: AppPreferences) {
        onPreferencesChanged(next)
        scope.launch { runCatching { repository.save(next) } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium)

        ThemePreference.entries.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = preferences.theme == option,
                    onClick = { update(preferences.copy(theme = option)) },
                )
                Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Accessibility", style = MaterialTheme.typography.titleMedium)
        ToggleRow(
            title = "Large controls",
            subtitle = "Increase primary touch target and timer sizes.",
            checked = preferences.largeControls,
            onCheckedChange = { update(preferences.copy(largeControls = it)) },
        )
        ToggleRow(
            title = "Reduced motion",
            subtitle = "Prefer minimal motion where animations are used.",
            checked = preferences.reducedMotion,
            onCheckedChange = { update(preferences.copy(reducedMotion = it)) },
        )

        if (miniStopwatchSupported) {
            Spacer(Modifier.height(16.dp))
            Text("Desktop", style = MaterialTheme.typography.titleMedium)
            ToggleRow(
                title = "Floating mini stopwatch",
                subtitle = "Show a compact always-on-top timer window.",
                checked = miniVisible,
                onCheckedChange = {
                    miniVisible = it
                    setMiniStopwatchVisible(it)
                },
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            "TempoTrack does not require an account and does not transmit session data.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        Text("Data", style = MaterialTheme.typography.titleMedium)
        Text(
            "Saved sessions and preferences use application-private local storage. Use History to export JSON backups or CSV data.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        Text("Updates", style = MaterialTheme.typography.titleMedium)
        Text(
            "Release information is published at https://github.com/sanskarIN/tempotrack/releases",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        Text("About", style = MaterialTheme.typography.titleMedium)
        Text(
            "Open-source MIT project • Made by the Sanskar",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
