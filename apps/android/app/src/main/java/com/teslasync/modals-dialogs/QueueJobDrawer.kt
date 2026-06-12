package com.teslasync.modalsdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp

/** State branch rendered by QueueJobDrawer. */
enum class QueueJobDrawerState { LOADING, ERROR, EMPTY, CONTENT, STALE, OFFLINE }

data class QueueJobDrawerItem(
    val id: String,
    val title: String?,
    val status: String,
    val startedAtLabel: String,
    val durationLabel: String? = null,
    val error: String? = null,
)

/** Native parity drawer for recent worker queue jobs, including loading, error, empty, stale, and offline states. */
@Composable
fun QueueJobDrawer(
    open: Boolean,
    worker: String?,
    displayName: String?,
    jobs: List<QueueJobDrawerItem>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: QueueJobDrawerState = QueueJobDrawerState.CONTENT,
    onRetry: (() -> Unit)? = null,
    titleWithWorkerFormat: String = "Recent %s jobs",
    titleLabel: String = "Recent jobs",
    closeLabel: String = "Close",
    loadingLabel: String = "Loading recent jobs…",
    errorLabel: String = "Could not load recent jobs. Check API logs and try again.",
    retryLabel: String = "Retry",
    emptyLabel: String = "No recent jobs to show. New jobs will appear here as the worker processes them.",
    staleLabel: String = "Showing cached jobs. Refresh to load recent activity.",
    offlineLabel: String = "Offline — showing cached jobs.",
    startedFormat: String = "Started %s",
    durationFormat: String = "Took %s",
    statusLabels: Map<String, String> = emptyMap(),
) {
    if (!open) return
    val title = displayName?.let { titleWithWorkerFormat.format(it) } ?: titleLabel
    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.widthIn(max = 560.dp), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Header(title, closeLabel, onClose)
                when (state) {
                    QueueJobDrawerState.LOADING -> Message(loadingLabel, busy = true)
                    QueueJobDrawerState.ERROR -> ErrorBlock(errorLabel, retryLabel, onRetry)
                    QueueJobDrawerState.EMPTY -> Message(emptyLabel)
                    QueueJobDrawerState.STALE, QueueJobDrawerState.OFFLINE, QueueJobDrawerState.CONTENT -> {
                        if (state == QueueJobDrawerState.STALE) Banner(staleLabel)
                        if (state == QueueJobDrawerState.OFFLINE) Banner(offlineLabel)
                        if (jobs.isEmpty()) Message(emptyLabel) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(jobs, key = { it.id }) { job -> JobRow(job, statusLabels, startedFormat, durationFormat) }
                        }
                    }
                }
                if (worker == null && state == QueueJobDrawerState.CONTENT) Text(emptyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun JobRow(job: QueueJobDrawerItem, statusLabels: Map<String, String>, startedFormat: String, durationFormat: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${job.title ?: job.id}, ${job.status}" },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(job.title ?: job.id, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(statusLabels[job.status] ?: job.status, color = statusColor(job.status), style = MaterialTheme.typography.labelMedium)
            }
            val duration = job.durationLabel?.let { " · ${durationFormat.format(it)}" }.orEmpty()
            Text(startedFormat.format(job.startedAtLabel) + duration, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (job.error != null) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) { Text("⚠ ${job.error}", Modifier.fillMaxWidth().padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun statusColor(status: String): Color = when (status) {
    "sent", "ready", "success" -> Color(0xFF34D399)
    "pending", "deferred_dnd", "queued", "partial" -> Color(0xFFFBBF24)
    "processing", "running" -> Color(0xFF67E8F9)
    "failed" -> Color(0xFFF87171)
    else -> Color.Unspecified
}

@Composable
private fun Header(title: String, closeLabel: String, onClose: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeLabel }) { Text(closeLabel) } } }
@Composable
private fun Message(text: String, busy: Boolean = false) { Column(Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = text }, verticalArrangement = Arrangement.spacedBy(10.dp)) { if (busy) CircularProgressIndicator(); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable
private fun ErrorBlock(text: String, retryLabel: String, onRetry: (() -> Unit)?) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text, color = MaterialTheme.colorScheme.error); if (onRetry != null) Button(onClick = onRetry) { Text(retryLabel) } } }
@Composable
private fun Banner(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }
