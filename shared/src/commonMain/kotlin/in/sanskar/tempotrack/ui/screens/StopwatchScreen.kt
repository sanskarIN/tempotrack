package in.sanskar.tempotrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import in.sanskar.tempotrack.data.ActiveStopwatchRepository
import in.sanskar.tempotrack.data.SessionRepository
import in.sanskar.tempotrack.domain.DurationFormatter
import in.sanskar.tempotrack.domain.Lap
import in.sanskar.tempotrack.domain.LapStatistics
import in.sanskar.tempotrack.domain.SessionValidation
import in.sanskar.tempotrack.domain.StopwatchEngine
import in.sanskar.tempotrack.domain.StopwatchSession
import in.sanskar.tempotrack.domain.StopwatchStatus
import in.sanskar.tempotrack.domain.WallClock
import in.sanskar.tempotrack.resources.Res
import in.sanskar.tempotrack.resources.action_lap
import in.sanskar.tempotrack.resources.action_pause
import in.sanskar.tempotrack.resources.action_reset
import in.sanskar.tempotrack.resources.action_resume
import in.sanskar.tempotrack.resources.action_start
import in.sanskar.tempotrack.resources.elapsed_time_description
import in.sanskar.tempotrack.resources.lap_average_value
import in.sanskar.tempotrack.resources.lap_descriptor_fastest
import in.sanskar.tempotrack.resources.lap_descriptor_slowest
import in.sanskar.tempotrack.resources.lap_empty
import in.sanskar.tempotrack.resources.lap_fastest_value
import in.sanskar.tempotrack.resources.lap_number
import in.sanskar.tempotrack.resources.lap_slowest_value
import in.sanskar.tempotrack.resources.lap_sort_fastest
import in.sanskar.tempotrack.resources.lap_sort_recorded
import in.sanskar.tempotrack.resources.lap_sort_slowest
import in.sanskar.tempotrack.resources.lap_total
import in.sanskar.tempotrack.resources.save_session
import in.sanskar.tempotrack.resources.session_default_prefix
import in.sanskar.tempotrack.resources.session_name_label
import in.sanskar.tempotrack.resources.session_name_support
import in.sanskar.tempotrack.resources.session_save_failed
import in.sanskar.tempotrack.resources.session_saved_prefix
import in.sanskar.tempotrack.util.suspendResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private enum class LapSort {
    RECORDED,
    FASTEST,
    SLOWEST,
}

@Composable
fun StopwatchScreen(
    engine: StopwatchEngine,
    wallClock: WallClock,
    sessions: SessionRepository,
    activeStopwatch: ActiveStopwatchRepository,
    largeControls: Boolean,
) {
    var snapshot by remember(engine) { mutableStateOf(engine.snapshot()) }
    var sessionName by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var savingSession by remember { mutableStateOf(false) }
    var lapSort by remember { mutableStateOf(LapSort.RECORDED) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(engine) {
        while (isActive) {
            delay(if (snapshot.status == StopwatchStatus.RUNNING) 16L else 200L)
            val latest = engine.snapshot()
            if (latest != snapshot) snapshot = latest
        }
    }

    val stats = remember(snapshot.laps) { LapStatistics.from(snapshot.laps) }
    val buttonHeight = if (largeControls) 68.dp else 52.dp
    val formattedElapsed = DurationFormatter.formatNanos(snapshot.elapsedNanos)
    val elapsedDescription = stringResource(Res.string.elapsed_time_description, formattedElapsed)
    val sessionDefaultPrefix = stringResource(Res.string.session_default_prefix)
    val savedPrefix = stringResource(Res.string.session_saved_prefix)
    val saveFailedMessage = stringResource(Res.string.session_save_failed)

    fun clearSavedFeedback() {
        savedMessage = null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formattedElapsed,
            modifier = Modifier.semantics {
                contentDescription = elapsedDescription
            },
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (largeControls) 48.sp else 40.sp,
        )
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (snapshot.status) {
                StopwatchStatus.IDLE -> Button(
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    onClick = {
                        clearSavedFeedback()
                        snapshot = engine.start()
                        scope.launch { suspendResult { activeStopwatch.save(engine.checkpoint()) } }
                    },
                ) { Text(stringResource(Res.string.action_start)) }

                StopwatchStatus.RUNNING -> {
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            clearSavedFeedback()
                            snapshot = engine.pause()
                            scope.launch { suspendResult { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text(stringResource(Res.string.action_pause)) }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            clearSavedFeedback()
                            snapshot = engine.lap()
                            scope.launch { suspendResult { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text(stringResource(Res.string.action_lap)) }
                }

                StopwatchStatus.PAUSED -> {
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            clearSavedFeedback()
                            snapshot = engine.resume()
                            scope.launch { suspendResult { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text(stringResource(Res.string.action_resume)) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            clearSavedFeedback()
                            snapshot = engine.reset()
                            sessionName = ""
                            scope.launch { suspendResult { activeStopwatch.clear() } }
                        },
                    ) { Text(stringResource(Res.string.action_reset)) }
                }
            }
        }

        if (snapshot.elapsedNanos > 0L) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = sessionName,
                onValueChange = {
                    sessionName = it.take(SessionValidation.MAX_SESSION_NAME_LENGTH)
                    clearSavedFeedback()
                },
                singleLine = true,
                label = { Text(stringResource(Res.string.session_name_label)) },
                supportingText = { Text(stringResource(Res.string.session_name_support)) },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                enabled = !savingSession,
                onClick = {
                    if (savingSession) return@OutlinedButton
                    val finalSnapshot = engine.snapshot()
                    val now = wallClock.nowEpochMillis()
                    val safeName = sessionName.trim().ifEmpty { "$sessionDefaultPrefix $now" }
                    val session = StopwatchSession(
                        id = "$now-${finalSnapshot.elapsedNanos}",
                        name = safeName,
                        createdAtEpochMillis = now,
                        durationNanos = finalSnapshot.elapsedNanos,
                        laps = finalSnapshot.laps,
                    )
                    savingSession = true
                    scope.launch {
                        suspendResult { sessions.upsert(session) }
                            .onSuccess {
                                savedMessage = "$savedPrefix “$safeName”"
                                savingSession = false
                            }
                            .onFailure {
                                savedMessage = saveFailedMessage
                                savingSession = false
                            }
                    }
                },
            ) { Text(stringResource(Res.string.save_session)) }
        }

        savedMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        if (snapshot.laps.isEmpty()) {
            Text(
                stringResource(Res.string.lap_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LapSummary(stats)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LapSort.entries.forEach { option ->
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { lapSort = option },
                    ) {
                        Text(option.localizedLabel())
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val visibleLaps = when (lapSort) {
                LapSort.RECORDED -> snapshot.laps.asReversed()
                LapSort.FASTEST -> snapshot.laps.sortedBy(Lap::splitNanos)
                LapSort.SLOWEST -> snapshot.laps.sortedByDescending(Lap::splitNanos)
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleLaps, key = Lap::index) { lap ->
                    LapCard(lap, stats)
                }
            }
        }
    }
}

@Composable
private fun LapSort.localizedLabel(): String = when (this) {
    LapSort.RECORDED -> stringResource(Res.string.lap_sort_recorded)
    LapSort.FASTEST -> stringResource(Res.string.lap_sort_fastest)
    LapSort.SLOWEST -> stringResource(Res.string.lap_sort_slowest)
}

@Composable
private fun LapSummary(stats: LapStatistics) {
    val fastest = stats.fastest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"
    val slowest = stats.slowest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"
    val average = DurationFormatter.formatNanos(stats.averageSplitNanos)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(Res.string.lap_fastest_value, fastest))
        Text(stringResource(Res.string.lap_average_value, average))
        Text(stringResource(Res.string.lap_slowest_value, slowest))
    }
}

@Composable
private fun LapCard(lap: Lap, stats: LapStatistics) {
    val descriptor = when (lap.index) {
        stats.fastest?.index -> stringResource(Res.string.lap_descriptor_fastest)
        stats.slowest?.index -> stringResource(Res.string.lap_descriptor_slowest)
        else -> null
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(Res.string.lap_number, lap.index.toString()), fontWeight = FontWeight.SemiBold)
                descriptor?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(DurationFormatter.formatNanos(lap.splitNanos), fontFamily = FontFamily.Monospace)
                Text(
                    stringResource(Res.string.lap_total, DurationFormatter.formatNanos(lap.totalNanos)),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
