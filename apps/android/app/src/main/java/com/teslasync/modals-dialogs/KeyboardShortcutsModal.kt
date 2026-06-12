package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Data readiness branch rendered by [KeyboardShortcutsModal]. */
enum class KeyboardShortcutsModalState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

/** Shortcut visibility filter mirrored from the web tab list. */
enum class ShortcutFilterMode { ALL, GLOBAL, PAGE }

/** Shortcut scope mirrored from the shared shortcut registry. */
enum class ShortcutScope { GLOBAL, PAGE }

/** Keyboard shortcut definition supplied by the caller's registry projection. */
data class ShortcutDefinition(
    val id: String,
    val group: String,
    val description: String,
    val keys: List<String>,
    val scope: ShortcutScope,
    val routePrefix: String? = null,
)

/**
 * Native Android parity for `web/src/components/feedback/KeyboardShortcutsModal.tsx`.
 *
 * Renders a searchable keyboard shortcut cheat sheet with All / Global / This
 * page filters, route-aware page shortcut filtering, priority group sorting,
 * empty results, and loading / error / stale / offline branches.
 */
@Composable
fun KeyboardShortcutsModal(
    open: Boolean,
    shortcuts: List<ShortcutDefinition>,
    currentRoute: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: KeyboardShortcutsModalState = KeyboardShortcutsModalState.CONTENT,
    initialFilter: ShortcutFilterMode = ShortcutFilterMode.ALL,
    onFilterChange: (ShortcutFilterMode) -> Unit = {},
    title: String = "Keyboard Shortcuts",
    searchLabel: String = "Search shortcuts…",
    allLabel: String = "All",
    globalLabel: String = "Global",
    pageLabel: String = "This page",
    emptyLabel: String = "No shortcuts match your search.",
    loadingLabel: String = "Loading shortcuts…",
    errorLabel: String? = null,
    retryLabel: String = "Retry",
    staleLabel: String = "Shortcuts may be out of date.",
    offlineLabel: String = "Offline; showing cached shortcuts.",
    dialogContentDescription: String = title,
    onRetry: (() -> Unit)? = null,
) {
    if (!open) return

    var search by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(initialFilter) }

    LaunchedEffect(open) {
        if (!open) search = ""
    }

    fun setFilter(next: ShortcutFilterMode) {
        mode = next
        onFilterChange(next)
    }

    val groups = remember(shortcuts, search, mode, currentRoute) {
        val needle = search.trim().lowercase()
        shortcuts
            .filter { shortcut ->
                val scopeVisible = when (mode) {
                    ShortcutFilterMode.ALL -> true
                    ShortcutFilterMode.GLOBAL -> shortcut.scope == ShortcutScope.GLOBAL
                    ShortcutFilterMode.PAGE -> shortcut.scope != ShortcutScope.GLOBAL
                }
                if (!scopeVisible) return@filter false
                if (shortcut.scope != ShortcutScope.GLOBAL) {
                    val routePrefix = shortcut.routePrefix ?: return@filter false
                    if (!currentRoute.startsWith(routePrefix)) return@filter false
                }
                if (needle.isNotBlank() && !shortcut.description.lowercase().contains(needle)) return@filter false
                true
            }
            .groupBy { it.group }
            .map { (group, items) -> ShortcutGroup(group, items.sortedBy { it.id }) }
            .sortedWith(compareByDescending<ShortcutGroup> { groupRank(it.title) }.thenBy { it.title })
    }

    Dialog(onDismissRequest = onClose) {
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
                    KeyboardShortcutsModalState.LOADING -> ShortcutsStatusMessage(loadingLabel, showSpinner = true)
                    KeyboardShortcutsModalState.EMPTY -> ShortcutsStatusMessage(emptyLabel)
                    KeyboardShortcutsModalState.ERROR -> ShortcutsStatusMessage(
                        message = errorLabel ?: emptyLabel,
                        isError = true,
                        actionLabel = retryLabel,
                        onAction = onRetry,
                    )
                    KeyboardShortcutsModalState.STALE -> {
                        ShortcutsStatusChip(staleLabel)
                        ShortcutContent(search, { search = it }, searchLabel, mode, ::setFilter, allLabel, globalLabel, pageLabel, groups, emptyLabel)
                    }
                    KeyboardShortcutsModalState.OFFLINE -> {
                        ShortcutsStatusChip(offlineLabel)
                        ShortcutContent(search, { search = it }, searchLabel, mode, ::setFilter, allLabel, globalLabel, pageLabel, groups, emptyLabel)
                    }
                    KeyboardShortcutsModalState.CONTENT -> ShortcutContent(
                        search = search,
                        onSearchChange = { search = it },
                        searchLabel = searchLabel,
                        mode = mode,
                        onModeChange = ::setFilter,
                        allLabel = allLabel,
                        globalLabel = globalLabel,
                        pageLabel = pageLabel,
                        groups = groups,
                        emptyLabel = emptyLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutContent(
    search: String,
    onSearchChange: (String) -> Unit,
    searchLabel: String,
    mode: ShortcutFilterMode,
    onModeChange: (ShortcutFilterMode) -> Unit,
    allLabel: String,
    globalLabel: String,
    pageLabel: String,
    groups: List<ShortcutGroup>,
    emptyLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = search,
            onValueChange = onSearchChange,
            label = { Text(searchLabel) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = mode == ShortcutFilterMode.ALL, onClick = { onModeChange(ShortcutFilterMode.ALL) }, label = { Text(allLabel) })
            FilterChip(selected = mode == ShortcutFilterMode.GLOBAL, onClick = { onModeChange(ShortcutFilterMode.GLOBAL) }, label = { Text(globalLabel) })
            FilterChip(selected = mode == ShortcutFilterMode.PAGE, onClick = { onModeChange(ShortcutFilterMode.PAGE) }, label = { Text(pageLabel) })
        }
        Column(
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (groups.isEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    text = emptyLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                groups.forEach { group -> ShortcutGroupView(group) }
            }
        }
    }
}

@Composable
private fun ShortcutGroupView(group: ShortcutGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.shortcuts.forEach { shortcut ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        text = shortcut.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        shortcut.keys.forEachIndexed { index, key ->
                            if (index > 0) Text(text = "+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            KeyCap(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCap(key: String) {
    Surface(shape = RoundedCornerShape(6.dp), tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = key,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShortcutsStatusChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ShortcutsStatusMessage(
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

private data class ShortcutGroup(
    val title: String,
    val shortcuts: List<ShortcutDefinition>,
)

private fun groupRank(label: String): Int {
    val key = label.lowercase().split(Regex("\\s|\\(")).firstOrNull().orEmpty()
    return when (key) {
        "navigation" -> 100
        "actions" -> 90
        "global" -> 90
        "commands" -> 80
        "table" -> 70
        "bulk" -> 60
        "form" -> 50
        "chart" -> 40
        "dashboard" -> 30
        "replay" -> 20
        else -> 0
    }
}
