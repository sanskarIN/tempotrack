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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import in.sanskar.tempotrack.data.Exporter
import in.sanskar.tempotrack.data.ExportResult
import in.sanskar.tempotrack.data.SessionCodec
import in.sanskar.tempotrack.data.SessionImportResult
import in.sanskar.tempotrack.data.SessionImporter
import in.sanskar.tempotrack.data.SessionRepository
import in.sanskar.tempotrack.domain.DurationFormatter
import in.sanskar.tempotrack.domain.LapStatistics
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    sessions: SessionRepository,
    exporter: Exporter,
) {
    var allSessions by remember { mutableStateOf<List<StopwatchSession>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var lastDeleted by remember { mutableStateOf<StopwatchSession?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        runCatching { sessions.all() }
            .onSuccess { allSessions = it }
            .onFailure { message = "Could not read saved sessions." }
    }

    LaunchedEffect(sessions) { reload() }

    val filtered = remember(allSessions, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) allSessions
        else allSessions.filter { it.name.lowercase().contains(normalized) }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it.take(100) },
            singleLine = true,
            label = { Text("Search sessions") },
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
                            exporter,
                            "tempotrack-sessions.json",
                            "application/json",
                            SessionCodec.toJson(allSessions),
                        )
                    }
                },
            ) { Text("Export JSON") }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = allSessions.isNotEmpty(),
                onClick = {
                    scope.launch {
                        message = export(
                            exporter,
                            "tempotrack-sessions.csv",
                            "text/csv",
                            SessionCodec.toCsv(allSessions),
                        )
                    }
                },
            ) { Text("Export CSV") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                importJson = ""
                showImportDialog = true
            },
        ) {
            Text("Restore from JSON backup")
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
                            .onFailure { message = "Could not restore the deleted session." }
                    }
                },
            ) {
                Text("Undo delete “${deleted.name}”")
            }
        }

        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Text(
                if (query.isBlank()) "No saved sessions yet." else "No sessions match your search.",
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
                        onDelete = {
                            scope.launch {
                                runCatching { sessions.delete(session.id) }
                                    .onSuccess {
                                        lastDeleted = session
                                        reload()
                                    }
                                    .onFailure { message = "Could not delete this session." }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore JSON backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste a TempoTrack JSON export. Restoring replaces the current saved history.")
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = importJson,
                        onValueChange = {
                            if (it.length <= SessionImporter.MAX_IMPORT_CHARACTERS) importJson = it
                        },
                        minLines = 6,
                        maxLines = 12,
                        label = { Text("Backup JSON") },
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = importJson.isNotBlank(),
                    onClick = {
                        when (val result = SessionImporter.fromJson(importJson)) {
                            is SessionImportResult.Failure -> message = result.userMessage
                            is SessionImportResult.Success -> {
                                scope.launch {
                                    runCatching { sessions.replaceAll(result.sessions) }
                                        .onSuccess {
                                            lastDeleted = null
                                            importJson = ""
                                            showImportDialog = false
                                            message = "Restored ${result.sessions.size} saved session(s)."
                                            reload()
                                        }
                                        .onFailure { message = "Could not restore this backup." }
                                }
                            }
                        }
                    },
                ) { Text("Replace history") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            },
        )
    }
}

private suspend fun export(
    exporter: Exporter,
    name: String,
    mimeType: String,
    content: String,
): String = when (val result = exporter.export(name, mimeType, content)) {
    is ExportResult.Success -> "Exported to ${result.destination}"
    is ExportResult.Failure -> result.userMessage
}

@Composable
private fun SessionCard(
    session: StopwatchSession,
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
                "${session.laps.size} laps • fastest ${
                    stats.fastest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"
                }",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDelete) { Text("Delete") }
        }
    }
}
