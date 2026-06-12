package com.teslasync.modalsdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp

/** State branch rendered by ImportPreviewModal. */
enum class ImportPreviewModalState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }
enum class ImportSourceTab { FILE, PASTE, URL }

data class ImportDashboardPreview(val name: String, val layoutCells: List<Boolean>)
data class ImportWidgetPreview(val id: String, val name: String? = null, val available: Boolean)
data class ImportValidationPreview(val isValid: Boolean, val errors: List<String>, val warnings: List<String>, val dashboard: ImportDashboardPreview?, val widgets: List<ImportWidgetPreview>)

/** Native parity modal for dashboard import input and validation preview with errors, warnings, and widget availability. */
@Composable
fun ImportPreviewModal(
    open: Boolean,
    validation: ImportValidationPreview?,
    onClose: () -> Unit,
    onValidateJson: (String) -> Unit,
    onValidateUrl: (String) -> Unit,
    onBrowseFiles: () -> Unit,
    onConfirm: () -> Unit,
    onBackToInput: () -> Unit = {},
    modifier: Modifier = Modifier,
    state: ImportPreviewModalState = ImportPreviewModalState.CONTENT,
    onRetry: (() -> Unit)? = null,
    titleLabel: String = "Import Dashboard",
    previewTitleLabel: String = "Import Preview",
    noDataError: String = "No data to validate",
    readErrorLabel: String = "Failed to read file",
    invalidFileTypeLabel: String = "Please drop a .json file",
    noImportParamLabel: String = "URL does not contain an import parameter",
    invalidUrlLabel: String = "Invalid URL format",
    fromFileLabel: String = "From File",
    pasteJsonLabel: String = "Paste JSON",
    fromUrlLabel: String = "From URL",
    dropFileLabel: String = "Drop a .json file here or click to browse",
    browseFilesLabel: String = "Browse Files",
    dashboardJsonFileLabel: String = "Dashboard JSON file",
    validateLabel: String = "Validate & Preview",
    loadUrlLabel: String = "Load from URL",
    widgetCountFormat: String = "%d widgets",
    skippedCountFormat: String = "%d skipped",
    widgetsLabel: String = "Widgets",
    notAvailableLabel: String = "Not available",
    cannotPreviewLabel: String = "Cannot preview this layout",
    backLabel: String = "Back",
    confirmLabel: String = "Import Dashboard",
    closeLabel: String = "Close",
    loadingLabel: String = "Reading dashboard import…",
    emptyLabel: String = "No import data has been selected.",
    errorLabel: String = "Could not validate dashboard import.",
    retryLabel: String = "Retry",
    staleLabel: String = "Showing saved import preview.",
    offlineLabel: String = "Offline — import validation may use cached data.",
    pasteJsonSample: String = "{\"name\": \"My Dashboard\", \"widgets\": [], \"layouts\": {}}",
    urlSample: String = "https://teslasync.example.com/dashboard#import=...",
    layoutPreviewLabel: String = "Dashboard layout preview",
) {
    if (!open) return
    var activeTab by remember { mutableStateOf(ImportSourceTab.FILE) }
    var pastedJson by remember { mutableStateOf("") }
    var importUrl by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    val showingPreview = validation != null

    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.widthIn(max = 640.dp), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Header(if (showingPreview) previewTitleLabel else titleLabel, closeLabel, onClose)
                when (state) {
                    ImportPreviewModalState.LOADING -> Message(loadingLabel, true)
                    ImportPreviewModalState.EMPTY -> Message(emptyLabel)
                    ImportPreviewModalState.ERROR -> ErrorBlock(errorLabel, retryLabel, onRetry)
                    else -> {
                        if (state == ImportPreviewModalState.STALE) Banner(staleLabel)
                        if (state == ImportPreviewModalState.OFFLINE) Banner(offlineLabel)
                        if (validation != null) PreviewContent(validation, widgetCountFormat, skippedCountFormat, widgetsLabel, notAvailableLabel, cannotPreviewLabel, backLabel, confirmLabel, layoutPreviewLabel, onBack = { parseError = null; onBackToInput() }, onConfirm = onConfirm) else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = activeTab == ImportSourceTab.FILE, onClick = { activeTab = ImportSourceTab.FILE; parseError = null }, label = { Text(fromFileLabel) })
                                FilterChip(selected = activeTab == ImportSourceTab.PASTE, onClick = { activeTab = ImportSourceTab.PASTE; parseError = null }, label = { Text(pasteJsonLabel) })
                                FilterChip(selected = activeTab == ImportSourceTab.URL, onClick = { activeTab = ImportSourceTab.URL; parseError = null }, label = { Text(fromUrlLabel) })
                            }
                            when (activeTab) {
                                ImportSourceTab.FILE -> Surface(border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = dashboardJsonFileLabel }, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("⬆", style = MaterialTheme.typography.headlineMedium); Text(dropFileLabel, color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedButton(onClick = onBrowseFiles) { Text(browseFilesLabel) }; Text(invalidFileTypeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text(readErrorLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }
                                ImportSourceTab.PASTE -> { OutlinedTextField(value = pastedJson, onValueChange = { pastedJson = it }, minLines = 10, textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), supportingText = { Text(pasteJsonSample) }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = pasteJsonLabel }); Button(onClick = { if (pastedJson.trim().isEmpty()) parseError = noDataError else onValidateJson(pastedJson) }, enabled = pastedJson.trim().isNotEmpty()) { Text(validateLabel) } }
                                ImportSourceTab.URL -> { OutlinedTextField(value = importUrl, onValueChange = { importUrl = it }, singleLine = true, supportingText = { Text(urlSample) }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = fromUrlLabel }); Button(onClick = { if (!importUrl.contains("import=")) parseError = noImportParamLabel else if (!importUrl.startsWith("http")) parseError = invalidUrlLabel else onValidateUrl(importUrl) }, enabled = importUrl.trim().isNotEmpty()) { Text(loadUrlLabel) } }
                            }
                            if (parseError != null) ErrorBanner(parseError!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun PreviewContent(validation: ImportValidationPreview, widgetCountFormat: String, skippedCountFormat: String, widgetsLabel: String, notAvailableLabel: String, cannotPreviewLabel: String, backLabel: String, confirmLabel: String, layoutPreviewLabel: String, onBack: () -> Unit, onConfirm: () -> Unit) { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) { if (validation.errors.isNotEmpty()) ErrorBanner(validation.errors.joinToString("\n")); if (validation.warnings.isNotEmpty()) WarningBanner(validation.warnings.joinToString("\n")); val dashboard = validation.dashboard; if (dashboard != null) { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { MiniGrid(dashboard.layoutCells, layoutPreviewLabel, Modifier.width(140.dp).height(96.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(dashboard.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); val available = validation.widgets.count { it.available }; val missing = validation.widgets.count { !it.available }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip(onClick = {}, label = { Text(widgetCountFormat.format(available)) }); if (missing > 0) AssistChip(onClick = {}, label = { Text(skippedCountFormat.format(missing)) }) } } }; Text(widgetsLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium); validation.widgets.forEach { WidgetRow(it, notAvailableLabel) } } else Message(cannotPreviewLabel); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onBack) { Text(backLabel) }; if (validation.isValid && validation.dashboard != null) Button(onClick = onConfirm) { Text("✓  $confirmLabel") } } } }
@Composable private fun WidgetRow(widget: ImportWidgetPreview, notAvailableLabel: String) { Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), border = BorderStroke(1.dp, if (widget.available) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) { Row(Modifier.fillMaxWidth().padding(10.dp).semantics { contentDescription = widget.name ?: widget.id }, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (widget.available) "✓" else "✕", color = if (widget.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error); Text(widget.name ?: widget.id, modifier = Modifier.weight(1f), textDecoration = if (widget.available) null else TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant); if (!widget.available) Text(notAvailableLabel, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun MiniGrid(cells: List<Boolean>, layoutPreviewLabel: String, modifier: Modifier) { Surface(modifier = modifier.semantics { contentDescription = layoutPreviewLabel }, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { repeat(3) { row -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(4) { col -> Surface(Modifier.size(26.dp), color = if (cells.getOrNull(row * 4 + col) == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp), content = {}) } } } } } }
@Composable private fun Header(title: String, closeLabel: String, onClose: () -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeLabel }) { Text(closeLabel) } } }
@Composable private fun Message(text: String, busy: Boolean = false) { Column(Modifier.fillMaxWidth().padding(24.dp).semantics { contentDescription = text }, verticalArrangement = Arrangement.spacedBy(10.dp)) { if (busy) CircularProgressIndicator(); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ErrorBlock(text: String, retryLabel: String, onRetry: (() -> Unit)?) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(text, color = MaterialTheme.colorScheme.error); if (onRetry != null) Button(onClick = onRetry) { Text(retryLabel) } } }
@Composable private fun Banner(text: String) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) } }
@Composable private fun ErrorBanner(text: String) { Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) { Text("⚠ $text", Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
@Composable private fun WarningBanner(text: String) { Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp)) { Text("⚠ $text", Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onTertiaryContainer) } }
