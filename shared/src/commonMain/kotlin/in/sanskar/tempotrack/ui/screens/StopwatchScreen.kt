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
import in.sanskar.tempotrack.domain.StopwatchEngine
import in.sanskar.tempotrack.domain.StopwatchSession
import in.sanskar.tempotrack.domain.StopwatchStatus
import in.sanskar.tempotrack.domain.WallClock
import in.sanskar.tempotrack.ui.EnglishTempoTrackStrings
import in.sanskar.tempotrack.ui.TempoMotion
import in.sanskar.tempotrack.ui.TempoTrackStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    strings: TempoTrackStrings = EnglishTempoTrackStrings,
) {
    var snapshot by remember(engine) { mutableStateOf(engine.snapshot()) }
    var sessionName by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var lapSort by remember { mutableStateOf(LapSort.RECORDED) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(engine) {
        while (isActive) {
            delay(
                if (snapshot.status == StopwatchStatus.RUNNING) {
                    TempoMotion.RUNNING_REFRESH_MILLIS
                } else {
                    TempoMotion.IDLE_REFRESH_MILLIS
                },
            )
            val latest = engine.snapshot()
            if (latest != snapshot) snapshot = latest
        }
    }

    val stats = remember(snapshot.laps) { LapStatistics.from(snapshot.laps) }
    val buttonHeight = if (largeControls) 68.dp else 52.dp

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val formattedElapsed = DurationFormatter.formatNanos(snapshot.elapsedNanos)
        Text(
            text = formattedElapsed,
            modifier = Modifier.semantics {
                contentDescription = "${strings.elapsedTime} $formattedElapsed"
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
                        snapshot = engine.start()
                        scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
                    },
                ) { Text(strings.start) }

                StopwatchStatus.RUNNING -> {
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.pause()
                            scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text(strings.pause) }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.lap()
                            scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text(strings.lap) }
                }

                StopwatchStatus.PAUSED -> {
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.resume()
                            scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text(strings.resume) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.reset()
                            sessionName = ""
                            scope.launch { runCatching { activeStopwatch.clear() } }
                        },
                    ) { Text(strings.reset) }
                }
            }
        }

        if (snapshot.elapsedNanos > 0L) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = sessionName,
                onValueChange = { sessionName = it.take(80) },
                singleLine = true,
                label = { Text(strings.sessionName) },
                supportingText = { Text(strings.sessionNameHint) },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                onClick = {
                    val finalSnapshot = engine.snapshot()
                    val now = wallClock.nowEpochMillis()
                    val safeName = sessionName.trim().ifEmpty { "${strings.defaultSessionName} $now" }
                    val session = StopwatchSession(
                        id = "$now-${finalSnapshot.elapsedNanos}",
                        name = safeName,
                        createdAtEpochMillis = now,
                        durationNanos = finalSnapshot.elapsedNanos,
                        laps = finalSnapshot.laps,
                    )
                    scope.launch {
                        runCatching { sessions.upsert(session) }
                            .onSuccess { savedMessage = "${strings.saved} “$safeName”" }
                            .onFailure { savedMessage = strings.saveFailed }
                    }
                },
            ) { Text(strings.saveSession) }
        }

        savedMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        if (snapshot.laps.isEmpty()) {
            Text(
                strings.recordLapHint,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LapSummary(stats = stats, strings = strings)
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
                        Text(
                            when (option) {
                                LapSort.RECORDED -> strings.recorded
                                LapSort.FASTEST -> strings.fastest
                                LapSort.SLOWEST -> strings.slowest
                            },
                        )
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
                    LapCard(lap = lap, stats = stats, strings = strings)
                }
            }
        }
    }
}

@Composable
private fun LapSummary(
    stats: LapStatistics,
    strings: TempoTrackStrings,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${strings.fastest} ${stats.fastest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"}")
        Text("${strings.average} ${DurationFormatter.formatNanos(stats.averageSplitNanos)}")
        Text("${strings.slowest} ${stats.slowest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"}")
    }
}

@Composable
private fun LapCard(
    lap: Lap,
    stats: LapStatistics,
    strings: TempoTrackStrings,
) {
    val descriptor = when (lap.index) {
        stats.fastest?.index -> strings.fastest
        stats.slowest?.index -> strings.slowest
        else -> null
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("${strings.lap} ${lap.index}", fontWeight = FontWeight.SemiBold)
                descriptor?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(DurationFormatter.formatNanos(lap.splitNanos), fontFamily = FontFamily.Monospace)
                Text(
                    "${strings.total} ${DurationFormatter.formatNanos(lap.totalNanos)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
