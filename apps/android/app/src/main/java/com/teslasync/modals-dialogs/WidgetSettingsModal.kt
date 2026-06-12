package com.teslasync.modalsdialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Widget categories used to determine which setting groups are visible. */
enum class WidgetSettingsCategory { VEHICLE, BATTERY, ENERGY, CHARGING, DRIVING, CLIMATE, TIRES, SECURITY, COMMANDS, MEDIA, TELEMETRY, ANALYTICS, ALERTS, AUTOMATIONS, SYSTEM, MAPS }

/** Immutable widget definition metadata from the dashboard registry. */
data class WidgetSettingsDefinition(
    val name: String,
    val category: WidgetSettingsCategory,
)

/** Immutable per-widget configuration edited by this dialog. */
data class WidgetSettingsConfig(
    val vehicleId: Long? = null,
    val refreshRateSeconds: Int? = null,
    val timeRange: String? = null,
    val showTitle: Boolean = true,
)

/** Vehicle selector row supplied by the vehicle state holder. */
data class WidgetSettingsVehicle(
    val id: Long,
    val displayName: String,
)

/** Native state wrapper for vehicle selector loading, cache and failure branches. */
data class WidgetSettingsUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val stale: Boolean = false,
    val offline: Boolean = false,
)

/**
 * Native Android parity surface for the web `WidgetSettingsModal`.
 *
 * Renders vehicle selection for vehicle-scoped widgets, refresh interval,
 * chart time range, appearance, cancel and save actions without performing any
 * network work in the view.
 */
@Composable
fun WidgetSettingsModal(
    open: Boolean,
    definition: WidgetSettingsDefinition,
    initialConfig: WidgetSettingsConfig,
    vehicles: List<WidgetSettingsVehicle>,
    onSave: (WidgetSettingsConfig) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: WidgetSettingsUiState = WidgetSettingsUiState(),
    onRetry: (() -> Unit)? = null,
    titleSuffixText: String = "Settings",
    vehicleText: String = "Vehicle",
    refreshIntervalText: String = "Refresh Interval",
    timeRangeText: String = "Time Range",
    appearanceText: String = "Appearance",
    allVehiclesText: String = "All Vehicles (first)",
    vehicleNameText: String = "Vehicle {id}",
    defaultText: String = "Default",
    fiveSecondsText: String = "5 seconds",
    fifteenSecondsText: String = "15 seconds",
    thirtySecondsText: String = "30 seconds",
    oneMinuteText: String = "1 minute",
    twentyFourHoursText: String = "Last 24 hours",
    sevenDaysText: String = "Last 7 days",
    thirtyDaysText: String = "Last 30 days",
    ninetyDaysText: String = "Last 90 days",
    showTitleText: String = "Show widget title",
    staleText: String = "Stale",
    offlineText: String = "Offline",
    loadingText: String = "Loading widget settings",
    retryText: String = "Retry",
    cancelText: String = "Cancel",
    saveText: String = "Save",
) {
    if (!open) return
    var config by remember(open, initialConfig) { mutableStateOf(initialConfig) }
    LaunchedEffect(initialConfig) { config = initialConfig }
    val isVehicleWidget = definition.category != WidgetSettingsCategory.SYSTEM && definition.category != WidgetSettingsCategory.ANALYTICS
    val isChartWidget = definition.category in setOf(
        WidgetSettingsCategory.DRIVING,
        WidgetSettingsCategory.CHARGING,
        WidgetSettingsCategory.ANALYTICS,
        WidgetSettingsCategory.BATTERY,
    )

    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.semantics { contentDescription = "${definition.name} $titleSuffixText" }, shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${definition.name} $titleSuffixText", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onClose) { Text(cancelText) }
                }
                StateChip(state, staleText, offlineText)
                when {
                    state.loading -> LoadingBlock(loadingText)
                    state.errorMessage != null -> ErrorBlock(state.errorMessage, retryText, onRetry)
                    else -> Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (isVehicleWidget) {
                            Section(vehicleText) {
                                val options = listOf("all" to allVehiclesText) + vehicles.map { it.id.toString() to (it.displayName.ifBlank { vehicleNameText.replace("{id}", it.id.toString()) }) }
                                SelectRow(vehicleText, config.vehicleId?.toString() ?: "all", options) { selected ->
                                    config = config.copy(vehicleId = selected.takeIf { it != "all" }?.toLongOrNull())
                                }
                            }
                        }
                        Section(refreshIntervalText) {
                            SelectRow(
                                refreshIntervalText,
                                config.refreshRateSeconds?.toString() ?: "default",
                                listOf("default" to defaultText, "5" to fiveSecondsText, "15" to fifteenSecondsText, "30" to thirtySecondsText, "60" to oneMinuteText),
                            ) { selected -> config = config.copy(refreshRateSeconds = selected.takeIf { it != "default" }?.toIntOrNull()) }
                        }
                        if (isChartWidget) {
                            Section(timeRangeText) {
                                SelectRow(
                                    timeRangeText,
                                    config.timeRange ?: "7d",
                                    listOf("24h" to twentyFourHoursText, "7d" to sevenDaysText, "30d" to thirtyDaysText, "90d" to ninetyDaysText),
                                ) { selected -> config = config.copy(timeRange = selected) }
                            }
                        }
                        Section(appearanceText) {
                            Row(Modifier.fillMaxWidth().semantics { contentDescription = showTitleText }, verticalAlignment = Alignment.CenterVertically) {
                                Text(showTitleText, modifier = Modifier.weight(1f))
                                Switch(checked = config.showTitle, onCheckedChange = { config = config.copy(showTitle = it) })
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onClose) { Text(cancelText) }
                    Button(onClick = { onSave(config); onClose() }, modifier = Modifier.padding(start = 8.dp)) { Text(saveText) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable Column.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SelectRow(label: String, value: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == value }?.second ?: value
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        androidx.compose.foundation.layout.Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(current) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (optionValue, optionLabel) ->
                    DropdownMenuItem(text = { Text(optionLabel) }, onClick = { expanded = false; onSelected(optionValue) })
                }
            }
        }
    }
}

@Composable
private fun StateChip(state: WidgetSettingsUiState, stale: String, offline: String) {
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
