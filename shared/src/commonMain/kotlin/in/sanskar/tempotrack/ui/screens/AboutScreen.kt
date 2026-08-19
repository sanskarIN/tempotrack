package in.sanskar.tempotrack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.ui.EnglishTempoTrackStrings
import in.sanskar.tempotrack.ui.TempoTrackStrings

@Composable
fun AboutScreen(
    platformName: String,
    versionName: String,
    strings: TempoTrackStrings = EnglishTempoTrackStrings,
) {
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(strings.appName, style = MaterialTheme.typography.headlineMedium)
        Text(strings.tagline)
        Spacer(Modifier.height(16.dp))
        Text("${strings.version} $versionName • $platformName")
        Text(strings.license)
        Spacer(Modifier.height(16.dp))
        Text(strings.credit, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        LinkButton(strings.github, "https://github.com/sanskarIN", uriHandler::openUri)
        LinkButton(strings.buyMeACoffee, "https://buymeacoffee.com/sanskarIN", uriHandler::openUri)
        Spacer(Modifier.height(12.dp))
        LinkButton(strings.businessPrimary, "mailto:sanskarin@outlook.in", uriHandler::openUri)
        LinkButton(strings.businessSecondary, "mailto:sanskarin.business@gmail.com", uriHandler::openUri)
        LinkButton(strings.support, "mailto:supportramsandesh@gmail.com", uriHandler::openUri)
        Spacer(Modifier.height(20.dp))
        Text(strings.localDataDescription, style = MaterialTheme.typography.bodyMedium)
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
