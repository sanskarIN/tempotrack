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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import in.sanskar.tempotrack.data.ExportResult
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.SessionCodec
import in.sanskar.tempotrack.data.SessionRepository
import in.sanskar.tempotrack.domain.DurationFormatter
import in.sanskar.tempotrack.domain.LapStatistics
import in.sanskar.tempotrack.domain.StopwatchSession
import in.sanskar.tempotrack.ui.EnglishTempoTrackStrings
import in.sanskar.tempotrack.ui.TempoTrackStrings
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    sessions: SessionRepository,
    exporter: Exporter,
    strings: TempoTrackStrings = EnglishTempoTrackStrings,
) {
    var allSessions by remember { mutableStateOf<List<StopwatchSession>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var lastDeleted by remember { mutableStateOf<StopwatchSession?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        runCatching { sessions.all() }
            .onSuccess { allSessions = it }
            .onFailure { message = strings.historyReadFailed }
    }

    LaunchedEffect(sessions) { reload() }

    val filtered = remember(allSessions, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) allSessions
        else allSessions.filter { it.name.lowercase().contains(normalized) }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(strings.history, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it.take(100) },
            singleLine = true,
            label = { Text(strings.searchSessions) },
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = allSessions.isNotEmpty(),
                onClick = {
                    scope.launch {
                        message = export(
                            exporter = exporter,
                            name = "tempotrack-sessions.json",
                            mimeType = "application/json",
                            content = SessionCodec.toJson(allSessions),
                            strings = strings,
                        )
                    }
                },
            ) { Text(strings.exportJson) }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = allSessions.isNotEmpty(),
                onClick = {
                    scope.launch {
                        message = export(
                            exporter = exporter,
                            name = "tempotrack-sessions.csv",
                            mimeType = "text/csv",
                            content = SessionCodec.toCsv(allSessions),
                            strings = strings,
                        )
                    }
                },
            ) { Text(strings.exportCsv) }
        }

        message?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        lastDeleted?.let { deleted ->
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { sessions.upsert(deleted) }
                            .onSuccess {
                                lastDeleted = null
                                reload()
                            }
                            .onFailure { message = strings.restoreFailed }
                    }
                },
            ) {
                Text("${strings.undoDelete} “${deleted.name}”")
            }
        }

        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Text(
                if (query.isBlank()) strings.noSavedSessions else strings.noMatchingSessions,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = StopwatchSession::id) { session ->
                    SessionCard(
                        session = session,
                        strings = strings,
                        onDelete = {
                            scope.launch {
                                runCatching { sessions.delete(session.id) }
                                    .onSuccess {
                                        lastDeleted = session
                                        reload()
                                    }
                                    .onFailure { message = strings.deleteFailed }
                            }
                        },
                    )
                }
            }
        }
    }
}

private suspend fun export(
    exporter: Exporter,
    name: String,
    mimeType: String,
    content: String,
    strings: TempoTrackStrings,
): String = when (val result = exporter.export(name, mimeType, content)) {
    is ExportResult.Success -> "${strings.exportedTo} ${result.destination}"
    is ExportResult.Failure -> strings.exportFailed
}

@Composable
private fun SessionCard(
    session: StopwatchSession,
    strings: TempoTrackStrings,
    onDelete: () -> Unit,
) {
    val stats = remember(session.laps) { LapStatistics.from(session.laps) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(session.name, fontWeight = FontWeight.SemiBold)
            Text(
                DurationFormatter.formatNanos(session.durationNanos),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${session.laps.size} ${strings.laps} • ${strings.fastest.lowercase()} ${
                    stats.fastest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"
                }",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDelete) { Text(strings.delete) }
        }
    }
}
