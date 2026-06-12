package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Data readiness branch rendered by [HelixConfirmDialog]. */
enum class HelixConfirmDialogState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

/** Tool metadata supplied by the paused Helix dispatcher action. */
data class HelixToolPreview(
    val name: String,
    val description: String? = null,
    val mutates: Boolean,
)

/**
 * Native Android parity for `web/src/components/ai/ConfirmDialog.tsx`.
 *
 * Presents the paused Helix tool call, its verbatim argument JSON, and explicit
 * Approve / Cancel decisions. The view is self-contained and presentational:
 * callers provide all strings, state, and callbacks.
 */
@Composable
fun HelixConfirmDialog(
    open: Boolean,
    tool: HelixToolPreview?,
    argsJson: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    state: HelixConfirmDialogState = HelixConfirmDialogState.CONTENT,
    loading: Boolean = false,
    title: String = "Approve Helix action",
    mutatingIntro: String = "The assistant wants to make a change to your data. Review what it will do, then approve or cancel.",
    readIntro: String = "The assistant wants to run a tool. Review the inputs, then approve or cancel.",
    toolLabel: String = "Tool",
    argsLabel: String = "Arguments",
    approveLabel: String = "Approve",
    cancelLabel: String = "Cancel",
    loadingLabel: String = "Loading action details…",
    emptyLabel: String = "No action details are available.",
    errorLabel: String? = null,
    retryLabel: String = "Retry",
    staleLabel: String = "Action details may be out of date.",
    offlineLabel: String = "Offline; showing cached action details.",
    dialogContentDescription: String = title,
    onRetry: (() -> Unit)? = null,
) {
    if (!open) return

    Dialog(onDismissRequest = { if (!loading) onCancel() }) {
        Surface(
            modifier = modifier
                .widthIn(max = 560.dp)
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
                    HelixConfirmDialogState.LOADING -> DialogStatusMessage(loadingLabel, showSpinner = true)
                    HelixConfirmDialogState.EMPTY -> DialogStatusMessage(emptyLabel)
                    HelixConfirmDialogState.ERROR -> DialogStatusMessage(
                        message = errorLabel ?: emptyLabel,
                        isError = true,
                        actionLabel = retryLabel,
                        onAction = onRetry,
                    )
                    HelixConfirmDialogState.STALE -> {
                        DialogStatusChip(staleLabel)
                        HelixConfirmDialogContent(tool, argsJson, mutatingIntro, readIntro, toolLabel, argsLabel)
                    }
                    HelixConfirmDialogState.OFFLINE -> {
                        DialogStatusChip(offlineLabel)
                        HelixConfirmDialogContent(tool, argsJson, mutatingIntro, readIntro, toolLabel, argsLabel)
                    }
                    HelixConfirmDialogState.CONTENT -> HelixConfirmDialogContent(
                        tool = tool,
                        argsJson = argsJson,
                        mutatingIntro = mutatingIntro,
                        readIntro = readIntro,
                        toolLabel = toolLabel,
                        argsLabel = argsLabel,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onCancel, enabled = !loading) { Text(cancelLabel) }
                    Spacer(Modifier.widthIn(min = 8.dp))
                    Button(onClick = onConfirm, enabled = !loading && tool != null && state != HelixConfirmDialogState.LOADING) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(16.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(approveLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelixConfirmDialogContent(
    tool: HelixToolPreview?,
    argsJson: String,
    mutatingIntro: String,
    readIntro: String,
    toolLabel: String,
    argsLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (tool?.mutates == true) mutatingIntro else readIntro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel(toolLabel)
            Text(
                text = tool?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!tool?.description.isNullOrBlank()) {
                Text(
                    text = tool.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel(argsLabel)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    text = argsJson,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DialogStatusChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun DialogStatusMessage(
    message: String,
    showSpinner: Boolean = false,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(20.dp),
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
