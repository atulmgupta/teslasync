package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Single selectable command option. */
data class CommandSelectOption(
    val value: String,
    val label: String,
    val description: String? = null,
)

/** Immutable command select metadata projected from the command registry. */
data class CommandSelectSpec(
    val label: String,
    val options: List<CommandSelectOption>,
)

/**
 * Native Android parity surface for the web `CommandSelectDialog`.
 *
 * Shows command options as full-width cards, disables selection while loading,
 * and preserves the cancel affordance from the web dialog.
 */
@Composable
fun CommandSelectDialog(
    open: Boolean,
    spec: CommandSelectSpec,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    emptyOptionsText: String = "No command options are available.",
    cancelText: String = "Cancel",
) {
    if (!open) return
    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.semantics { contentDescription = spec.label }, shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("⌁", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(spec.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (spec.options.isEmpty()) {
                        Text(emptyOptionsText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        spec.options.forEach { option ->
                            Button(
                                enabled = !loading,
                                onClick = { onSelect(option.value) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                    .semantics { contentDescription = option.label },
                            ) {
                                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.Start) {
                                    Text(option.label, fontWeight = FontWeight.Medium)
                                    if (option.description != null) {
                                        Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onClose) { Text(cancelText) }
                }
            }
        }
    }
}
