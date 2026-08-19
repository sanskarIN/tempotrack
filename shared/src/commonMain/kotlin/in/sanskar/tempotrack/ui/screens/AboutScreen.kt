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
import in.sanskar.tempotrack.resources.Res
import in.sanskar.tempotrack.resources.about_bmc
import in.sanskar.tempotrack.resources.about_business_gmail
import in.sanskar.tempotrack.resources.about_business_outlook
import in.sanskar.tempotrack.resources.about_github
import in.sanskar.tempotrack.resources.about_license
import in.sanskar.tempotrack.resources.about_privacy
import in.sanskar.tempotrack.resources.about_support
import in.sanskar.tempotrack.resources.about_tagline
import in.sanskar.tempotrack.resources.about_version_platform
import in.sanskar.tempotrack.resources.app_name
import in.sanskar.tempotrack.resources.made_by
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(
    platformName: String,
    versionName: String,
) {
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(Res.string.about_tagline))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.about_version_platform, versionName, platformName))
        Text(stringResource(Res.string.about_license))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.made_by), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        LinkButton(stringResource(Res.string.about_github), "https://github.com/sanskarIN", uriHandler::openUri)
        LinkButton(stringResource(Res.string.about_bmc), "https://buymeacoffee.com/sanskarIN", uriHandler::openUri)
        Spacer(Modifier.height(12.dp))
        LinkButton(
            stringResource(Res.string.about_business_outlook),
            "mailto:sanskarin@outlook.in",
            uriHandler::openUri,
        )
        LinkButton(
            stringResource(Res.string.about_business_gmail),
            "mailto:sanskarin.business@gmail.com",
            uriHandler::openUri,
        )
        LinkButton(
            stringResource(Res.string.about_support),
            "mailto:supportramsandesh@gmail.com",
            uriHandler::openUri,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(Res.string.about_privacy),
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
