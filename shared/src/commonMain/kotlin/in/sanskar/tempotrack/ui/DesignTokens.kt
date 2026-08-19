package in.sanskar.tempotrack.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp

object TempoTrackSpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 20.dp
    val section = 24.dp
    val page = 20.dp
}

object TempoTrackSizing {
    val controlHeight = 52.dp
    val largeControlHeight = 68.dp
    val minimumTouchTarget = 48.dp
}

val TempoTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val TempoTrackTypography = Typography()

object TempoTrackMotion {
    const val quickMillis: Int = 120
    const val standardMillis: Int = 220
}
