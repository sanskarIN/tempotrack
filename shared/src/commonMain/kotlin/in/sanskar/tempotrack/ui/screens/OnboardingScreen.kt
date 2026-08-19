package in.sanskar.tempotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.data.AppPreferences
import in.sanskar.tempotrack.resources.Res
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(
    preferences: AppPreferences,
    onContinue: () -> Unit,
    onPersist: suspend (AppPreferences) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(Res.string.onboarding_value),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(Res.string.onboarding_privacy),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val next = preferences.copy(onboardingCompleted = true)
                scope.launch { onPersist(next) }
                onContinue()
            },
        ) {
            Text(stringResource(Res.string.onboarding_start))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(Res.string.made_by), style = MaterialTheme.typography.labelLarge)
    }
}
