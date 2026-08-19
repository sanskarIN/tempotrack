package in.sanskar.tempotrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.data.ActiveStopwatchRepository
import in.sanskar.tempotrack.domain.DurationFormatter
import in.sanskar.tempotrack.domain.StopwatchEngine
import in.sanskar.tempotrack.domain.StopwatchStatus
import in.sanskar.tempotrack.resources.Res
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun MiniStopwatch(
    engine: StopwatchEngine,
    activeStopwatch: ActiveStopwatchRepository,
) {
    var snapshot by remember(engine) { mutableStateOf(engine.snapshot()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(engine) {
        while (isActive) {
            delay(if (snapshot.status == StopwatchStatus.RUNNING) 33L else 200L)
            val latest = engine.snapshot()
            if (latest != snapshot) snapshot = latest
        }
    }

    fun persist() {
        scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
    }

    Column(Modifier.padding(12.dp)) {
        Text(
            DurationFormatter.formatNanos(snapshot.elapsedNanos),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (snapshot.status) {
                StopwatchStatus.IDLE -> Button(
                    onClick = {
                        snapshot = engine.start()
                        persist()
                    },
                ) { Text(stringResource(Res.string.action_start)) }

                StopwatchStatus.RUNNING -> {
                    Button(
                        onClick = {
                            snapshot = engine.pause()
                            persist()
                        },
                    ) { Text(stringResource(Res.string.action_pause)) }
                    OutlinedButton(
                        onClick = {
                            snapshot = engine.lap()
                            persist()
                        },
                    ) { Text(stringResource(Res.string.action_lap)) }
                }

                StopwatchStatus.PAUSED -> {
                    Button(
                        onClick = {
                            snapshot = engine.resume()
                            persist()
                        },
                    ) { Text(stringResource(Res.string.action_resume)) }
                    OutlinedButton(
                        onClick = {
                            snapshot = engine.reset()
                            scope.launch { runCatching { activeStopwatch.clear() } }
                        },
                    ) { Text(stringResource(Res.string.action_reset)) }
                }
            }
        }
    }
}
