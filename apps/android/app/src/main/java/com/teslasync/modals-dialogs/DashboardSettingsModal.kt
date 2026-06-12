package com.teslasync.modalsdialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp

/** State branch rendered by DashboardSettingsModal. */
enum class DashboardSettingsModalState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

data class AndroidDashboardSettings(val vehicleId: Int? = null, val refreshIntervalSeconds: Int = 0, val showWidgetBorders: Boolean = true, val compactMode: Boolean = false)
data class AndroidSavedDashboard(val id: String, val name: String, val icon: String = "📊", val settings: AndroidDashboardSettings = AndroidDashboardSettings())
data class AndroidVehicleOption(val id: Int, val displayName: String)
data class DashboardSettingsResult(val name: String, val icon: String, val settings: AndroidDashboardSettings)

/** Native parity modal for dashboard identity, vehicle filter, refresh interval, and display settings. */
@Composable
fun DashboardSettingsModal(
    open: Boolean,
    dashboard: AndroidSavedDashboard,
    vehicles: List<AndroidVehicleOption>,
    onClose: () -> Unit,
    onSave: (DashboardSettingsResult) -> Unit,
    modifier: Modifier = Modifier,
    state: DashboardSettingsModalState = DashboardSettingsModalState.CONTENT,
    onRetry: (() -> Unit)? = null,
    titleLabel: String = "Dashboard Settings",
    identityLabel: String = "Identity",
    vehicleFilterLabel: String = "Vehicle Filter",
    vehicleFilterDescription: String = "Show data for a specific vehicle in all widgets. Widget-level filters take precedence.",
    refreshLabel: String = "Auto-Refresh",
    displayLabel: String = "Display",
    allVehiclesLabel: String = "All Vehicles",
    dashboardNameLabel: String = "Dashboard name",
    nameLabel: String = "Name",
    iconLabel: String = "Icon",
    showBordersLabel: String = "Show widget borders",
    compactModeLabel: String = "Compact mode (smaller gaps)",
    cancelLabel: String = "Cancel",
    saveLabel: String = "Save",
    closeLabel: String = "Close",
    loadingLabel: String = "Loading dashboard settings…",
    emptyLabel: String = "No dashboard settings to show.",
    errorLabel: String = "Could not load dashboard settings.",
    retryLabel: String = "Retry",
    staleLabel: String = "Showing saved dashboard settings.",
    offlineLabel: String = "Offline — cached dashboard settings are shown.",
    refreshOptions: List<Pair<Int, String>> = listOf(0 to "Default (per widget)", 5 to "Every 5 seconds", 10 to "Every 10 seconds", 30 to "Every 30 seconds", 60 to "Every minute", 300 to "Every 5 minutes"),
    emojis: List<String> = listOf("📊", "🔋", "🚗", "⚡", "🛡️", "🗺️", "📈", "🎯", "🔧", "🏠", "🌡️", "🎮", "��", "🖥️", "🔔", "⭐"),
) {
    if (!open) return
    var name by remember { mutableStateOf(dashboard.name) }
    var icon by remember { mutableStateOf(dashboard.icon) }
    var settings by remember { mutableStateOf(dashboard.settings) }
    LaunchedEffect(open, dashboard.id) { if (open) { name = dashboard.name; icon = dashboard.icon; settings = dashboard.settings } }

    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.widthIn(max = 580.dp), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Header(titleLabel, closeLabel, onClose)
                when (state) {
                    DashboardSettingsModalState.LOADING -> Message(loadingLabel, true)
                    DashboardSettingsModalState.EMPTY -> Message(emptyLabel)
                    DashboardSettingsModalState.ERROR -> ErrorBlock(errorLabel, retryLabel, onRetry)
                    else -> {
                        if (state == DashboardSettingsModalState.STALE) Banner(staleLabel)
                        if (state == DashboardSettingsModalState.OFFLINE) Banner(offlineLabel)
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Section(identityLabel) {
                                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(nameLabel) }, supportingText = { Text(dashboardNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = nameLabel })
                                Text(iconLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { emojis.forEach { emoji -> FilterChip(selected = icon == emoji, onClick = { icon = emoji }, label = { Text(emoji) }, modifier = Modifier.semantics { contentDescription = emoji }) } }
                            }
                            Section(vehicleFilterLabel, vehicleFilterDescription) {
                                VehicleChoice(null, allVehiclesLabel, settings.vehicleId == null) { settings = settings.copy(vehicleId = null) }
                                vehicles.forEach { v -> VehicleChoice(v.id, v.displayName, settings.vehicleId == v.id) { settings = settings.copy(vehicleId = v.id) } }
                            }
                            Section(refreshLabel) { refreshOptions.forEach { (seconds, label) -> VehicleChoice(seconds, label, settings.refreshIntervalSeconds == seconds) { settings = settings.copy(refreshIntervalSeconds = seconds) } } }
                            Section(displayLabel) {
                                ToggleRow(showBordersLabel, settings.showWidgetBorders) { settings = settings.copy(showWidgetBorders = it) }
                                ToggleRow(compactModeLabel, settings.compactMode) { settings = settings.copy(compactMode = it) }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { OutlinedButton(onClick = onClose) { Text(cancelLabel) }; Button(onClick = { if (name.trim().isNotEmpty()) { onSave(DashboardSettingsResult(name.trim(), icon, settings)); onClose() } }, modifier = Modifier.padding(start = 8.dp).semantics { contentDescription = saveLabel }) { Text(saveLabel) } }
                    }
                }
            }
        }
    }
}

@Composable private fun Header(title: String, closeLabel: String, onClose: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeLabel }) { Text(closeLabel) } } }
@Composable private fun Section(title: String, body: String? = null, content: @Composable () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant); if (body != null) Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); content() } }
@Composable private fun VehicleChoice(id: Int?, label: String, selected: Boolean, onSelect: () -> Unit) { Row(Modifier.fillMaxWidth().semantics { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(8.dp)) { RadioButton(selected = selected, onClick = onSelect); Text(label, Modifier.padding(top = 12.dp)) } }
@Composable private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().semantics { contentDescription = label }, horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Switch(checked = checked, onCheckedChange = onChange) } }
@Composable private fun Message(text: String, busy: Boolean = false) { Column(Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = text }, verticalArrangement = Arrangement.spacedBy(10.dp)) { if (busy) CircularProgressIndicator(); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ErrorBlock(text: String, retryLabel: String, onRetry: (() -> Unit)?) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text, color = MaterialTheme.colorScheme.error); if (onRetry != null) Button(onClick = onRetry) { Text(retryLabel) } } }
@Composable private fun Banner(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }
