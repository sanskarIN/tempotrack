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
) {
    var snapshot by remember(engine) { mutableStateOf(engine.snapshot()) }
    var sessionName by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
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

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = DurationFormatter.formatNanos(snapshot.elapsedNanos),
            modifier = Modifier.semantics {
                contentDescription = "Elapsed time ${DurationFormatter.formatNanos(snapshot.elapsedNanos)}"
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
                ) { Text("Start") }

                StopwatchStatus.RUNNING -> {
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.pause()
                            scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text("Pause") }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.lap()
                            scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text("Lap") }
                }

                StopwatchStatus.PAUSED -> {
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.resume()
                            scope.launch { runCatching { activeStopwatch.save(engine.checkpoint()) } }
                        },
                    ) { Text("Resume") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = {
                            snapshot = engine.reset()
                            sessionName = ""
                            scope.launch { runCatching { activeStopwatch.clear() } }
                        },
                    ) { Text("Reset") }
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
                label = { Text("Session name") },
                supportingText = { Text("Optional; defaults to a timestamp-based name") },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                onClick = {
                    val finalSnapshot = engine.snapshot()
                    val now = wallClock.nowEpochMillis()
                    val safeName = sessionName.trim().ifEmpty { "Session $now" }
                    val session = StopwatchSession(
                        id = "$now-${finalSnapshot.elapsedNanos}",
                        name = safeName,
                        createdAtEpochMillis = now,
                        durationNanos = finalSnapshot.elapsedNanos,
                        laps = finalSnapshot.laps,
                    )
                    scope.launch {
                        runCatching { sessions.upsert(session) }
                            .onSuccess { savedMessage = "Saved “$safeName”" }
                            .onFailure { savedMessage = "Could not save this session." }
                    }
                },
            ) { Text("Save session") }
        }

        savedMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        if (snapshot.laps.isEmpty()) {
            Text(
                "Record a lap while the stopwatch is running to compare splits.",
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
                        Text(
                            when (option) {
                                LapSort.RECORDED -> "Recorded"
                                LapSort.FASTEST -> "Fastest"
                                LapSort.SLOWEST -> "Slowest"
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
                    LapCard(lap, stats)
                }
            }
        }
    }
}

@Composable
private fun LapSummary(stats: LapStatistics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Fastest ${stats.fastest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"}")
        Text("Average ${DurationFormatter.formatNanos(stats.averageSplitNanos)}")
        Text("Slowest ${stats.slowest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"}")
    }
}

@Composable
private fun LapCard(lap: Lap, stats: LapStatistics) {
    val descriptor = when (lap.index) {
        stats.fastest?.index -> "Fastest"
        stats.slowest?.index -> "Slowest"
        else -> null
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Lap ${lap.index}", fontWeight = FontWeight.SemiBold)
                descriptor?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(DurationFormatter.formatNanos(lap.splitNanos), fontFamily = FontFamily.Monospace)
                Text(
                    "Total ${DurationFormatter.formatNanos(lap.totalNanos)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
