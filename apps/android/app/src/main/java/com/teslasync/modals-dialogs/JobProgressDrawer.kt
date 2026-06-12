package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Drawer visibility branch mirrored from the web persisted state. */
enum class JobProgressDrawerDisplayState { OPEN, MINIMIZED, DISMISSED }

/** Export job status mirrored from the web `ExportJobSummary.status`. */
enum class ExportJobStatus { QUEUED, PROCESSING, READY, FAILED, EXPIRED }

/** Export job row data rendered by [JobProgressDrawer]. */
data class ExportJobSummary(
    val id: String,
    val type: String,
    val format: String,
    val status: ExportJobStatus,
    val createdAtRelative: String,
    val completedAtRelative: String? = null,
    val fileSizeLabel: String? = null,
    val errorMessage: String? = null,
)

/**
 * Native Android parity for `web/src/components/feedback/JobProgressDrawer.tsx`.
 *
 * Renders the minimizable export job drawer with active/recent bucketing,
 * loading and empty section branches, ready download action, failed detail
 * affordance, stale/offline chips, and dismiss/minimize controls.
 */
@Composable
fun JobProgressDrawer(
    jobs: List<ExportJobSummary>,
    displayState: JobProgressDrawerDisplayState,
    onDisplayStateChange: (JobProgressDrawerDisplayState) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    maxRecent: Int = 5,
    errorMessage: String? = null,
    isStale: Boolean = false,
    isOffline: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onDownload: (ExportJobSummary) -> Unit = {},
    onFailedDetails: (ExportJobSummary) -> Unit = {},
    failedDetailsLabel: String = "View failure details",
    expandLabel: String = "Show export jobs (${jobs.count { it.isActive() }} active)",
    activeCountLabel: String = "${jobs.count { it.isActive() }} export running",
    recentChipLabel: String = "Exports",
    regionLabel: String = "Export job progress",
    title: String = "Export jobs",
    activePillLabel: String = "${jobs.count { it.isActive() }} active",
    minimizeLabel: String = "Minimize",
    dismissLabel: String = "Dismiss",
    loadingLabel: String = "Loading export jobs…",
    activeHeading: String = "In progress",
    activeEmptyLabel: String = "No active exports",
    recentHeading: String = "Recent",
    recentEmptyLabel: String = "No recent exports",
    startedLine: (status: String, relative: String) -> String = { status, relative -> "$status · started $relative" },
    completedLine: (size: String, relative: String) -> String = { size, relative -> "$size · $relative" },
    downloadLabel: String = "Download",
    retryLabel: String = "Retry",
    staleLabel: String = "Export jobs may be out of date.",
    offlineLabel: String = "Offline; showing cached export jobs.",
    emptyFileSizeLabel: String = "—",
    typeLabels: Map<String, String> = defaultExportTypeLabels(),
    statusLabels: Map<ExportJobStatus, String> = defaultExportStatusLabels(),
) {
    val activeJobs = jobs.filter { it.isActive() }
    val recentJobs = jobs.filterNot { it.isActive() }.take(maxRecent)

    if (displayState == JobProgressDrawerDisplayState.DISMISSED && activeJobs.isEmpty()) return
    if (jobs.isEmpty() && !isLoading && errorMessage == null) return

    if (displayState == JobProgressDrawerDisplayState.MINIMIZED) {
        Surface(
            modifier = modifier
                .semantics {
                    role = Role.Button
                    contentDescription = expandLabel
                }
                .clickable { onDisplayStateChange(JobProgressDrawerDisplayState.OPEN) },
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (activeJobs.isNotEmpty()) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else StatusDot(Color(0xFF34D399))
                Text(text = if (activeJobs.isNotEmpty()) activeCountLabel else recentChipLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
        return
    }

    Surface(
        modifier = modifier
            .widthIn(max = 420.dp)
            .semantics { contentDescription = regionLabel },
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(Color(0xFF22D3EE))
                    Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (activeJobs.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                text = activePillLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { onDisplayStateChange(JobProgressDrawerDisplayState.MINIMIZED) }, modifier = Modifier.semantics { contentDescription = minimizeLabel }) {
                        Text(text = "−", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { onDisplayStateChange(JobProgressDrawerDisplayState.DISMISSED) }, modifier = Modifier.semantics { contentDescription = dismissLabel }) {
                        Text(text = "×", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isStale) DrawerStatusChip(staleLabel)
                if (isOffline) DrawerStatusChip(offlineLabel)
                when {
                    errorMessage != null && jobs.isEmpty() -> DrawerStatusMessage(errorMessage, retryLabel, onRetry)
                    isLoading && jobs.isEmpty() -> DrawerLoading(loadingLabel)
                    else -> {
                        DrawerSection(
                            label = activeHeading,
                            emptyLabel = activeEmptyLabel,
                            jobs = activeJobs,
                            startedLine = startedLine,
                            completedLine = completedLine,
                            downloadLabel = downloadLabel,
                            emptyFileSizeLabel = emptyFileSizeLabel,
                            typeLabels = typeLabels,
                            statusLabels = statusLabels,
                            onDownload = onDownload,
                            onFailedDetails = onFailedDetails,
                            failedDetailsLabel = failedDetailsLabel,
                        )
                        DrawerSection(
                            label = recentHeading,
                            emptyLabel = recentEmptyLabel,
                            jobs = recentJobs,
                            startedLine = startedLine,
                            completedLine = completedLine,
                            downloadLabel = downloadLabel,
                            emptyFileSizeLabel = emptyFileSizeLabel,
                            typeLabels = typeLabels,
                            statusLabels = statusLabels,
                            onDownload = onDownload,
                            onFailedDetails = onFailedDetails,
                            failedDetailsLabel = failedDetailsLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSection(
    label: String,
    emptyLabel: String,
    jobs: List<ExportJobSummary>,
    startedLine: (status: String, relative: String) -> String,
    completedLine: (size: String, relative: String) -> String,
    downloadLabel: String,
    emptyFileSizeLabel: String,
    typeLabels: Map<String, String>,
    statusLabels: Map<ExportJobStatus, String>,
    onDownload: (ExportJobSummary) -> Unit,
    onFailedDetails: (ExportJobSummary) -> Unit,
    failedDetailsLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (jobs.isEmpty()) {
            Text(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), text = emptyLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            jobs.forEach { job ->
                JobRow(
                    job = job,
                    startedLine = startedLine,
                    completedLine = completedLine,
                    downloadLabel = downloadLabel,
                    emptyFileSizeLabel = emptyFileSizeLabel,
                    typeLabels = typeLabels,
                    statusLabels = statusLabels,
                    onDownload = onDownload,
                    onFailedDetails = onFailedDetails,
                    failedDetailsLabel = failedDetailsLabel,
                )
            }
        }
    }
}

@Composable
private fun JobRow(
    job: ExportJobSummary,
    startedLine: (status: String, relative: String) -> String,
    completedLine: (size: String, relative: String) -> String,
    downloadLabel: String,
    emptyFileSizeLabel: String,
    typeLabels: Map<String, String>,
    statusLabels: Map<ExportJobStatus, String>,
    onDownload: (ExportJobSummary) -> Unit,
    onFailedDetails: (ExportJobSummary) -> Unit,
    failedDetailsLabel: String,
) {
    val statusLabel = statusLabels[job.status] ?: job.status.name.lowercase()
    val line = if (job.isActive()) {
        startedLine(statusLabel, job.createdAtRelative)
    } else {
        completedLine(job.fileSizeLabel?.ifBlank { emptyFileSizeLabel } ?: emptyFileSizeLabel, job.completedAtRelative ?: job.createdAtRelative)
    }

    Surface(shape = RoundedCornerShape(12.dp), color = if (job.isActive()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (job.status == ExportJobStatus.PROCESSING) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else StatusDot(statusColor(job.status))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = typeLabels[job.type] ?: job.type, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = job.format.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = line, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!job.errorMessage.isNullOrBlank()) {
                    Text(text = job.errorMessage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (job.status == ExportJobStatus.READY) {
                Button(onClick = { onDownload(job) }) { Text(downloadLabel) }
            }
            if (job.status == ExportJobStatus.FAILED) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = failedDetailsLabel
                        }
                        .clickable { onFailedDetails(job) },
                    contentAlignment = Alignment.Center,
                ) { Text(text = "↗", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun DrawerLoading(label: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator()
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DrawerStatusMessage(message: String, retryLabel: String, onRetry: (() -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        if (onRetry != null) TextButton(onClick = onRetry) { Text(retryLabel) }
    }
}

@Composable
private fun DrawerStatusChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(999.dp)))
}

private fun ExportJobSummary.isActive(): Boolean = status == ExportJobStatus.QUEUED || status == ExportJobStatus.PROCESSING

private fun statusColor(status: ExportJobStatus): Color = when (status) {
    ExportJobStatus.QUEUED -> Color(0xFF94A3B8)
    ExportJobStatus.PROCESSING -> Color(0xFF22D3EE)
    ExportJobStatus.READY -> Color(0xFF34D399)
    ExportJobStatus.FAILED -> Color(0xFFFB7185)
    ExportJobStatus.EXPIRED -> Color(0xFFFBBF24)
}

private fun defaultExportTypeLabels(): Map<String, String> = mapOf(
    "account" to "Account export",
    "drives" to "Drives",
    "charging" to "Charging",
    "analytics" to "Analytics",
    "backup" to "Backup",
    "import_drives" to "Import drives",
    "import_charging" to "Import charging",
)

private fun defaultExportStatusLabels(): Map<ExportJobStatus, String> = mapOf(
    ExportJobStatus.QUEUED to "Queued",
    ExportJobStatus.PROCESSING to "Processing",
    ExportJobStatus.READY to "Ready",
    ExportJobStatus.FAILED to "Failed",
    ExportJobStatus.EXPIRED to "Expired",
)
