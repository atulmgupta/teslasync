package com.teslasync.modalsdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp

/** State branch rendered by ExportModal. */
enum class ExportModalState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

data class ExportDashboardSummary(val name: String, val widgetCount: Int, val updatedAtLabel: String, val jsonText: String, val shareUrl: String, val layoutCells: List<Boolean>)

/** Native parity modal for dashboard export actions: download, copy JSON, copy share URL, and URL length warning. */
@Composable
fun ExportModal(
    open: Boolean,
    dashboard: ExportDashboardSummary,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onCopyJson: (String) -> Unit,
    onCopyShareUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: ExportModalState = ExportModalState.CONTENT,
    onRetry: (() -> Unit)? = null,
    titleLabel: String = "Export Dashboard",
    widgetCountFormat: String = "%d widgets",
    updatedFormat: String = "Updated %s",
    downloadLabel: String = "Download JSON File",
    copyClipboardLabel: String = "Copy to Clipboard",
    copyShareUrlLabel: String = "Copy Shareable URL",
    closeLabel: String = "Close",
    loadingLabel: String = "Preparing dashboard export…",
    emptyLabel: String = "No dashboard layout is available to export.",
    errorLabel: String = "Could not prepare dashboard export.",
    retryLabel: String = "Retry",
    staleLabel: String = "Showing saved export data.",
    offlineLabel: String = "Offline — export data may be cached.",
    shareTooLongFormat: String = "Layout too large for URL sharing (%d chars). Use clipboard or file export instead.",
    layoutPreviewLabel: String = "Dashboard layout preview",
) {
    if (!open) return
    val sizeLabel = byteSizeLabel(dashboard.jsonText.length)
    val shareTooLong = dashboard.shareUrl.length > 2000
    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.widthIn(max = 560.dp), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Header(titleLabel, closeLabel, onClose)
                when (state) {
                    ExportModalState.LOADING -> Message(loadingLabel, true)
                    ExportModalState.EMPTY -> Message(emptyLabel)
                    ExportModalState.ERROR -> ErrorBlock(errorLabel, retryLabel, onRetry)
                    else -> {
                        if (state == ExportModalState.STALE) Banner(staleLabel)
                        if (state == ExportModalState.OFFLINE) Banner(offlineLabel)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            MiniGrid(dashboard.layoutCells, layoutPreviewLabel, Modifier.width(120.dp).height(88.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(dashboard.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip(onClick = {}, label = { Text(widgetCountFormat.format(dashboard.widgetCount)) }); AssistChip(onClick = {}, label = { Text(sizeLabel) }) }
                                Text(updatedFormat.format(dashboard.updatedAtLabel), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onDownload(); onClose() }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = downloadLabel }) { Text("⬇  $downloadLabel") }
                            OutlinedButton(onClick = { onCopyJson(dashboard.jsonText) }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = copyClipboardLabel }) { Text(copyClipboardLabel) }
                            OutlinedButton(onClick = { onCopyShareUrl(dashboard.shareUrl) }, enabled = !shareTooLong, modifier = Modifier.fillMaxWidth().semantics { contentDescription = copyShareUrlLabel }) { Text(copyShareUrlLabel) }
                        }
                        if (shareTooLong) ErrorBanner(shareTooLongFormat.format(dashboard.shareUrl.length))
                    }
                }
            }
        }
    }
}

private fun byteSizeLabel(bytes: Int): String = if (bytes < 1024) "$bytes B" else "${((bytes * 10) / 1024) / 10.0} KB"
@Composable private fun MiniGrid(cells: List<Boolean>, layoutPreviewLabel: String, modifier: Modifier) { Surface(modifier = modifier.semantics { contentDescription = layoutPreviewLabel }, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { repeat(3) { row -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(4) { col -> val active = cells.getOrNull(row * 4 + col) == true; Surface(Modifier.size(22.dp), color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp), content = {}) } } } } } }
@Composable private fun Header(title: String, closeLabel: String, onClose: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeLabel }) { Text(closeLabel) } } }
@Composable private fun Message(text: String, busy: Boolean = false) { Column(Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = text }, verticalArrangement = Arrangement.spacedBy(10.dp)) { if (busy) CircularProgressIndicator(); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ErrorBlock(text: String, retryLabel: String, onRetry: (() -> Unit)?) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text, color = MaterialTheme.colorScheme.error); if (onRetry != null) Button(onClick = onRetry) { Text(retryLabel) } } }
@Composable private fun Banner(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }
@Composable private fun ErrorBanner(text: String) { Surface(color = MaterialTheme.colorScheme.errorContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Text("⚠ $text", Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
