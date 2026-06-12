package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Severity for notification preview rows, mirroring web alert severity. */
enum class NotificationSeverity { INFO, WARN, CRITICAL }

/** One unread notification preview row for [NotificationBellPopover]. */
data class NotificationPreview(
    val id: String,
    val title: String?,
    val message: String? = null,
    val createdAtLabel: String,
    val vehicleName: String? = null,
    val vehicleId: Long? = null,
    val severity: NotificationSeverity = NotificationSeverity.INFO,
)

/** Data status for the popover body. */
enum class NotificationPopoverState { CONTENT, LOADING, ERROR, STALE, OFFLINE }

/**
 * Native parity surface for `web/src/components/layout/NotificationBellPopover.tsx`.
 *
 * Renders a bell trigger with unread badge. On compact/mobile callers can route
 * directly via [onViewAll]; otherwise the trigger opens a non-modal triage
 * panel with loading, error, empty and content branches plus mark-all-read and
 * view-all footer actions.
 */
@Composable
fun NotificationBellPopover(
    unreadCount: Int,
    notifications: List<NotificationPreview>,
    onViewAll: () -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
    state: NotificationPopoverState = NotificationPopoverState.CONTENT,
    isMobile: Boolean = false,
    markAllPending: Boolean = false,
    onRetry: (() -> Unit)? = null,
    title: String = "Notifications",
    unreadNotificationsTemplate: String = "%d unread notifications",
    unreadCountTemplate: String = "%d unread",
    allReadLabel: String = "All caught up",
    closeLabel: String = "Close",
    loadingLabel: String = "Loading…",
    errorLabel: String = "Could not load notifications",
    emptyTitle: String = "You're all caught up",
    emptyMessage: String = "No unread notifications right now.",
    untitledLabel: String = "Notification",
    markAllReadLabel: String = "Mark all read",
    viewAllLabel: String = "View all",
    retryLabel: String = "Retry",
    staleLabel: String = "Notifications may be out of date.",
    offlineLabel: String = "Offline — cached notifications shown.",
    vehicleFallbackTemplate: String = "#%d",
    onNotificationClick: (NotificationPreview) -> Unit = { onViewAll() },
) {
    var open by remember { mutableStateOf(false) }
    val triggerLabel = if (unreadCount > 0) unreadNotificationsTemplate.format(unreadCount) else title
    Box(modifier = modifier) {
        IconButton(
            onClick = {
                if (isMobile) onViewAll() else open = !open
            },
            modifier = Modifier.semantics { contentDescription = triggerLabel },
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Text("🔔", style = MaterialTheme.typography.titleMedium)
                if (unreadCount > 0) {
                    Surface(color = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError, shape = CircleShape) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            modifier = Modifier.padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = null,
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (unreadCount > 0) unreadCountTemplate.format(unreadCount) else allReadLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        IconButton(onClick = { open = false }, modifier = Modifier.semantics { contentDescription = closeLabel }) { Text("×") }
                    }
                    HorizontalDivider()
                    NotificationBody(
                        notifications = notifications,
                        state = state,
                        loadingLabel = loadingLabel,
                        errorLabel = errorLabel,
                        emptyTitle = emptyTitle,
                        emptyMessage = emptyMessage,
                        untitledLabel = untitledLabel,
                        staleLabel = staleLabel,
                        offlineLabel = offlineLabel,
                        retryLabel = retryLabel,
                        onRetry = onRetry,
                        vehicleFallbackTemplate = vehicleFallbackTemplate,
                        onNotificationClick = {
                            open = false
                            onNotificationClick(it)
                        },
                    )
                    HorizontalDivider()
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onMarkAllRead,
                    enabled = notifications.isNotEmpty() && !markAllPending,
                ) { Text(markAllReadLabel) }
            },
            confirmButton = {
                Button(onClick = { open = false; onViewAll() }) { Text(viewAllLabel) }
            },
        )
    }
}

@Composable
private fun NotificationBody(
    notifications: List<NotificationPreview>,
    state: NotificationPopoverState,
    loadingLabel: String,
    errorLabel: String,
    emptyTitle: String,
    emptyMessage: String,
    untitledLabel: String,
    staleLabel: String,
    offlineLabel: String,
    retryLabel: String,
    onRetry: (() -> Unit)?,
    vehicleFallbackTemplate: String,
    onNotificationClick: (NotificationPreview) -> Unit,
) {
    val hasLogs = notifications.isNotEmpty()
    val showSpinner = state == NotificationPopoverState.LOADING && !hasLogs
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(vertical = 12.dp)) {
        when {
            showSpinner -> Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(loadingLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            state == NotificationPopoverState.ERROR && !hasLogs -> Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = errorLabel },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(errorLabel, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (onRetry != null) OutlinedButton(onClick = onRetry) { Text(retryLabel) }
            }
            !hasLogs -> Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(emptyTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            else -> {
                if (state == NotificationPopoverState.STALE || state == NotificationPopoverState.OFFLINE) {
                    Text(
                        if (state == NotificationPopoverState.STALE) staleLabel else offlineLabel,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                LazyColumn {
                    items(notifications.take(10), key = { it.id }) { item ->
                        NotificationRow(item, untitledLabel, vehicleFallbackTemplate, onNotificationClick)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationPreview, untitledLabel: String, vehicleFallbackTemplate: String, onClick: (NotificationPreview) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(item) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(severityColor(item.severity))
                .semantics { contentDescription = item.severity.name.lowercase() },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title?.takeIf { it.isNotBlank() } ?: untitledLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!item.message.isNullOrBlank()) {
                Text(item.message, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.createdAtLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                val vehicle = item.vehicleName ?: item.vehicleId?.let { vehicleFallbackTemplate.format(it) }
                if (vehicle != null) Text("· $vehicle", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun severityColor(severity: NotificationSeverity): Color = when (severity) {
    NotificationSeverity.INFO -> Color(0xFF38BDF8)
    NotificationSeverity.WARN -> Color(0xFFFBBF24)
    NotificationSeverity.CRITICAL -> Color(0xFFF43F5E)
}
