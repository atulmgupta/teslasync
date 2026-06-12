package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Data readiness branch rendered by [AddAnnotationPopover]. */
enum class AddAnnotationPopoverState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

/** Category value mirrored from the web `AnnotationCategory` union. */
enum class AnnotationCategory { MILESTONE, MAINTENANCE, TRIP, ISSUE, UPGRADE, CUSTOM }

/** Render metadata for a selectable annotation category pill. */
data class AnnotationCategoryOption(
    val category: AnnotationCategory,
    val label: String,
    val color: Color,
)

/**
 * Native Android parity for `web/src/components/charts/AddAnnotationPopover.tsx`.
 *
 * Renders the add-annotation modal with editable or read-only timestamp,
 * label, category pills, optional description, submit validation, reset-on-close
 * behavior, and loading / empty / error / stale / offline branches.
 */
@Composable
fun AddAnnotationPopover(
    open: Boolean,
    timestamp: String,
    onAdd: (label: String, category: AnnotationCategory, description: String?, occurredAt: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    editableDate: Boolean = false,
    state: AddAnnotationPopoverState = AddAnnotationPopoverState.CONTENT,
    categories: List<AnnotationCategoryOption> = defaultAnnotationCategoryOptions(),
    title: String = "Add Annotation",
    dateLabel: String = "Date",
    timestampLabel: String = "Selected timestamp",
    labelLabel: String = "Label",
    labelHint: String = "e.g., Battery replaced",
    categoryLabel: String = "Category",
    descriptionLabel: String = "Description",
    descriptionHint: String = "Optional description...",
    cancelLabel: String = "Cancel",
    addLabel: String = "Add Annotation",
    loadingLabel: String = "Loading annotation form…",
    emptyLabel: String = "No timestamp is selected.",
    errorLabel: String? = null,
    retryLabel: String = "Retry",
    staleLabel: String = "Timestamp may be out of date.",
    offlineLabel: String = "Offline; the annotation can be queued by the caller.",
    dialogContentDescription: String = title,
    onRetry: (() -> Unit)? = null,
) {
    if (!open) return

    var label by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AnnotationCategory.MILESTONE) }
    var description by remember { mutableStateOf("") }
    var editedDate by remember { mutableStateOf(toDateInputValue(timestamp)) }

    LaunchedEffect(open, timestamp) {
        if (open) editedDate = toDateInputValue(timestamp)
    }

    fun resetAndCancel() {
        label = ""
        category = AnnotationCategory.MILESTONE
        description = ""
        onCancel()
    }

    val occurredAt = if (editableDate) toIsoTimestamp(editedDate) else timestamp
    val canSubmit = label.trim().isNotEmpty() && occurredAt.isNotBlank()

    Dialog(onDismissRequest = { resetAndCancel() }) {
        Surface(
            modifier = modifier
                .widthIn(max = 480.dp)
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
                    AddAnnotationPopoverState.LOADING -> AnnotationStatusMessage(loadingLabel, showSpinner = true)
                    AddAnnotationPopoverState.EMPTY -> AnnotationStatusMessage(emptyLabel)
                    AddAnnotationPopoverState.ERROR -> AnnotationStatusMessage(
                        message = errorLabel ?: emptyLabel,
                        isError = true,
                        actionLabel = retryLabel,
                        onAction = onRetry,
                    )
                    AddAnnotationPopoverState.STALE -> {
                        AnnotationStatusChip(staleLabel)
                        AnnotationForm(
                            editableDate = editableDate,
                            timestamp = timestamp,
                            timestampLabel = timestampLabel,
                            editedDate = editedDate,
                            onDateChange = { editedDate = it },
                            dateLabel = dateLabel,
                            label = label,
                            onLabelChange = { label = it.take(50) },
                            labelLabel = labelLabel,
                            labelHint = labelHint,
                            category = category,
                            onCategoryChange = { category = it },
                            categoryLabel = categoryLabel,
                            categories = categories,
                            description = description,
                            onDescriptionChange = { description = it.take(200) },
                            descriptionLabel = descriptionLabel,
                            descriptionHint = descriptionHint,
                        )
                    }
                    AddAnnotationPopoverState.OFFLINE -> {
                        AnnotationStatusChip(offlineLabel)
                        AnnotationForm(
                            editableDate = editableDate,
                            timestamp = timestamp,
                            timestampLabel = timestampLabel,
                            editedDate = editedDate,
                            onDateChange = { editedDate = it },
                            dateLabel = dateLabel,
                            label = label,
                            onLabelChange = { label = it.take(50) },
                            labelLabel = labelLabel,
                            labelHint = labelHint,
                            category = category,
                            onCategoryChange = { category = it },
                            categoryLabel = categoryLabel,
                            categories = categories,
                            description = description,
                            onDescriptionChange = { description = it.take(200) },
                            descriptionLabel = descriptionLabel,
                            descriptionHint = descriptionHint,
                        )
                    }
                    AddAnnotationPopoverState.CONTENT -> AnnotationForm(
                        editableDate = editableDate,
                        timestamp = timestamp,
                        timestampLabel = timestampLabel,
                        editedDate = editedDate,
                        onDateChange = { editedDate = it },
                        dateLabel = dateLabel,
                        label = label,
                        onLabelChange = { label = it.take(50) },
                        labelLabel = labelLabel,
                        labelHint = labelHint,
                        category = category,
                        onCategoryChange = { category = it },
                        categoryLabel = categoryLabel,
                        categories = categories,
                        description = description,
                        onDescriptionChange = { description = it.take(200) },
                        descriptionLabel = descriptionLabel,
                        descriptionHint = descriptionHint,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { resetAndCancel() }) { Text(cancelLabel) }
                    Button(
                        modifier = Modifier.padding(start = 8.dp),
                        enabled = canSubmit && state != AddAnnotationPopoverState.LOADING,
                        onClick = {
                            onAdd(label.trim(), category, description.trim().ifBlank { null }, occurredAt)
                            label = ""
                            category = AnnotationCategory.MILESTONE
                            description = ""
                        },
                    ) { Text(addLabel) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnotationForm(
    editableDate: Boolean,
    timestamp: String,
    timestampLabel: String,
    editedDate: String,
    onDateChange: (String) -> Unit,
    dateLabel: String,
    label: String,
    onLabelChange: (String) -> Unit,
    labelLabel: String,
    labelHint: String,
    category: AnnotationCategory,
    onCategoryChange: (AnnotationCategory) -> Unit,
    categoryLabel: String,
    categories: List<AnnotationCategoryOption>,
    description: String,
    onDescriptionChange: (String) -> Unit,
    descriptionLabel: String,
    descriptionHint: String,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (editableDate) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = editedDate,
                onValueChange = { onDateChange(it.take(10)) },
                label = { Text(dateLabel) },
                singleLine = true,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnnotationSectionLabel(timestampLabel)
                Text(text = timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = label,
            onValueChange = onLabelChange,
            label = { Text(labelLabel) },
            supportingText = { Text(labelHint) },
            singleLine = true,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnnotationSectionLabel(categoryLabel)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { option ->
                    FilterChip(
                        selected = category == option.category,
                        onClick = { onCategoryChange(option.category) },
                        label = { Text(option.label) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(option.color, RoundedCornerShape(999.dp)),
                            )
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(descriptionLabel) },
            supportingText = { Text(descriptionHint) },
            minLines = 2,
            maxLines = 4,
        )
    }
}

@Composable
private fun AnnotationSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AnnotationStatusChip(text: String) {
    AssistChip(onClick = {}, label = { Text(text) })
}

@Composable
private fun AnnotationStatusMessage(
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

private fun defaultAnnotationCategoryOptions(): List<AnnotationCategoryOption> = listOf(
    AnnotationCategoryOption(AnnotationCategory.MILESTONE, "Milestone", Color(0xFF22C55E)),
    AnnotationCategoryOption(AnnotationCategory.MAINTENANCE, "Maintenance", Color(0xFF38BDF8)),
    AnnotationCategoryOption(AnnotationCategory.TRIP, "Trip", Color(0xFFA78BFA)),
    AnnotationCategoryOption(AnnotationCategory.ISSUE, "Issue", Color(0xFFFB7185)),
    AnnotationCategoryOption(AnnotationCategory.UPGRADE, "Upgrade", Color(0xFFF59E0B)),
    AnnotationCategoryOption(AnnotationCategory.CUSTOM, "Custom", Color(0xFF94A3B8)),
)

private fun toDateInputValue(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    val direct = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    if (direct.matches(timestamp)) return timestamp
    val prefix = Regex("^(\\d{4}-\\d{2}-\\d{2})").find(timestamp)?.groupValues?.getOrNull(1)
    return prefix ?: ""
}

private fun toIsoTimestamp(date: String): String =
    if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)) "${date}T00:00:00Z" else ""
