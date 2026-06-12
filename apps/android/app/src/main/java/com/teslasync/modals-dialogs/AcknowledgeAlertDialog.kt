package com.teslasync.modalsdialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Display copy for [AcknowledgeAlertDialog], resolved by the caller's i18n layer. */
data class AcknowledgeAlertDialogText(
    val title: String = "Acknowledge alert",
    val noteLabel: String = "Note (optional)",
    val noteAssistiveText: String = "Optional: what's being done?",
    val noteLimitHint: String = "Up to 1000 characters. Shared in the audit timeline.",
    val cancel: String = "Cancel",
    val submit: String = "Acknowledge",
    val submitting: String = "Acknowledging",
    val closeContentDescription: String = "Close",
)

/**
 * `AcknowledgeAlertDialog` — native parity for the admin alert acknowledge modal.
 *
 * Resets local note state whenever reopened, accepts an empty trimmed note,
 * blocks dismiss/actions while [submitting] is true, and disables submit when
 * the trimmed note exceeds [noteMaxCharacters]. The parent owns mutations.
 */
@Composable
fun AcknowledgeAlertDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    submitting: Boolean = false,
    alertTitle: String? = null,
    strings: AcknowledgeAlertDialogText = AcknowledgeAlertDialogText(),
    noteMaxCharacters: Int = 1000,
) {
    var note by remember { mutableStateOf("") }
    LaunchedEffect(visible) {
        if (visible) note = ""
    }

    val trimmed = note.trim()
    val tooLong = trimmed.length > noteMaxCharacters
    val canAct = !submitting

    Modal(
        visible = visible,
        onDismiss = { if (canAct) onDismiss() },
        title = strings.title,
        size = ModalSize.MD,
        closeContentDescription = strings.closeContentDescription,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = strings.title },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (alertTitle != null) {
                Text(
                    text = alertTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(noteMaxCharacters + 50) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                enabled = canAct,
                label = { Text(strings.noteLabel) },
                supportingText = { Text(if (tooLong) strings.noteLimitHint else strings.noteAssistiveText) },
                isError = tooLong,
                minLines = 4,
                maxLines = 6,
            )

            Text(
                text = strings.noteLimitHint,
                color = if (tooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = canAct) { Text(strings.cancel) }
                Button(
                    onClick = { if (!tooLong && canAct) onSubmit(trimmed) },
                    enabled = canAct && !tooLong,
                ) {
                    if (submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(text = strings.submitting, modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Text(strings.submit)
                    }
                }
            }
        }
    }
}
