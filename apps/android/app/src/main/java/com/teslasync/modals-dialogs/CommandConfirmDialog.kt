package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/** Immutable command confirmation metadata projected from the command registry. */
data class CommandConfirmSpec(
    val label: String,
    val confirmMessage: String = "Are you sure?",
    val countdownSeconds: Int = 0,
    val confirmInput: String? = null,
)

/**
 * Native Android parity surface for the web `CommandConfirmDialog`.
 *
 * Displays the destructive command warning, optional countdown, optional typed
 * confirmation word, cancel and confirm actions.
 */
@Composable
fun CommandConfirmDialog(
    open: Boolean,
    spec: CommandConfirmSpec,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    typeToConfirmText: String = "Type \"{word}\" to confirm:",
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
    warningContentDescription: String = "Warning",
) {
    if (!open) return
    var remaining by remember(open, spec.countdownSeconds) { mutableIntStateOf(spec.countdownSeconds.coerceAtLeast(0)) }
    var inputValue by remember(open, spec.confirmInput) { mutableStateOf("") }
    LaunchedEffect(open, spec.countdownSeconds) {
        remaining = spec.countdownSeconds.coerceAtLeast(0)
        inputValue = ""
        while (open && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    val expected = spec.confirmInput
    val canConfirm = remaining == 0 && (expected == null || inputValue.trim().uppercase() == expected.uppercase()) && !loading

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = modifier.semantics { contentDescription = spec.label },
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(14.dp))
                            .semantics { contentDescription = warningContentDescription },
                        contentAlignment = Alignment.Center,
                    ) { Text("!", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold) }
                    Text(spec.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(spec.confirmMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                if (expected != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(typeToConfirmText.replace("{word}", expected), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = expected },
                            label = { Text(expected) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm() }),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onClose) { Text(cancelText) }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = canConfirm, onClick = onConfirm) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (remaining > 0) "$confirmText (${remaining}s)" else confirmText)
                    }
                }
            }
        }
    }
}
