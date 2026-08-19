package in.sanskar.tempotrack.ui

import androidx.compose.ui.platform.UriHandler

fun UriHandler.openUriSafely(uri: String): Boolean = try {
    openUri(uri)
    true
} catch (_: Exception) {
    false
}
