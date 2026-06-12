package com.teslasync.modalsdialogs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp

/** Modal state branch for TOU settings. */
enum class TOUSettingsModalState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }
enum class TOUSettingsTab { PRESET, CUSTOM }

data class TOUPresetOption(val id: String, val name: String, val utility: String, val settingsJson: String)
data class TOUSettingsSubmit(val presetId: String?, val settingsJson: String, val wrapsInnerObject: Boolean)

/** Native parity modal for selecting a preset tariff or submitting custom TOU JSON. */
@Composable
fun TOUSettingsModal(
    open: Boolean,
    presets: List<TOUPresetOption>,
    updating: Boolean,
    onClose: () -> Unit,
    onSubmit: (TOUSettingsSubmit) -> Unit,
    modifier: Modifier = Modifier,
    state: TOUSettingsModalState = TOUSettingsModalState.CONTENT,
    onRetry: (() -> Unit)? = null,
    titleLabel: String = "Update Rate Plan",
    descriptionLabel: String = "Configure your utility rate plan so the Powerwall can optimize charging and discharging based on electricity pricing.",
    presetTabLabel: String = "Preset Tariff",
    customTabLabel: String = "Custom JSON",
    noPresetError: String = "Please select a rate plan",
    emptyJsonError: String = "Please enter the TOU settings JSON",
    notObjectError: String = "JSON must be an object",
    invalidJsonError: String = "Invalid JSON — please check syntax",
    ratePlanLabel: String = "Rate Plan",
    choosePlanLabel: String = "Choose a rate plan…",
    previewLabel: String = "Preview",
    customJsonLabel: String = "TOU Settings JSON",
    customJsonSample: String = "{\n  \"tou_settings\": {\n    \"optimization_strategy\": \"economics\",\n    \"tariff_content_v2\": { }\n  }\n}",
    customHintLabel: String = "Paste the full tou_settings payload or just the inner object. See Tesla Fleet API docs for the schema.",
    cancelLabel: String = "Cancel",
    submitLabel: String = "Update Rate Plan",
    closeLabel: String = "Close",
    loadingLabel: String = "Loading rate plans…",
    emptyLabel: String = "No rate plans are available.",
    errorLabel: String = "Could not load rate plan settings.",
    retryLabel: String = "Retry",
    staleLabel: String = "Showing saved rate plan data.",
    offlineLabel: String = "Offline — cached rate plan data is shown.",
) {
    if (!open) return
    var activeTab by remember { mutableStateOf(TOUSettingsTab.PRESET) }
    var selectedPreset by remember { mutableStateOf("") }
    var customJson by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        error = null
        if (activeTab == TOUSettingsTab.PRESET) {
            val preset = presets.firstOrNull { it.id == selectedPreset }
            if (preset == null) { error = noPresetError; return }
            onSubmit(TOUSettingsSubmit(preset.id, preset.settingsJson, false))
        } else {
            val trimmed = customJson.trim()
            if (trimmed.isEmpty()) { error = emptyJsonError; return }
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) { error = notObjectError; return }
            if (trimmed.count { it == '{' } != trimmed.count { it == '}' }) { error = invalidJsonError; return }
            onSubmit(TOUSettingsSubmit(null, trimmed, !trimmed.contains("\"tou_settings\"")))
        }
    }

    Dialog(onDismissRequest = { if (!updating) onClose() }) {
        Surface(modifier = modifier.widthIn(max = 620.dp), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Header(titleLabel, closeLabel) { if (!updating) onClose() }
                when (state) {
                    TOUSettingsModalState.LOADING -> Message(loadingLabel, true)
                    TOUSettingsModalState.EMPTY -> Message(emptyLabel)
                    TOUSettingsModalState.ERROR -> ErrorBlock(errorLabel, retryLabel, onRetry)
                    else -> {
                        if (state == TOUSettingsModalState.STALE) Banner(staleLabel)
                        if (state == TOUSettingsModalState.OFFLINE) Banner(offlineLabel)
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(descriptionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = activeTab == TOUSettingsTab.PRESET, onClick = { activeTab = TOUSettingsTab.PRESET; error = null }, label = { Text(presetTabLabel) })
                                FilterChip(selected = activeTab == TOUSettingsTab.CUSTOM, onClick = { activeTab = TOUSettingsTab.CUSTOM; error = null }, label = { Text(customTabLabel) })
                            }
                            if (activeTab == TOUSettingsTab.PRESET) {
                                Text(ratePlanLabel, fontWeight = FontWeight.Medium)
                                if (presets.isEmpty()) Text(choosePlanLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                presets.forEach { preset ->
                                    Row(Modifier.fillMaxWidth().semantics { contentDescription = "${preset.name} ${preset.utility}" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RadioButton(selected = selectedPreset == preset.id, onClick = { selectedPreset = preset.id })
                                        Column { Text(preset.name); Text(preset.utility, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                                presets.firstOrNull { it.id == selectedPreset }?.let { Panel { Text(previewLabel, style = MaterialTheme.typography.labelMedium); Text(it.settingsJson, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) } }
                            } else {
                                OutlinedTextField(value = customJson, onValueChange = { customJson = it }, label = { Text(customJsonLabel) }, supportingText = { Text(customHintLabel) }, minLines = 12, textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.fillMaxWidth().semantics { contentDescription = customJsonLabel })
                                Text(customJsonSample, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                            if (error != null) Text("⚡ ${error!!}", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { contentDescription = error!! })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { OutlinedButton(onClick = onClose, enabled = !updating) { Text(cancelLabel) }; Button(onClick = { submit() }, enabled = !updating, modifier = Modifier.padding(start = 8.dp)) { if (updating) CircularProgressIndicator(strokeWidth = 2.dp); Text(submitLabel, Modifier.padding(start = if (updating) 8.dp else 0.dp)) } }
                    }
                }
            }
        }
    }
}

@Composable private fun Header(title: String, closeLabel: String, onClose: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeLabel }) { Text(closeLabel) } } }
@Composable private fun Panel(content: @Composable () -> Unit) { Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(12.dp)) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() } } }
@Composable private fun Message(text: String, busy: Boolean = false) { Column(Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = text }, verticalArrangement = Arrangement.spacedBy(10.dp)) { if (busy) CircularProgressIndicator(); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ErrorBlock(text: String, retryLabel: String, onRetry: (() -> Unit)?) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text, color = MaterialTheme.colorScheme.error); if (onRetry != null) Button(onClick = onRetry) { Text(retryLabel) } } }
@Composable private fun Banner(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }
