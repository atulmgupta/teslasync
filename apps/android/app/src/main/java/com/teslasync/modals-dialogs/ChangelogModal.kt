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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Data readiness branch rendered by [ChangelogModal]. */
enum class ChangelogModalState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

/** Changelog change type, preserving the web section order and labels. */
enum class ChangelogChangeType { ADDED, CHANGED, FIXED, REMOVED, DEPRECATED, SECURITY }

/** Badge value emitted by the changelog generator. */
enum class ChangelogBadge { LATEST, STABLE, BETA }

/** Single bullet inside a changelog section. */
data class ChangelogChange(
    val type: ChangelogChangeType,
    val text: String,
)

/** Single release entry shown in the changelog modal. */
data class ChangelogEntry(
    val version: String,
    val date: String,
    val badge: ChangelogBadge,
    val changes: List<ChangelogChange>,
)

/**
 * Native Android parity for `web/src/components/feedback/ChangelogModal.tsx`.
 *
 * Shows first-visit or since-last-visit copy, renders unseen entries when
 * present (otherwise full history), keeps the first two entries expanded, and
 * exposes Got it / View full changelog actions through caller callbacks.
 */
@Composable
fun ChangelogModal(
    open: Boolean,
    entries: List<ChangelogEntry>,
    newEntries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
    onGotIt: () -> Unit,
    onViewFull: () -> Unit,
    modifier: Modifier = Modifier,
    state: ChangelogModalState = ChangelogModalState.CONTENT,
    title: String = "What's new in TeslaSync",
    firstVisitSubtitle: String = "Welcome! Here's a quick tour of what TeslaSync ships with right now.",
    sinceLastVisitSubtitle: String = "${newEntries.ifEmpty { entries }.size} new release(s) since your last visit.",
    viewFullLabel: String = "View full changelog",
    gotItLabel: String = "Got it",
    loadingLabel: String = "Loading changelog…",
    emptyLabel: String = "No changelog entries are available.",
    errorLabel: String? = null,
    retryLabel: String = "Retry",
    staleLabel: String = "Changelog may be out of date.",
    offlineLabel: String = "Offline; showing cached changelog.",
    expandedContentDescription: String = "Collapse release notes",
    collapsedContentDescription: String = "Expand release notes",
    dialogContentDescription: String = title,
    sectionLabels: Map<ChangelogChangeType, String> = defaultSectionLabels(),
    badgeLabels: Map<ChangelogBadge, String> = defaultBadgeLabels(),
    onRetry: (() -> Unit)? = null,
) {
    if (!open) return

    val visibleEntries = if (newEntries.isNotEmpty()) newEntries else entries
    val isFirstVisit = newEntries.size == entries.size

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .widthIn(max = 640.dp)
                .semantics { contentDescription = dialogContentDescription },
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)

                when (state) {
                    ChangelogModalState.LOADING -> ChangelogStatusMessage(loadingLabel, showSpinner = true)
                    ChangelogModalState.EMPTY -> ChangelogStatusMessage(emptyLabel)
                    ChangelogModalState.ERROR -> ChangelogStatusMessage(
                        message = errorLabel ?: emptyLabel,
                        isError = true,
                        actionLabel = retryLabel,
                        onAction = onRetry,
                    )
                    ChangelogModalState.STALE -> {
                        ChangelogStatusChip(staleLabel)
                        ChangelogContent(
                            visibleEntries,
                            isFirstVisit,
                            firstVisitSubtitle,
                            sinceLastVisitSubtitle,
                            sectionLabels,
                            badgeLabels,
                            expandedContentDescription,
                            collapsedContentDescription,
                            emptyLabel,
                        )
                    }
                    ChangelogModalState.OFFLINE -> {
                        ChangelogStatusChip(offlineLabel)
                        ChangelogContent(
                            visibleEntries,
                            isFirstVisit,
                            firstVisitSubtitle,
                            sinceLastVisitSubtitle,
                            sectionLabels,
                            badgeLabels,
                            expandedContentDescription,
                            collapsedContentDescription,
                            emptyLabel,
                        )
                    }
                    ChangelogModalState.CONTENT -> ChangelogContent(
                        visibleEntries,
                        isFirstVisit,
                        firstVisitSubtitle,
                        sinceLastVisitSubtitle,
                        sectionLabels,
                        badgeLabels,
                        expandedContentDescription,
                        collapsedContentDescription,
                        emptyLabel,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onViewFull) { Text(viewFullLabel) }
                    Button(modifier = Modifier.padding(start = 8.dp), onClick = onGotIt) { Text(gotItLabel) }
                }
            }
        }
    }
}

@Composable
private fun ChangelogContent(
    visibleEntries: List<ChangelogEntry>,
    isFirstVisit: Boolean,
    firstVisitSubtitle: String,
    sinceLastVisitSubtitle: String,
    sectionLabels: Map<ChangelogChangeType, String>,
    badgeLabels: Map<ChangelogBadge, String>,
    expandedContentDescription: String,
    collapsedContentDescription: String,
    emptyLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isFirstVisit) firstVisitSubtitle else sinceLastVisitSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (visibleEntries.isEmpty()) {
            ChangelogStatusMessage(emptyLabel)
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                visibleEntries.forEachIndexed { index, entry ->
                    ChangelogEntryCard(
                        entry = entry,
                        defaultOpen = index < 2,
                        sectionLabels = sectionLabels,
                        badgeLabels = badgeLabels,
                        expandedContentDescription = expandedContentDescription,
                        collapsedContentDescription = collapsedContentDescription,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangelogEntryCard(
    entry: ChangelogEntry,
    defaultOpen: Boolean,
    sectionLabels: Map<ChangelogChangeType, String>,
    badgeLabels: Map<ChangelogBadge, String>,
    expandedContentDescription: String,
    collapsedContentDescription: String,
) {
    var expanded by remember(entry.version) { mutableStateOf(defaultOpen) }
    val grouped = ChangelogChangeType.entries.mapNotNull { type ->
        val items = entry.changes.filter { it.type == type }
        if (items.isEmpty()) null else type to items
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .semantics {
                        role = Role.Button
                        contentDescription = if (expanded) expandedContentDescription else collapsedContentDescription
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "v${entry.version}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                    BadgeSurface(entry.badge, badgeLabels[entry.badge].orEmpty())
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(text = if (expanded) "▾" else "▸", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    grouped.forEach { (type, items) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = sectionLabels[type].orEmpty().uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                            items.forEach { item -> ChangeBullet(type, item.text) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeSurface(badge: ChangelogBadge, label: String) {
    val colors = when (badge) {
        ChangelogBadge.LATEST -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ChangelogBadge.STABLE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ChangelogBadge.BETA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(shape = RoundedCornerShape(999.dp), color = colors.first, contentColor = colors.second) {
        Text(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChangeBullet(type: ChangelogChangeType, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .background(sectionDotColor(type), RoundedCornerShape(999.dp)),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChangelogStatusChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChangelogStatusMessage(
    message: String,
    showSpinner: Boolean = false,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showSpinner) CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

private fun defaultSectionLabels(): Map<ChangelogChangeType, String> = mapOf(
    ChangelogChangeType.ADDED to "Added",
    ChangelogChangeType.CHANGED to "Changed",
    ChangelogChangeType.FIXED to "Fixed",
    ChangelogChangeType.REMOVED to "Removed",
    ChangelogChangeType.DEPRECATED to "Deprecated",
    ChangelogChangeType.SECURITY to "Security",
)

private fun defaultBadgeLabels(): Map<ChangelogBadge, String> = mapOf(
    ChangelogBadge.LATEST to "Latest",
    ChangelogBadge.STABLE to "Stable",
    ChangelogBadge.BETA to "Beta",
)

private fun sectionDotColor(type: ChangelogChangeType): Color = when (type) {
    ChangelogChangeType.ADDED -> Color(0xFF34D399)
    ChangelogChangeType.CHANGED -> Color(0xFF22D3EE)
    ChangelogChangeType.FIXED -> Color(0xFFFBBF24)
    ChangelogChangeType.REMOVED -> Color(0xFFFB7185)
    ChangelogChangeType.DEPRECATED -> Color(0xFFC084FC)
    ChangelogChangeType.SECURITY -> Color(0xFFFB7185)
}
