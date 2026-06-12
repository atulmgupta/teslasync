package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Widget categories shown by the catalogue, in the same order as the web registry. */
enum class WidgetCatalogueCategory { VEHICLE, BATTERY, ENERGY, CHARGING, DRIVING, CLIMATE, TIRES, SECURITY, COMMANDS, MEDIA, TELEMETRY, ANALYTICS, ALERTS, AUTOMATIONS, SYSTEM, MAPS }

/** Immutable catalogue entry supplied by the dashboard widget registry projection. */
data class WidgetCatalogueEntry(
    val id: String,
    val name: String,
    val description: String,
    val category: WidgetCatalogueCategory,
)

/** Native state wrapper for catalogue loading, cache and failure surfaces. */
data class WidgetCatalogueUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val stale: Boolean = false,
    val offline: Boolean = false,
)

/**
 * Native Android parity surface for the web `WidgetCatalogueDialog`.
 *
 * Lists every widget grouped by category, filters by query, disables already
 * active widgets, and closes immediately after a new widget is selected.
 */
@Composable
fun WidgetCatalogueDialog(
    open: Boolean,
    widgets: List<WidgetCatalogueEntry>,
    activeWidgetIds: List<String>,
    onAdd: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: WidgetCatalogueUiState = WidgetCatalogueUiState(),
    onRetry: (() -> Unit)? = null,
    titleText: String = "Widget catalogue",
    subtitleText: String = "Pick a widget to add to your dashboard. {added} of {total} widgets are already on your layout.",
    searchLabelText: String = "Search widgets",
    searchHintText: String = "Search widgets by name, description, or category…",
    resultCountText: String = "{count} of {total} widgets match",
    emptyTitleText: String = "No widgets match your search",
    emptyBodyText: String = "Try a different keyword, or clear the search to browse all {total} widgets.",
    emptyCatalogueText: String = "No widgets are available.",
    clearSearchText: String = "Clear search",
    addedText: String = "Added",
    addText: String = "Add",
    addLabelText: String = "Add {name} widget",
    staleText: String = "Stale",
    offlineText: String = "Offline",
    loadingText: String = "Loading widget catalogue",
    retryText: String = "Retry",
    closeText: String = "Close",
    categoryLabels: Map<WidgetCatalogueCategory, String> = defaultCategoryLabels(),
    categoryEmoji: Map<WidgetCatalogueCategory, String> = defaultCategoryEmoji(),
) {
    if (!open) return
    val activeSet = remember(activeWidgetIds) { activeWidgetIds.toSet() }
    var query by remember(open) { mutableStateOf("") }
    val trimmed = query.trim().lowercase()
    val grouped = remember(widgets) { widgets.groupBy { it.category }.toSortedMap(compareBy { it.ordinal }) }
    val filtered = remember(grouped, trimmed, categoryLabels) {
        if (trimmed.isEmpty()) grouped else grouped.mapValues { (category, items) ->
            val categoryHit = (categoryLabels[category] ?: category.name).lowercase().contains(trimmed)
            items.filter { categoryHit || listOf(it.id, it.name, it.description).joinToString(" ").lowercase().contains(trimmed) }
        }.filterValues { it.isNotEmpty() }
    }
    val visibleCount = filtered.values.sumOf { it.size }

    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.semantics { contentDescription = titleText }, shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(titleText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onClose) { Text(closeText) }
                }
                StateChip(state, staleText, offlineText)
                when {
                    state.loading -> LoadingBlock(loadingText)
                    state.errorMessage != null -> ErrorBlock(state.errorMessage, retryText, onRetry)
                    widgets.isEmpty() -> EmptyCatalogue(emptyCatalogueText)
                    else -> Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            subtitleText.replace("{added}", activeSet.size.toString()).replace("{total}", widgets.size.toString()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchLabelText },
                            label = { Text(searchLabelText) },
                            supportingText = { Text(searchHintText) },
                            singleLine = true,
                        )
                        if (trimmed.isNotEmpty()) {
                            Text(
                                resultCountText.replace("{count}", visibleCount.toString()).replace("{total}", widgets.size.toString()),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        if (trimmed.isNotEmpty() && visibleCount == 0) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(emptyTitleText, fontWeight = FontWeight.Medium)
                                    Text(emptyBodyText.replace("{total}", widgets.size.toString()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { query = "" }) { Text(clearSearchText) }
                                }
                            }
                        } else {
                            filtered.forEach { (category, entries) ->
                                CategorySection(
                                    category = category,
                                    entries = entries,
                                    activeSet = activeSet,
                                    onAdd = { id -> onAdd(id); onClose() },
                                    categoryLabel = categoryLabels[category] ?: category.name,
                                    emoji = categoryEmoji[category] ?: "•",
                                    addedText = addedText,
                                    addText = addText,
                                    addLabelText = addLabelText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    category: WidgetCatalogueCategory,
    entries: List<WidgetCatalogueEntry>,
    activeSet: Set<String>,
    onAdd: (String) -> Unit,
    categoryLabel: String,
    emoji: String,
    addedText: String,
    addText: String,
    addLabelText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji)
            Text(categoryLabel.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("(${entries.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        entries.forEach { widget ->
            val isAdded = widget.id in activeSet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Text(emoji, modifier = Modifier.semantics { contentDescription = category.name.lowercase() })
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(widget.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isAdded) AssistChip(onClick = {}, label = { Text(addedText) })
                    }
                    Text(widget.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    enabled = !isAdded,
                    onClick = { onAdd(widget.id) },
                    modifier = Modifier.semantics { contentDescription = addLabelText.replace("{name}", widget.name) },
                ) { Text(if (isAdded) addedText else addText) }
            }
        }
    }
}

@Composable
private fun StateChip(state: WidgetCatalogueUiState, stale: String, offline: String) {
    val label = when {
        state.offline -> offline
        state.stale -> stale
        else -> null
    } ?: return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LoadingBlock(text: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorBlock(message: String, retry: String, onRetry: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) TextButton(onClick = onRetry) { Text(retry) }
    }
}

@Composable
private fun EmptyCatalogue(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), shape = RoundedCornerShape(16.dp)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun defaultCategoryLabels() = mapOf(
    WidgetCatalogueCategory.VEHICLE to "Vehicle",
    WidgetCatalogueCategory.BATTERY to "Battery & Range",
    WidgetCatalogueCategory.ENERGY to "Energy",
    WidgetCatalogueCategory.CHARGING to "Charging",
    WidgetCatalogueCategory.DRIVING to "Driving",
    WidgetCatalogueCategory.CLIMATE to "Climate",
    WidgetCatalogueCategory.TIRES to "Tires",
    WidgetCatalogueCategory.SECURITY to "Security",
    WidgetCatalogueCategory.COMMANDS to "Commands",
    WidgetCatalogueCategory.MEDIA to "Media",
    WidgetCatalogueCategory.TELEMETRY to "Telemetry",
    WidgetCatalogueCategory.ANALYTICS to "Analytics",
    WidgetCatalogueCategory.ALERTS to "Alerts",
    WidgetCatalogueCategory.AUTOMATIONS to "Automations",
    WidgetCatalogueCategory.SYSTEM to "System",
    WidgetCatalogueCategory.MAPS to "Maps",
)

private fun defaultCategoryEmoji() = mapOf(
    WidgetCatalogueCategory.VEHICLE to "🚗",
    WidgetCatalogueCategory.BATTERY to "🔋",
    WidgetCatalogueCategory.ENERGY to "⚡",
    WidgetCatalogueCategory.CHARGING to "🔌",
    WidgetCatalogueCategory.DRIVING to "🛣",
    WidgetCatalogueCategory.CLIMATE to "🌡",
    WidgetCatalogueCategory.TIRES to "🛞",
    WidgetCatalogueCategory.SECURITY to "🛡",
    WidgetCatalogueCategory.COMMANDS to "🎛",
    WidgetCatalogueCategory.MEDIA to "🎵",
    WidgetCatalogueCategory.TELEMETRY to "📡",
    WidgetCatalogueCategory.ANALYTICS to "📊",
    WidgetCatalogueCategory.ALERTS to "🔔",
    WidgetCatalogueCategory.AUTOMATIONS to "🤖",
    WidgetCatalogueCategory.SYSTEM to "⚙",
    WidgetCatalogueCategory.MAPS to "🗺",
)
