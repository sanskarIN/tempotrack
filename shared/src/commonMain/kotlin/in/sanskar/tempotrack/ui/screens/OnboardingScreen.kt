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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.data.AppPreferences
import in.sanskar.tempotrack.resources.Res
import in.sanskar.tempotrack.resources.app_name
import in.sanskar.tempotrack.resources.made_by
import in.sanskar.tempotrack.resources.onboarding_privacy
import in.sanskar.tempotrack.resources.onboarding_save_failed
import in.sanskar.tempotrack.resources.onboarding_start
import in.sanskar.tempotrack.resources.onboarding_value
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(
    preferences: AppPreferences,
    onContinue: () -> Unit,
    onPersist: suspend (AppPreferences) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

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
            enabled = !saving,
            onClick = {
                val next = preferences.copy(onboardingCompleted = true)
                saving = true
                saveFailed = false
                scope.launch {
                    val persisted = onPersist(next)
                    saving = false
                    if (persisted) {
                        onContinue()
                    } else {
                        saveFailed = true
                    }
                }
            },
        ) {
            Text(stringResource(Res.string.onboarding_start))
        }
        if (saveFailed) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.onboarding_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(Res.string.made_by), style = MaterialTheme.typography.labelLarge)
    }
}
