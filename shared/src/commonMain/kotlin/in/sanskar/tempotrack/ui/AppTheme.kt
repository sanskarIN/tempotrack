package in.sanskar.tempotrack.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import in.sanskar.tempotrack.data.ThemePreference

@Composable
fun TempoTrackTheme(
    preference: ThemePreference,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
        shapes = TempoTrackShapes,
        typography = TempoTrackTypography,
        content = content,
    )
}
