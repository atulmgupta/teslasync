package com.teslasync.modalsdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp

/** State branch rendered by FlagEditDrawer before the edit form. */
enum class FlagEditDrawerState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

data class FeatureFlagEditorEntry(
    val key: String,
    val valueJson: String,
)

data class FeatureFlagSaveInput(
    val key: String,
    val valueJson: String,
    val reason: String,
)

/**
 * Native parity surface for the web FlagEditDrawer: create/edit a feature flag JSON value with audit reason.
 */
@Composable
fun FlagEditDrawer(
    open: Boolean,
    initial: FeatureFlagEditorEntry?,
    saving: Boolean,
    onClose: () -> Unit,
    onSave: (FeatureFlagSaveInput) -> Unit,
    modifier: Modifier = Modifier,
    state: FlagEditDrawerState = FlagEditDrawerState.CONTENT,
    errorMessage: String = "Could not load feature flag details.",
    onRetry: (() -> Unit)? = null,
    loadingLabel: String = "Loading feature flag…",
    emptyLabel: String = "No feature flag selected.",
    staleLabel: String = "Showing saved data. Refresh to get the latest value.",
    offlineLabel: String = "Offline — editing cached flag data.",
    editTitleFormat: String = "Edit flag \"%s\"",
    createTitle: String = "Create flag",
    cancelLabel: String = "Cancel",
    saveLabel: String = "Save flag",
    closeLabel: String = "Close",
    retryLabel: String = "Retry",
    keyLabel: String = "Flag key",
    keySample: String = "feature.dlq.replay_enabled",
    keyImmutableLabel: String = "Flag keys are immutable once created. Delete + re-create to rename.",
    valueLabel: String = "Value (JSON)",
    valueSample: String = "{\n  \"enabled\": true\n}",
    reasonLabel: String = "Reason",
    reasonSample: String = "Why this change? (logged in audit)",
    valueRequiredError: String = "Value is required.",
    invalidJsonPrefix: String = "Invalid JSON:",
    malformedJsonLabel: String = "malformed JSON value",
) {
    if (!open) return
    val editing = initial != null
    var keyInput by remember { mutableStateOf(initial?.key.orEmpty()) }
    var valueInput by remember { mutableStateOf(initial?.valueJson.orEmpty()) }
    var reasonInput by remember { mutableStateOf("") }

    LaunchedEffect(open, initial) {
        if (open) {
            keyInput = initial?.key.orEmpty()
            valueInput = initial?.valueJson.orEmpty()
            reasonInput = ""
        }
    }

    val validationError = jsonValidationError(valueInput, valueRequiredError, invalidJsonPrefix, malformedJsonLabel)
    val canSave = keyInput.trim().isNotEmpty() && reasonInput.trim().isNotEmpty() && validationError == null && !saving
    val title = if (editing) editTitleFormat.format(initial?.key.orEmpty()) else createTitle

    Dialog(onDismissRequest = { if (!saving) onClose() }) {
        Surface(
            modifier = modifier.widthIn(max = 560.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModalHeader(title = title, closeLabel = closeLabel, onClose = { if (!saving) onClose() })
                when (state) {
                    FlagEditDrawerState.LOADING -> CenterMessage(loadingLabel, busy = true)
                    FlagEditDrawerState.EMPTY -> CenterMessage(emptyLabel)
                    FlagEditDrawerState.ERROR -> ErrorMessage(errorMessage, retryLabel, onRetry)
                    else -> {
                        if (state == FlagEditDrawerState.STALE) InfoBanner(staleLabel)
                        if (state == FlagEditDrawerState.OFFLINE) InfoBanner(offlineLabel)
                        Column(
                            Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Panel {
                                OutlinedTextField(
                                    value = keyInput,
                                    onValueChange = { keyInput = it },
                                    enabled = !editing && !saving,
                                    label = { Text(keyLabel) },
                                    supportingText = { Text(if (editing) keyImmutableLabel else keySample) },
                                    singleLine = true,
                                    isError = keyInput.trim().isEmpty(),
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = keyLabel },
                                )
                            }
                            Panel {
                                OutlinedTextField(
                                    value = valueInput,
                                    onValueChange = { valueInput = it },
                                    enabled = !saving,
                                    label = { Text(valueLabel) },
                                    supportingText = { Text(validationError ?: valueSample) },
                                    minLines = 8,
                                    isError = validationError != null,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = valueLabel },
                                )
                            }
                            Panel {
                                OutlinedTextField(
                                    value = reasonInput,
                                    onValueChange = { reasonInput = it },
                                    enabled = !saving,
                                    label = { Text(reasonLabel) },
                                    supportingText = { Text(reasonSample) },
                                    singleLine = true,
                                    isError = reasonInput.trim().isEmpty(),
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = reasonLabel },
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = onClose, enabled = !saving) { Text(cancelLabel) }
                            Spacer(Modifier.widthIn(min = 8.dp))
                            Button(
                                onClick = { onSave(FeatureFlagSaveInput(keyInput.trim(), valueInput.trim(), reasonInput.trim())) },
                                enabled = canSave,
                                modifier = Modifier.semantics { contentDescription = saveLabel },
                            ) {
                                if (saving) CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp)
                                Text(saveLabel, Modifier.padding(start = if (saving) 8.dp else 0.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun jsonValidationError(value: String, required: String, invalidPrefix: String, malformedJsonLabel: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return required
    val startsOk = trimmed.first() in listOf('{', '[', '"') || trimmed in listOf("true", "false", "null") || trimmed.first().isDigit() || trimmed.first() == '-'
    val pairsOk = trimmed.count { it == '{' } == trimmed.count { it == '}' } && trimmed.count { it == '[' } == trimmed.count { it == ']' }
    return if (startsOk && pairsOk) null else "$invalidPrefix $malformedJsonLabel"
}

@Composable
private fun ModalHeader(title: String, closeLabel: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeLabel }) { Text(closeLabel) }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
    ) { Column(Modifier.padding(14.dp)) { content() } }
}

@Composable
private fun InfoBanner(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }

@Composable
private fun CenterMessage(text: String, busy: Boolean = false) { Column(Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = text }, verticalArrangement = Arrangement.spacedBy(10.dp)) { if (busy) CircularProgressIndicator(); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun ErrorMessage(text: String, retryLabel: String, onRetry: (() -> Unit)?) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text, color = MaterialTheme.colorScheme.error); if (onRetry != null) Button(onClick = onRetry) { Text(retryLabel) } } }
