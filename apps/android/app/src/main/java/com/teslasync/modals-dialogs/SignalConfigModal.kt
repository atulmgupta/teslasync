package com.teslasync.modalsdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Signal category input shape, matching the web `CategoryDef`. */
data class SignalCategoryDef(val category: String, val fields: List<String>)

/** Selected signal output shape, matching the web submit payload. */
data class SignalSubscription(val name: String, val interval: Int)

/** Display copy for [SignalConfigModal], resolved by the caller's i18n layer. */
data class SignalConfigModalText(
    val title: String = "Fleet Telemetry Signal Configuration",
    val selectedSummary: String = "%d / %d signals selected",
    val selectedFooter: String = "%d signals selected",
    val atFastInterval: String = "%d at 500ms",
    val atDefaultInterval: String = "%d at 10s",
    val selectAll: String = "Select All",
    val deselectAll: String = "Deselect All",
    val masterInterval: String = "Master Interval:",
    val searchLabel: String = "Search signals",
    val setAll: String = "Set all...",
    val cancel: String = "Cancel",
    val subscribe: String = "Subscribe %d Signals",
    val emptySearch: String = "No signals match the current search.",
    val closeContentDescription: String = "Close",
)

/** Interval menu option text and value for [SignalConfigModal]. */
data class SignalIntervalOption(val value: Int, val label: String, val description: String)

/** Preset strategy matching the web preset matrix. */
enum class SignalPresetStrategy { REAL_TIME_DRIVING, BALANCED, LOW_POWER, TRACK_MODE, COST_SAVER, SLEEP_WATCH, DIAGNOSTICS, TRIP_LOGGER }

/** Preset display text plus strategy for [SignalConfigModal]. */
data class SignalPresetOption(val name: String, val description: String, val strategy: SignalPresetStrategy)

private data class SignalConfigRow(val name: String, val category: String, val selected: Boolean, val interval: Int)

private fun defaultSignalIntervalOptions() = listOf(
    SignalIntervalOption(0, "500ms", "Real-time"),
    SignalIntervalOption(1, "1s", "Fast"),
    SignalIntervalOption(5, "5s", "Medium"),
    SignalIntervalOption(10, "10s", "Default"),
    SignalIntervalOption(30, "30s", "Slow"),
    SignalIntervalOption(60, "60s", "1 min"),
    SignalIntervalOption(300, "5m", "Rare"),
    SignalIntervalOption(900, "15m", "15 min"),
    SignalIntervalOption(3600, "1h", "1 hour"),
    SignalIntervalOption(86400, "24h", "Daily"),
)

private fun defaultSignalPresetOptions() = listOf(
    SignalPresetOption("⚡ Real-time Driving", "Driving signals at 1s, battery at 10s, config at 24h", SignalPresetStrategy.REAL_TIME_DRIVING),
    SignalPresetOption("⚖️ Balanced", "All signals at 10s — good balance of data and battery", SignalPresetStrategy.BALANCED),
    SignalPresetOption("🔋 Low Power", "All signals at 60s — minimal battery impact", SignalPresetStrategy.LOW_POWER),
    SignalPresetOption("🏎️ Track Mode", "Driving & powertrain at 1s, everything else at 30s", SignalPresetStrategy.TRACK_MODE),
    SignalPresetOption("💰 Cost Saver", "Essential signals only at 5–15min, non-essentials off", SignalPresetStrategy.COST_SAVER),
    SignalPresetOption("😴 Sleep Watch", "Security & location at 60s, charging at 1min, rest off", SignalPresetStrategy.SLEEP_WATCH),
    SignalPresetOption("🔧 Diagnostics", "Powertrain/tires/climate at 5s, driving at 10s", SignalPresetStrategy.DIAGNOSTICS),
    SignalPresetOption("🗺️ Trip Logger", "Location at 1s, driving at 5s — optimized for routes", SignalPresetStrategy.TRIP_LOGGER),
)

/**
 * `SignalConfigModal` — native parity for the fleet telemetry signal picker.
 *
 * Builds local form state from immutable [categories], supports presets,
 * master/category/signal toggles, interval updates, search filtering,
 * expandable groups, and submits selected signal intervals without performing
 * networking in the view.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignalConfigModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    categories: List<SignalCategoryDef>,
    initialSelected: List<String>,
    initialInterval: Int,
    onSubmit: (List<SignalSubscription>) -> Unit,
    modifier: Modifier = Modifier,
    strings: SignalConfigModalText = SignalConfigModalText(),
    intervalOptions: List<SignalIntervalOption> = defaultSignalIntervalOptions(),
    presetOptions: List<SignalPresetOption> = defaultSignalPresetOptions(),
) {
    var signals by remember(categories, initialSelected, initialInterval) {
        mutableStateOf(categories.flatMap { cat ->
            cat.fields.map { field -> SignalConfigRow(field, cat.category, field in initialSelected, initialInterval) }
        })
    }
    var search by remember { mutableStateOf("") }
    var masterInterval by remember(initialInterval) { mutableStateOf(initialInterval) }
    var expanded by remember(categories) { mutableStateOf(categories.map { it.category }.toSet()) }

    val filtered = signals.filter { it.name.contains(search, ignoreCase = true) }
    val selectedCount = signals.count { it.selected }
    val totalCount = signals.size
    val allSelected = totalCount > 0 && selectedCount == totalCount
    val grouped = filtered.groupBy { it.category }

    Modal(
        visible = visible,
        onDismiss = onDismiss,
        title = strings.title,
        size = ModalSize.FULL,
        closeContentDescription = strings.closeContentDescription,
        modifier = modifier,
        footer = {
            SignalFooter(
                strings = strings,
                selectedCount = selectedCount,
                fastCount = signals.count { it.selected && it.interval == 0 },
                defaultCount = signals.count { it.selected && it.interval == 10 },
                onDismiss = onDismiss,
                onSubmit = {
                    onSubmit(signals.filter { it.selected }.map { SignalSubscription(it.name, it.interval) })
                    onDismiss()
                },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = strings.selectedSummary.format(selectedCount, totalCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presetOptions.forEach { preset ->
                    OutlinedButton(
                        onClick = { signals = signals.map { applyPreset(it, preset.strategy) } },
                        modifier = Modifier.semantics { contentDescription = preset.description },
                    ) { Text(preset.name, style = MaterialTheme.typography.labelSmall) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { signals = signals.map { it.copy(selected = !allSelected) } }) {
                    Text(if (allSelected) strings.deselectAll else strings.selectAll)
                }
                IntervalMenu(
                    label = strings.masterInterval,
                    selected = masterInterval,
                    options = intervalOptions,
                    showDescription = true,
                    onSelected = { value ->
                        masterInterval = value
                        signals = signals.map { it.copy(interval = value) }
                    },
                )
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(strings.searchLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (filtered.isEmpty()) {
                Text(
                    text = strings.emptySearch,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(grouped.entries.toList(), key = { it.key }) { (category, rows) ->
                        SignalCategoryCard(
                            category = category,
                            rows = rows,
                            expanded = category in expanded,
                            strings = strings,
                            onToggleExpanded = {
                                expanded = if (category in expanded) expanded - category else expanded + category
                            },
                            onToggleCategory = {
                                val allCatSelected = signals.filter { it.category == category }.all { it.selected }
                                signals = signals.map { if (it.category == category) it.copy(selected = !allCatSelected) else it }
                            },
                            onCategoryInterval = { interval ->
                                signals = signals.map { if (it.category == category) it.copy(interval = interval) else it }
                            },
                            onSignalToggle = { name ->
                                signals = signals.map { if (it.name == name) it.copy(selected = !it.selected) else it }
                            },
                            onSignalInterval = { name, interval ->
                                signals = signals.map { if (it.name == name) it.copy(interval = interval) else it }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalCategoryCard(
    category: String,
    rows: List<SignalConfigRow>,
    expanded: Boolean,
    strings: SignalConfigModalText,
    onToggleExpanded: () -> Unit,
    onToggleCategory: () -> Unit,
    onCategoryInterval: (Int) -> Unit,
    onSignalToggle: (String) -> Unit,
    onSignalInterval: (String, Int) -> Unit,
) {
    val selectedCount = rows.count { it.selected }
    val allSelected = rows.isNotEmpty() && selectedCount == rows.size
    val someSelected = selectedCount > 0
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggleExpanded)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (expanded) "⌄" else "›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                CheckBoxGlyph(selected = allSelected, partial = someSelected && !allSelected, onClick = onToggleCategory)
                Text(text = category.uppercase(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text(text = "($selectedCount/${rows.size})", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                IntervalMenu(label = strings.setAll, selected = null, options = intervalOptions, onSelected = onCategoryInterval)
            }
            if (expanded) {
                rows.forEachIndexed { index, row ->
                    if (index > 0) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (row.selected) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.34f))
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckBoxGlyph(selected = row.selected, partial = false, onClick = { onSignalToggle(row.name) })
                        Text(
                            text = row.name,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IntervalMenu(label = "", selected = row.interval, options = intervalOptions, onSelected = { onSignalInterval(row.name, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalMenu(
    label: String,
    selected: Int?,
    options: List<SignalIntervalOption>,
    showDescription: Boolean = false,
    onSelected: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selected }?.label ?: label
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.widthIn(min = 80.dp)) {
            Text(text = selectedLabel, style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (showDescription) "${option.label} (${option.description})" else option.label) },
                    onClick = {
                        open = false
                        onSelected(option.value)
                    },
                )
            }
        }
    }
}

private fun applyPreset(row: SignalConfigRow, strategy: SignalPresetStrategy): SignalConfigRow = when (strategy) {
    SignalPresetStrategy.REAL_TIME_DRIVING -> row.copy(selected = true, interval = when (row.category) {
        "Driving", "Powertrain", "Location" -> 1
        "Charging", "Climate", "Tires & Service" -> 10
        "Vehicle Config", "User Preference" -> 86400
        else -> 10
    })
    SignalPresetStrategy.BALANCED -> row.copy(selected = true, interval = 10)
    SignalPresetStrategy.LOW_POWER -> row.copy(selected = true, interval = 60)
    SignalPresetStrategy.TRACK_MODE -> row.copy(selected = true, interval = when (row.category) {
        "Driving", "Powertrain", "Location" -> 1
        "Vehicle Config", "User Preference" -> 3600
        else -> 30
    })
    SignalPresetStrategy.COST_SAVER -> row.copy(
        selected = row.category in setOf("Location", "Charging", "Vehicle State", "Safety"),
        interval = if (row.category == "Vehicle State") 900 else 300,
    )
    SignalPresetStrategy.SLEEP_WATCH -> row.copy(
        selected = row.category in setOf("Safety", "Vehicle State", "Location", "Charging", "Climate"),
        interval = if (row.category in setOf("Safety", "Vehicle State", "Charging")) 60 else 300,
    )
    SignalPresetStrategy.DIAGNOSTICS -> row.copy(selected = true, interval = when (row.category) {
        "Powertrain", "Tires & Service", "Climate" -> 5
        "Driving", "Charging", "Vehicle State", "Safety", "Location" -> 10
        "Media" -> 60
        else -> 3600
    })
    SignalPresetStrategy.TRIP_LOGGER -> row.copy(
        selected = row.category !in setOf("Media", "User Preference", "Vehicle Config"),
        interval = when (row.category) {
            "Location" -> 1
            "Driving" -> 5
            "Powertrain", "Charging" -> 30
            "Climate", "Vehicle State", "Safety" -> 60
            else -> 300
        },
    )
}

@Composable
private fun CheckBoxGlyph(selected: Boolean, partial: Boolean, onClick: () -> Unit) {
    val color = if (selected || partial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        modifier = Modifier
            .size(22.dp)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { role = Role.Checkbox },
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, color),
        color = if (selected) MaterialTheme.colorScheme.primary else if (partial) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) Text("✓", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
            if (partial) Text("–", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SignalFooter(
    strings: SignalConfigModalText,
    selectedCount: Int,
    fastCount: Int,
    defaultCount: Int,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildString {
                append(strings.selectedFooter.format(selectedCount))
                if (selectedCount > 0) append(" • ").append(strings.atFastInterval.format(fastCount))
                if (selectedCount > 0) append(" • ").append(strings.atDefaultInterval.format(defaultCount))
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
            Button(onClick = onSubmit, enabled = selectedCount > 0) { Text(strings.subscribe.format(selectedCount)) }
        }
    }
}
