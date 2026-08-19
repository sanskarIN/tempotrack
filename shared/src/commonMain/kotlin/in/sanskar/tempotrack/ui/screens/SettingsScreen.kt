package in.sanskar.tempotrack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import in.sanskar.tempotrack.ui.EnglishTempoTrackStrings
import in.sanskar.tempotrack.ui.TempoTrackStrings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    repository: PreferencesRepository,
    onPreferencesChanged: (AppPreferences) -> Unit,
    miniStopwatchSupported: Boolean,
    setMiniStopwatchVisible: (Boolean) -> Unit,
    strings: TempoTrackStrings = EnglishTempoTrackStrings,
) {
    val scope = rememberCoroutineScope()
    var miniVisible by remember { mutableStateOf(false) }

    fun update(next: AppPreferences) {
        onPreferencesChanged(next)
        scope.launch { runCatching { repository.save(next) } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(strings.settings, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(strings.appearance, style = MaterialTheme.typography.titleMedium)

        ThemePreference.entries.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = preferences.theme == option,
                    onClick = { update(preferences.copy(theme = option)) },
                )
                Text(
                    when (option) {
                        ThemePreference.SYSTEM -> strings.system
                        ThemePreference.LIGHT -> strings.light
                        ThemePreference.DARK -> strings.dark
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(strings.accessibility, style = MaterialTheme.typography.titleMedium)
        ToggleRow(
            title = strings.largeControls,
            subtitle = strings.largeControlsDescription,
            checked = preferences.largeControls,
            onCheckedChange = { update(preferences.copy(largeControls = it)) },
        )
        ToggleRow(
            title = strings.reducedMotion,
            subtitle = strings.reducedMotionDescription,
            checked = preferences.reducedMotion,
            onCheckedChange = { update(preferences.copy(reducedMotion = it)) },
        )

        if (miniStopwatchSupported) {
            Spacer(Modifier.height(16.dp))
            Text(strings.desktop, style = MaterialTheme.typography.titleMedium)
            ToggleRow(
                title = strings.floatingStopwatch,
                subtitle = strings.floatingStopwatchDescription,
                checked = miniVisible,
                onCheckedChange = {
                    miniVisible = it
                    setMiniStopwatchVisible(it)
                },
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(strings.privacy, style = MaterialTheme.typography.titleMedium)
        Text(strings.privacyDescription, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Text(strings.data, style = MaterialTheme.typography.titleMedium)
        Text(strings.dataDescription, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Text(strings.updates, style = MaterialTheme.typography.titleMedium)
        Text(strings.updatesDescription, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Text(strings.about, style = MaterialTheme.typography.titleMedium)
        Text(strings.openSourceCredit, style = MaterialTheme.typography.bodyMedium)
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
