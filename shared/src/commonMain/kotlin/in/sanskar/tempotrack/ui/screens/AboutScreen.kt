package `in`.sanskar.tempotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import `in`.sanskar.tempotrack.resources.Res
import `in`.sanskar.tempotrack.resources.about_bmc
import `in`.sanskar.tempotrack.resources.about_business_gmail
import `in`.sanskar.tempotrack.resources.about_business_outlook
import `in`.sanskar.tempotrack.resources.about_github
import `in`.sanskar.tempotrack.resources.about_license
import `in`.sanskar.tempotrack.resources.about_privacy
import `in`.sanskar.tempotrack.resources.about_support
import `in`.sanskar.tempotrack.resources.about_tagline
import `in`.sanskar.tempotrack.resources.about_version_platform
import `in`.sanskar.tempotrack.resources.app_name
import `in`.sanskar.tempotrack.resources.external_link_failed
import `in`.sanskar.tempotrack.resources.made_by
import `in`.sanskar.tempotrack.ui.openUriSafely
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(
    platformName: String,
    versionName: String,
) {
    val uriHandler = LocalUriHandler.current
    var linkFailed by remember { mutableStateOf(false) }

    fun open(uri: String) {
        linkFailed = !uriHandler.openUriSafely(uri)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(Res.string.about_tagline))
        Text(stringResource(Res.string.about_version_platform, versionName, platformName))
        Text(stringResource(Res.string.made_by), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.about_license))
        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { open("https://github.com/sanskarIN/tempotrack") },
        ) {
            Text(stringResource(Res.string.about_github))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { open("https://buymeacoffee.com/sanskarIN") },
        ) {
            Text(stringResource(Res.string.about_bmc))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { open("mailto:sanskarin@outlook.in") },
        ) {
            Text(stringResource(Res.string.about_business_outlook))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { open("mailto:sanskarin.business@gmail.com") },
        ) {
            Text(stringResource(Res.string.about_business_gmail))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { open("mailto:supportramsandesh@gmail.com") },
        ) {
            Text(stringResource(Res.string.about_support))
        }

        if (linkFailed) {
            Text(
                stringResource(Res.string.external_link_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.about_privacy),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
