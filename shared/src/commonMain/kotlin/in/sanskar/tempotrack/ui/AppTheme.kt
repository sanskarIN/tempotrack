package in.sanskar.tempotrack.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.data.ThemePreference

private val TempoTrackLightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF475569),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    surface = Color(0xFFFAFBFF),
    onSurface = Color(0xFF171C24),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF414752),
    error = Color(0xFFBA1A1A),
)

private val TempoTrackDarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002D6B),
    primaryContainer = Color(0xFF0B3F91),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFC5CEDC),
    onSecondary = Color(0xFF2A303A),
    secondaryContainer = Color(0xFF404751),
    onSecondaryContainer = Color(0xFFDCE3F0),
    surface = Color(0xFF101318),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF414752),
    onSurfaceVariant = Color(0xFFC1C6D0),
    error = Color(0xFFFFB4AB),
)

private val TempoTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object TempoSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xLarge = 24.dp
    val xxLarge = 32.dp
}

object TempoMotion {
    const val RUNNING_REFRESH_MILLIS = 16L
    const val IDLE_REFRESH_MILLIS = 200L
}

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
        colorScheme = if (dark) TempoTrackDarkColors else TempoTrackLightColors,
        shapes = TempoTrackShapes,
        content = content,
    )
}
