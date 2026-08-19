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
import androidx.compose.material3.AlertDialog
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
import in.sanskar.tempotrack.resources.action_close
import in.sanskar.tempotrack.resources.about_github
import in.sanskar.tempotrack.resources.keyboard_shortcuts_body
import in.sanskar.tempotrack.resources.keyboard_shortcuts_title
import in.sanskar.tempotrack.resources.settings_accessibility
import in.sanskar.tempotrack.resources.settings_about
import in.sanskar.tempotrack.resources.settings_about_summary
import in.sanskar.tempotrack.resources.settings_appearance
import in.sanskar.tempotrack.resources.settings_data
import in.sanskar.tempotrack.resources.settings_data_summary
import in.sanskar.tempotrack.resources.settings_desktop
import in.sanskar.tempotrack.resources.settings_large_controls
import in.sanskar.tempotrack.resources.settings_large_controls_summary
import in.sanskar.tempotrack.resources.settings_mini_stopwatch
import in.sanskar.tempotrack.resources.settings_mini_stopwatch_summary
import in.sanskar.tempotrack.resources.settings_privacy
import in.sanskar.tempotrack.resources.settings_privacy_summary
import in.sanskar.tempotrack.resources.settings_reduced_motion
import in.sanskar.tempotrack.resources.settings_reduced_motion_summary
import in.sanskar.tempotrack.resources.settings_save_failed
import in.sanskar.tempotrack.resources.settings_shortcuts
import in.sanskar.tempotrack.resources.settings_shortcuts_button
import in.sanskar.tempotrack.resources.settings_shortcuts_enabled
import in.sanskar.tempotrack.resources.settings_shortcuts_enabled_summary
import in.sanskar.tempotrack.resources.settings_shortcuts_summary
import in.sanskar.tempotrack.resources.settings_theme_dark
import in.sanskar.tempotrack.resources.settings_theme_light
import in.sanskar.tempotrack.resources.settings_theme_system
import in.sanskar.tempotrack.resources.settings_title
import in.sanskar.tempotrack.resources.settings_updates
import in.sanskar.tempotrack.resources.settings_updates_summary
import in.sanskar.tempotrack.util.suspendResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    repository: PreferencesRepository,
    onPreferencesChanged: (AppPreferences) -> Unit,
    miniStopwatchSupported: Boolean,
    setMiniStopwatchVisible: (Boolean) -> Unit,
    keyboardShortcutsSupported: Boolean,
    setKeyboardShortcutsEnabled: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var showShortcutHelp by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var lastPersistedPreferences by remember(repository) { mutableStateOf(preferences) }

    fun update(
        next: AppPreferences,
        onApplied: () -> Unit = {},
        onRollback: (AppPreferences) -> Unit = {},
    ) {
        onPreferencesChanged(next)
        onApplied()
        saveFailed = false
        saveJob?.cancel()
        saveJob = scope.launch {
            suspendResult { repository.save(next) }
                .onSuccess {
                    lastPersistedPreferences = next
                    saveFailed = false
                }
                .onFailure {
                    val rollback = lastPersistedPreferences
                    onPreferencesChanged(rollback)
                    onRollback(rollback)
                    saveFailed = true
                }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        if (saveFailed) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.settings_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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

        if (miniStopwatchSupported || keyboardShortcutsSupported) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.settings_desktop), style = MaterialTheme.typography.titleMedium)
        }

        if (miniStopwatchSupported) {
            ToggleRow(
                title = stringResource(Res.string.settings_mini_stopwatch),
                subtitle = stringResource(Res.string.settings_mini_stopwatch_summary),
                checked = preferences.miniStopwatchVisible,
                onCheckedChange = { visible ->
                    val next = preferences.copy(miniStopwatchVisible = visible)
                    update(
                        next = next,
                        onApplied = { setMiniStopwatchVisible(visible) },
                        onRollback = { rollback -> setMiniStopwatchVisible(rollback.miniStopwatchVisible) },
                    )
                },
            )
        }

        if (keyboardShortcutsSupported) {
            ToggleRow(
                title = stringResource(Res.string.settings_shortcuts_enabled),
                subtitle = stringResource(Res.string.settings_shortcuts_enabled_summary),
                checked = preferences.keyboardShortcutsEnabled,
                onCheckedChange = { enabled ->
                    val next = preferences.copy(keyboardShortcutsEnabled = enabled)
                    update(
                        next = next,
                        onApplied = { setKeyboardShortcutsEnabled(enabled) },
                        onRollback = { rollback -> setKeyboardShortcutsEnabled(rollback.keyboardShortcutsEnabled) },
                    )
                },
            )
            Text(stringResource(Res.string.settings_shortcuts), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.settings_shortcuts_summary),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { showShortcutHelp = true }) {
                Text(stringResource(Res.string.settings_shortcuts_button))
            }
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

    if (showShortcutHelp) {
        AlertDialog(
            onDismissRequest = { showShortcutHelp = false },
            title = { Text(stringResource(Res.string.keyboard_shortcuts_title)) },
            text = { Text(stringResource(Res.string.keyboard_shortcuts_body)) },
            confirmButton = {
                TextButton(onClick = { showShortcutHelp = false }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
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
