package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

/** Clock location options used by [KioskSettingsConfig]. */
enum class KioskClockPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** Immutable kiosk configuration projected from the dashboard kiosk state holder. */
data class KioskSettingsConfig(
    val rotateIntervalSeconds: Int = 0,
    val dashboardIds: List<String> = emptyList(),
    val hideCursor: Boolean = false,
    val cursorTimeoutSeconds: Int = 5,
    val dimAfterMinutes: Int = 0,
    val dimLevel: Float = 0.5f,
    val showClock: Boolean = true,
    val clockPosition: KioskClockPosition = KioskClockPosition.TOP_RIGHT,
    val widgetOpacity: Float = 1f,
    val backgroundOpacity: Float = 1f,
)

/** Saved dashboard row available for kiosk rotation selection. */
data class KioskDashboardOption(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
)

/** Native dialog state wrapper for the kiosk settings surface. */
data class KioskSettingsUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val stale: Boolean = false,
    val offline: Boolean = false,
)

/**
 * Native Android parity surface for the web `KioskSettingsModal`.
 *
 * Renders dashboard rotation, display, transparency, preview, hint, dismiss and
 * enter-kiosk actions. The composable is presentational only: all state is
 * passed in, and every user-facing string is a parameter with an English default.
 */
@Composable
fun KioskSettingsModal(
    open: Boolean,
    config: KioskSettingsConfig,
    dashboards: List<KioskDashboardOption>,
    onUpdateConfig: (KioskSettingsConfig) -> Unit,
    onEnterKiosk: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: KioskSettingsUiState = KioskSettingsUiState(),
    onRetry: (() -> Unit)? = null,
    titleText: String = "Kiosk Settings",
    rotationText: String = "Dashboard Rotation",
    displayText: String = "Display",
    transparencyText: String = "Transparency",
    rotationIntervalText: String = "Rotation Interval",
    dashboardsToRotateText: String = "Dashboards to Rotate",
    defaultText: String = "Default",
    hideCursorText: String = "Auto-hide Cursor",
    cursorTimeoutText: String = "Hide After",
    dimAfterText: String = "Dim Screen After",
    brightnessText: String = "Dimmed Brightness",
    showClockText: String = "Show Clock",
    clockPositionText: String = "Clock Position",
    widgetOpacityText: String = "Widget Opacity",
    backgroundOpacityText: String = "Background Opacity",
    transparentText: String = "Transparent",
    solidText: String = "Solid",
    previewText: String = "Preview — this is how widgets will look",
    transparencyDescriptionText: String = "Adjust widget and background opacity. Higher values are more solid and readable.",
    hintText: String = "Kiosk mode enters fullscreen and hides all navigation. Move the mouse or touch the screen to reveal the exit button. Press Esc to exit.",
    emptyDashboardsText: String = "No dashboards are available for rotation.",
    staleText: String = "Stale",
    offlineText: String = "Offline",
    loadingText: String = "Loading kiosk settings",
    retryText: String = "Retry",
    cancelText: String = "Cancel",
    enterText: String = "Enter Kiosk Mode",
    rotationOptions: List<Pair<String, String>> = defaultRotationOptions(),
    cursorTimeoutOptions: List<Pair<String, String>> = defaultCursorOptions(),
    dimAfterOptions: List<Pair<String, String>> = defaultDimOptions(),
    clockPositionOptions: List<Pair<String, String>> = defaultClockOptions(),
) {
    if (!open) return

    var selectedIds by remember(open, config.dashboardIds, dashboards) {
        mutableStateOf(
            (if (config.dashboardIds.isNotEmpty()) config.dashboardIds else dashboards.map { it.id }).toSet(),
        )
    }
    LaunchedEffect(config.dashboardIds, dashboards) {
        selectedIds = (if (config.dashboardIds.isNotEmpty()) config.dashboardIds else dashboards.map { it.id }).toSet()
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = modifier.semantics { contentDescription = titleText },
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DialogHeader(titleText, onClose, cancelText)
                StateBanner(state, staleText, offlineText)
                when {
                    state.loading -> LoadingBlock(loadingText)
                    state.errorMessage != null -> ErrorBlock(state.errorMessage, retryText, onRetry)
                    dashboards.isEmpty() -> Text(emptyDashboardsText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Section(rotationText) {
                            SelectRow(
                                label = rotationIntervalText,
                                value = config.rotateIntervalSeconds.toString(),
                                options = rotationOptions,
                                onSelected = { onUpdateConfig(config.copy(rotateIntervalSeconds = it.toInt())) },
                            )
                            if (config.rotateIntervalSeconds > 0 && dashboards.size > 1) {
                                Text(dashboardsToRotateText, style = MaterialTheme.typography.labelLarge)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    dashboards.forEach { dashboard ->
                                        val checked = dashboard.id in selectedIds
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    .semantics { contentDescription = dashboard.name },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Switch(
                                                    checked = checked,
                                                    onCheckedChange = {
                                                        val next = selectedIds.toMutableSet()
                                                        if (checked && next.size > 1) next.remove(dashboard.id) else next.add(dashboard.id)
                                                        selectedIds = next
                                                        onUpdateConfig(config.copy(dashboardIds = next.toList()))
                                                    },
                                                )
                                                Text(dashboard.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (dashboard.isDefault) Text(defaultText, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Section(displayText) {
                            SwitchRow(hideCursorText, config.hideCursor) { onUpdateConfig(config.copy(hideCursor = it)) }
                            if (config.hideCursor) {
                                SelectRow(cursorTimeoutText, config.cursorTimeoutSeconds.toString(), cursorTimeoutOptions) {
                                    onUpdateConfig(config.copy(cursorTimeoutSeconds = it.toInt()))
                                }
                            }
                            SelectRow(dimAfterText, config.dimAfterMinutes.toString(), dimAfterOptions) {
                                onUpdateConfig(config.copy(dimAfterMinutes = it.toInt()))
                            }
                            if (config.dimAfterMinutes > 0) {
                                PercentSlider(brightnessText, config.dimLevel, 0.3f, 0.9f) { onUpdateConfig(config.copy(dimLevel = it)) }
                            }
                            SwitchRow(showClockText, config.showClock) { onUpdateConfig(config.copy(showClock = it)) }
                            if (config.showClock) {
                                SelectRow(clockPositionText, config.clockPosition.name, clockPositionOptions) {
                                    onUpdateConfig(config.copy(clockPosition = KioskClockPosition.valueOf(it)))
                                }
                            }
                        }
                        Section(transparencyText) {
                            Text(
                                transparencyDescriptionText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            PercentSlider(widgetOpacityText, config.widgetOpacity, 0.3f, 1f) { onUpdateConfig(config.copy(widgetOpacity = it)) }
                            RangeLabels(transparentText, solidText)
                            PercentSlider(backgroundOpacityText, config.backgroundOpacity, 0f, 1f) { onUpdateConfig(config.copy(backgroundOpacity = it)) }
                            RangeLabels(transparentText, solidText)
                            PreviewSwatch(previewText, config.widgetOpacity, config.backgroundOpacity)
                        }
                        HintBlock(hintText)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onClose) { Text(cancelText) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onUpdateConfig(config.copy(dashboardIds = selectedIds.toList()))
                        onClose()
                        onEnterKiosk()
                    }) { Text(enterText) }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String, onClose: () -> Unit, closeText: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onClose) { Text(closeText) }
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
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SelectRow(label: String, value: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == value }?.second ?: value
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
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
private fun PercentSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label ${(value * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max, steps = 0)
    }
}

@Composable
private fun RangeLabels(start: String, end: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(start, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(end, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewSwatch(text: String, widgetOpacity: Float, backgroundOpacity: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0A14).copy(alpha = backgroundOpacity.coerceIn(0f, 1f)))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f + widgetOpacity.coerceIn(0f, 1f) * 0.55f),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(text, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HintBlock(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("▣", color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StateBanner(state: KioskSettingsUiState, stale: String, offline: String) {
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

private fun defaultRotationOptions() = listOf("0" to "Off", "10" to "10s", "15" to "15s", "30" to "30s", "60" to "1 min", "120" to "2 min", "300" to "5 min")
private fun defaultCursorOptions() = listOf("3" to "3s", "5" to "5s", "10" to "10s", "15" to "15s")
private fun defaultDimOptions() = listOf("0" to "Never", "5" to "5 min", "10" to "10 min", "15" to "15 min", "30" to "30 min", "60" to "60 min")
private fun defaultClockOptions() = listOf(
    KioskClockPosition.TOP_LEFT.name to "Top Left",
    KioskClockPosition.TOP_RIGHT.name to "Top Right",
    KioskClockPosition.BOTTOM_LEFT.name to "Bottom Left",
    KioskClockPosition.BOTTOM_RIGHT.name to "Bottom Right",
)
