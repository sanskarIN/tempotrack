package in.sanskar.tempotrack.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    platformName: String,
    versionName: String,
) {
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("TempoTrack", style = MaterialTheme.typography.headlineMedium)
        Text("Precise timing. Local-first history. Portable data.")
        Spacer(Modifier.height(16.dp))
        Text("Version $versionName • $platformName")
        Text("License: MIT")
        Spacer(Modifier.height(16.dp))
        Text("Made by the Sanskar", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        LinkButton("GitHub", "https://github.com/sanskarIN", uriHandler::openUri)
        LinkButton("Buy Me a Coffee", "https://buymeacoffee.com/sanskarIN", uriHandler::openUri)
        Spacer(Modifier.height(12.dp))
        LinkButton("Business: sanskarin@outlook.in", "mailto:sanskarin@outlook.in", uriHandler::openUri)
        LinkButton("Business: sanskarin.business@gmail.com", "mailto:sanskarin.business@gmail.com", uriHandler::openUri)
        LinkButton("Support: supportramsandesh@gmail.com", "mailto:supportramsandesh@gmail.com", uriHandler::openUri)
        Spacer(Modifier.height(20.dp))
        Text(
            "TempoTrack stores stopwatch sessions locally. Nothing is uploaded unless you choose to export and share a file.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LinkButton(
    label: String,
    uri: String,
    open: (String) -> Unit,
) {
    TextButton(onClick = { open(uri) }) {
        Text(label)
    }
}
