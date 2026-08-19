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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.data.AppPreferences
import in.sanskar.tempotrack.data.PreferencesRepository
import in.sanskar.tempotrack.data.ThemePreference
import in.sanskar.tempotrack.resources.Res
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    repository: PreferencesRepository,
    onPreferencesChanged: (AppPreferences) -> Unit,
    miniStopwatchSupported: Boolean,
    setMiniStopwatchVisible: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var miniVisible by remember { mutableStateOf(false) }

    fun update(next: AppPreferences) {
        onPreferencesChanged(next)
        scope.launch { runCatching { repository.save(next) } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.settings_appearance), style = MaterialTheme.typography.titleMedium)

        ThemePreference.entries.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = preferences.theme == option,
                    onClick = { update(preferences.copy(theme = option)) },
                )
                Text(option.localizedLabel())
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.settings_accessibility), style = MaterialTheme.typography.titleMedium)
        ToggleRow(
            title = stringResource(Res.string.settings_large_controls),
            subtitle = stringResource(Res.string.settings_large_controls_summary),
            checked = preferences.largeControls,
            onCheckedChange = { update(preferences.copy(largeControls = it)) },
        )
        ToggleRow(
            title = stringResource(Res.string.settings_reduced_motion),
            subtitle = stringResource(Res.string.settings_reduced_motion_summary),
            checked = preferences.reducedMotion,
            onCheckedChange = { update(preferences.copy(reducedMotion = it)) },
        )

        if (miniStopwatchSupported) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.settings_desktop), style = MaterialTheme.typography.titleMedium)
            ToggleRow(
                title = stringResource(Res.string.settings_mini_stopwatch),
                subtitle = stringResource(Res.string.settings_mini_stopwatch_summary),
                checked = miniVisible,
                onCheckedChange = {
                    miniVisible = it
                    setMiniStopwatchVisible(it)
                },
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(Res.string.settings_privacy), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.settings_privacy_summary),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.settings_data), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.settings_data_summary),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.settings_updates), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.settings_updates_summary),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { uriHandler.openUri("https://github.com/sanskarIN/tempotrack/releases") }) {
            Text(stringResource(Res.string.about_github))
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.settings_about), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.settings_about_summary),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ThemePreference.localizedLabel(): String = when (this) {
    ThemePreference.SYSTEM -> stringResource(Res.string.settings_theme_system)
    ThemePreference.LIGHT -> stringResource(Res.string.settings_theme_light)
    ThemePreference.DARK -> stringResource(Res.string.settings_theme_dark)
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
