package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

/** Visual intent for [ConfirmDialog], matching the web danger/warning variants. */
enum class ConfirmDialogVariant { DANGER, WARNING }

/**
 * Native parity surface for `web/src/components/ui/ConfirmDialog.tsx`.
 *
 * Renders a generic confirmation modal with severity styling, optional typed
 * confirmation, loading lockout, and an optional non-destructive silence choice
 * controlled by the caller. Persistence is deliberately external to keep this
 * composable presentational and network/storage free.
 */
@Composable
fun ConfirmDialog(
    open: Boolean,
    title: String,
    message: String,
    onConfirm: (dontAskAgain: Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Confirm",
    cancelLabel: String = "Cancel",
    variant: ConfirmDialogVariant = ConfirmDialogVariant.DANGER,
    loading: Boolean = false,
    requireTypedConfirmation: String? = null,
    typedConfirmationLabel: String? = null,
    silenceKey: String? = null,
    alreadySilenced: Boolean = false,
    dontAskAgainLabel: String = "Don't ask again for this action",
    typingLabelTemplate: String = "Type \"%s\" to confirm",
) {
    var typed by remember(open, requireTypedConfirmation) { mutableStateOf("") }
    var dontAskAgain by remember(open) { mutableStateOf(false) }
    val silenceHonored = silenceKey != null && variant != ConfirmDialogVariant.DANGER && requireTypedConfirmation == null

    LaunchedEffect(open, alreadySilenced, silenceHonored) {
        if (open && silenceHonored && alreadySilenced) onConfirm(false)
    }

    if (!open || (silenceHonored && alreadySilenced)) return

    val typedMatches = requireTypedConfirmation == null || typed == requireTypedConfirmation
    val confirmDisabled = loading || !typedMatches
    val colors = variantColors(variant)
    val inputLabel = typedConfirmationLabel ?: requireTypedConfirmation?.let { typingLabelTemplate.format(it) }.orEmpty()

    AlertDialog(
        modifier = modifier.semantics { contentDescription = title },
        onDismissRequest = { if (!loading) onCancel() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.first, RoundedCornerShape(10.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(if (variant == ConfirmDialogVariant.DANGER) "⛔" else "⚠", color = colors.second, fontWeight = FontWeight.Bold)
                    Text(message, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                }
                if (requireTypedConfirmation != null) {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        label = { Text(inputLabel) },
                        singleLine = true,
                    )
                }
                if (silenceHonored) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dontAskAgain, onCheckedChange = { if (!loading) dontAskAgain = it }, enabled = !loading)
                        Text(dontAskAgainLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel, enabled = !loading) { Text(cancelLabel) }
        },
        confirmButton = {
            Button(enabled = !confirmDisabled, onClick = { onConfirm(dontAskAgain && silenceHonored) }) {
                if (loading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                Text(confirmLabel)
            }
        },
    )
}

private fun variantColors(variant: ConfirmDialogVariant): Pair<Color, Color> = when (variant) {
    ConfirmDialogVariant.DANGER -> Color(0x1AF43F5E) to Color(0xFFF43F5E)
    ConfirmDialogVariant.WARNING -> Color(0x1AF59E0B) to Color(0xFFFBBF24)
}
