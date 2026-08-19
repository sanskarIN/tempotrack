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
import in.sanskar.tempotrack.data.ExportError
import in.sanskar.tempotrack.data.ExportResult
import in.sanskar.tempotrack.data.SessionCodec
import in.sanskar.tempotrack.data.SessionImportError
import in.sanskar.tempotrack.data.SessionImportResult
import in.sanskar.tempotrack.data.SessionImporter
import in.sanskar.tempotrack.data.SessionRepository
import in.sanskar.tempotrack.data.ShareError
import in.sanskar.tempotrack.data.ShareResult
import in.sanskar.tempotrack.data.ShareService
import in.sanskar.tempotrack.domain.DurationFormatter
import in.sanskar.tempotrack.domain.LapStatistics
import in.sanskar.tempotrack.domain.SessionValidation
import in.sanskar.tempotrack.domain.StopwatchSession
import in.sanskar.tempotrack.resources.Res
import in.sanskar.tempotrack.resources.action_cancel
import in.sanskar.tempotrack.resources.action_delete
import in.sanskar.tempotrack.resources.action_rename
import in.sanskar.tempotrack.resources.action_save
import in.sanskar.tempotrack.resources.history_backup_json_label
import in.sanskar.tempotrack.resources.history_delete_failed
import in.sanskar.tempotrack.resources.history_empty
import in.sanskar.tempotrack.resources.history_export_cancelled
import in.sanskar.tempotrack.resources.history_export_csv
import in.sanskar.tempotrack.resources.history_export_json
import in.sanskar.tempotrack.resources.history_export_unavailable
import in.sanskar.tempotrack.resources.history_export_write_failed
import in.sanskar.tempotrack.resources.history_exported_to
import in.sanskar.tempotrack.resources.history_import_duplicate_ids
import in.sanskar.tempotrack.resources.history_import_empty
import in.sanskar.tempotrack.resources.history_import_invalid_data
import in.sanskar.tempotrack.resources.history_import_invalid_json
import in.sanskar.tempotrack.resources.history_import_invalid_session
import in.sanskar.tempotrack.resources.history_import_too_large
import in.sanskar.tempotrack.resources.history_import_too_many_sessions
import in.sanskar.tempotrack.resources.history_no_search_results
import in.sanskar.tempotrack.resources.history_read_failed
import in.sanskar.tempotrack.resources.history_rename_failed
import in.sanskar.tempotrack.resources.history_rename_label
import in.sanskar.tempotrack.resources.history_rename_title
import in.sanskar.tempotrack.resources.history_renamed
import in.sanskar.tempotrack.resources.history_replace
import in.sanskar.tempotrack.resources.history_restore_deleted_failed
import in.sanskar.tempotrack.resources.history_restore_dialog_body
import in.sanskar.tempotrack.resources.history_restore_dialog_title
import in.sanskar.tempotrack.resources.history_restore_failed
import in.sanskar.tempotrack.resources.history_restore_json
import in.sanskar.tempotrack.resources.history_restored_count
import in.sanskar.tempotrack.resources.history_search
import in.sanskar.tempotrack.resources.history_session_summary
import in.sanskar.tempotrack.resources.history_share_csv
import in.sanskar.tempotrack.resources.history_share_failed
import in.sanskar.tempotrack.resources.history_share_json
import in.sanskar.tempotrack.resources.history_share_started
import in.sanskar.tempotrack.resources.history_share_unavailable
import in.sanskar.tempotrack.resources.history_title
import in.sanskar.tempotrack.resources.history_undo_delete
import in.sanskar.tempotrack.util.suspendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun HistoryScreen(
    sessions: SessionRepository,
    exporter: Exporter,
    shareService: ShareService? = null,
) {
    var allSessions by remember { mutableStateOf<List<StopwatchSession>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var lastDeleted by remember { mutableStateOf<StopwatchSession?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var renamingSession by remember { mutableStateOf<StopwatchSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val readFailedMessage = stringResource(Res.string.history_read_failed)
    val restoreDeletedFailedMessage = stringResource(Res.string.history_restore_deleted_failed)
    val deleteFailedMessage = stringResource(Res.string.history_delete_failed)
    val restoreFailedMessage = stringResource(Res.string.history_restore_failed)
    val renameFailedMessage = stringResource(Res.string.history_rename_failed)

    suspend fun reload() {
        suspendResult { sessions.all() }
            .onSuccess { allSessions = it }
            .onFailure { message = readFailedMessage }
    }

    LaunchedEffect(sessions) { reload() }

    val filtered = remember(allSessions, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) allSessions
        else allSessions.filter { it.name.lowercase().contains(normalized) }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(stringResource(Res.string.history_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it.take(100) },
            singleLine = true,
            label = { Text(stringResource(Res.string.history_search)) },
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
                    val snapshot = allSessions
                    scope.launch {
                        val content = withContext(Dispatchers.Default) { SessionCodec.toJson(snapshot) }
                        message = export(
                            exporter,
                            "tempotrack-sessions.json",
                            "application/json",
                            content,
                        )
                    }
                },
            ) { Text(stringResource(Res.string.history_export_json)) }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = allSessions.isNotEmpty(),
                onClick = {
                    val snapshot = allSessions
                    scope.launch {
                        val content = withContext(Dispatchers.Default) { SessionCodec.toCsv(snapshot) }
                        message = export(
                            exporter,
                            "tempotrack-sessions.csv",
                            "text/csv",
                            content,
                        )
                    }
                },
            ) { Text(stringResource(Res.string.history_export_csv)) }
        }

        if (shareService != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = allSessions.isNotEmpty(),
                    onClick = {
                        val snapshot = allSessions
                        scope.launch {
                            val content = withContext(Dispatchers.Default) { SessionCodec.toJson(snapshot) }
                            message = share(
                                shareService,
                                "tempotrack-sessions.json",
                                "application/json",
                                content,
                            )
                        }
                    },
                ) { Text(stringResource(Res.string.history_share_json)) }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = allSessions.isNotEmpty(),
                    onClick = {
                        val snapshot = allSessions
                        scope.launch {
                            val content = withContext(Dispatchers.Default) { SessionCodec.toCsv(snapshot) }
                            message = share(
                                shareService,
                                "tempotrack-sessions.csv",
                                "text/csv",
                                content,
                            )
                        }
                    },
                ) { Text(stringResource(Res.string.history_share_csv)) }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                importJson = ""
                importError = null
                showImportDialog = true
            },
        ) {
            Text(stringResource(Res.string.history_restore_json))
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
                        suspendResult { sessions.upsert(deleted) }
                            .onSuccess {
                                lastDeleted = null
                                reload()
                            }
                            .onFailure { message = restoreDeletedFailedMessage }
                    }
                },
            ) {
                Text(stringResource(Res.string.history_undo_delete, deleted.name))
            }
        }

        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Text(
                if (query.isBlank()) {
                    stringResource(Res.string.history_empty)
                } else {
                    stringResource(Res.string.history_no_search_results)
                },
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
                        onRename = {
                            renamingSession = session
                            renameText = session.name
                            renameError = null
                        },
                        onDelete = {
                            scope.launch {
                                suspendResult { sessions.delete(session.id) }
                                    .onSuccess {
                                        lastDeleted = session
                                        reload()
                                    }
                                    .onFailure { message = deleteFailedMessage }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!importing) {
                    showImportDialog = false
                    importError = null
                }
            },
            title = { Text(stringResource(Res.string.history_restore_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.history_restore_dialog_body))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = importJson,
                        onValueChange = {
                            if (!importing && it.length <= SessionImporter.MAX_IMPORT_CHARACTERS) {
                                importJson = it
                                importError = null
                            }
                        },
                        minLines = 6,
                        maxLines = 12,
                        enabled = !importing,
                        isError = importError != null,
                        label = { Text(stringResource(Res.string.history_backup_json_label)) },
                    )
                    importError?.let { error ->
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = importJson.isNotBlank() && !importing,
                    onClick = {
                        val content = importJson
                        importing = true
                        importError = null
                        scope.launch {
                            try {
                                when (
                                    val result = withContext(Dispatchers.Default) {
                                        SessionImporter.fromJson(content)
                                    }
                                ) {
                                    is SessionImportResult.Failure -> {
                                        importError = importFailureMessage(result)
                                    }

                                    is SessionImportResult.Success -> {
                                        suspendResult { sessions.replaceAll(result.sessions) }
                                            .onSuccess {
                                                lastDeleted = null
                                                importJson = ""
                                                importError = null
                                                showImportDialog = false
                                                message = getString(
                                                    Res.string.history_restored_count,
                                                    result.sessions.size.toString(),
                                                )
                                                reload()
                                            }
                                            .onFailure { importError = restoreFailedMessage }
                                    }
                                }
                            } finally {
                                importing = false
                            }
                        }
                    },
                ) { Text(stringResource(Res.string.history_replace)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !importing,
                    onClick = {
                        showImportDialog = false
                        importError = null
                    },
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    renamingSession?.let { session ->
        AlertDialog(
            onDismissRequest = {
                renamingSession = null
                renameError = null
            },
            title = { Text(stringResource(Res.string.history_rename_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = renameText,
                        onValueChange = {
                            renameText = it.take(SessionValidation.MAX_SESSION_NAME_LENGTH)
                            renameError = null
                        },
                        singleLine = true,
                        isError = renameError != null,
                        label = { Text(stringResource(Res.string.history_rename_label)) },
                    )
                    renameError?.let { error ->
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = renameText.trim().isNotEmpty() && renameText.trim() != session.name,
                    onClick = {
                        val requestedName = renameText.trim()
                        scope.launch {
                            suspendResult { sessions.rename(session.id, requestedName) }
                                .onSuccess { renamed ->
                                    if (renamed) {
                                        renamingSession = null
                                        renameError = null
                                        message = getString(Res.string.history_renamed, requestedName)
                                        reload()
                                    } else {
                                        renameError = renameFailedMessage
                                    }
                                }
                                .onFailure { renameError = renameFailedMessage }
                        }
                    },
                ) { Text(stringResource(Res.string.action_save)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renamingSession = null
                        renameError = null
                    },
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

private suspend fun importFailureMessage(failure: SessionImportResult.Failure): String = when (failure.error) {
    SessionImportError.EMPTY_BACKUP -> getString(Res.string.history_import_empty)
    SessionImportError.BACKUP_TOO_LARGE -> getString(Res.string.history_import_too_large)
    SessionImportError.INVALID_JSON -> getString(Res.string.history_import_invalid_json)
    SessionImportError.INVALID_DATA -> getString(Res.string.history_import_invalid_data)
    SessionImportError.TOO_MANY_SESSIONS -> getString(Res.string.history_import_too_many_sessions)
    SessionImportError.DUPLICATE_SESSION_IDS -> getString(Res.string.history_import_duplicate_ids)
    SessionImportError.INVALID_SESSION -> getString(
        Res.string.history_import_invalid_session,
        (failure.invalidSessionNumber ?: 0).toString(),
    )
}

private suspend fun export(
    exporter: Exporter,
    name: String,
    mimeType: String,
    content: String,
): String = when (val result = exporter.export(name, mimeType, content)) {
    is ExportResult.Success -> getString(Res.string.history_exported_to, result.destination)
    is ExportResult.Failure -> when (result.error) {
        ExportError.WRITE_FAILED -> getString(Res.string.history_export_write_failed)
        ExportError.PLATFORM_EXPORT_UNAVAILABLE -> getString(Res.string.history_export_unavailable)
        ExportError.USER_CANCELLED -> getString(Res.string.history_export_cancelled)
    }
}

private suspend fun share(
    shareService: ShareService,
    name: String,
    mimeType: String,
    content: String,
): String = when (val result = shareService.share(name, mimeType, content)) {
    ShareResult.Started -> getString(Res.string.history_share_started)
    is ShareResult.Failure -> when (result.error) {
        ShareError.PREPARE_FAILED -> getString(Res.string.history_share_failed)
        ShareError.PLATFORM_SHARE_UNAVAILABLE -> getString(Res.string.history_share_unavailable)
    }
}

@Composable
private fun SessionCard(
    session: StopwatchSession,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val stats = remember(session.laps) { LapStatistics.from(session.laps) }
    val fastest = stats.fastest?.let { DurationFormatter.formatNanos(it.splitNanos) } ?: "—"

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(session.name, fontWeight = FontWeight.SemiBold)
            Text(
                DurationFormatter.formatNanos(session.durationNanos),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    Res.string.history_session_summary,
                    session.laps.size.toString(),
                    fastest,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRename) {
                    Text(stringResource(Res.string.action_rename))
                }
                Button(onClick = onDelete) {
                    Text(stringResource(Res.string.action_delete))
                }
            }
        }
    }
}
