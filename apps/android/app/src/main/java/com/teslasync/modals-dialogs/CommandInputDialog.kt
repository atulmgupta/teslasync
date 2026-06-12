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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Validation modes supported by command input fields. */
enum class CommandInputValidation { TEXT, PIN, NUMBER, DECIMAL }

/** Single input field definition for multi-field commands. */
data class CommandInputField(
    val name: String,
    val label: String,
    val assistiveText: String? = null,
    val validation: CommandInputValidation = CommandInputValidation.TEXT,
    val min: Double? = null,
    val max: Double? = null,
)

/** Immutable command input metadata projected from the command registry. */
data class CommandInputSpec(
    val label: String,
    val prompt: String,
    val paramName: String,
    val assistiveText: String? = null,
    val defaultValue: String = "",
    val validation: CommandInputValidation = CommandInputValidation.TEXT,
    val min: Double? = null,
    val max: Double? = null,
    val fields: List<CommandInputField>? = null,
)

/**
 * Native Android parity surface for the web `CommandInputDialog`.
 *
 * Renders either a single command parameter input or a multi-field command form,
 * validates required, PIN, integer and decimal constraints, and submits only
 * valid values.
 */
@Composable
fun CommandInputDialog(
    open: Boolean,
    spec: CommandInputSpec,
    onSubmit: (Map<String, String>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    requiredErrorText: String = "Required",
    pinErrorText: String = "Enter a 4-digit PIN",
    wholeNumberErrorText: String = "Enter a whole number",
    validNumberErrorText: String = "Enter a valid number",
    minimumText: String = "Minimum: {min}",
    maximumText: String = "Maximum: {max}",
    cancelText: String = "Cancel",
    sendText: String = "Send",
) {
    if (!open) return
    val fieldDefs = spec.fields ?: listOf(
        CommandInputField(
            name = spec.paramName,
            label = spec.assistiveText ?: spec.label,
            assistiveText = spec.assistiveText,
            validation = spec.validation,
            min = spec.min,
            max = spec.max,
        ),
    )
    fun initialValues(): Map<String, String> = fieldDefs.associate { field ->
        field.name to if (spec.fields == null && field.name == spec.paramName) spec.defaultValue else ""
    }
    var values by remember(open, spec) { mutableStateOf(initialValues()) }
    var touched by remember(open, spec) { mutableStateOf(emptySet<String>()) }

    fun errorFor(field: CommandInputField): String? = validateField(
        value = values[field.name].orEmpty(),
        validation = field.validation,
        min = field.min,
        max = field.max,
        required = requiredErrorText,
        pin = pinErrorText,
        whole = wholeNumberErrorText,
        valid = validNumberErrorText,
        minimum = minimumText,
        maximum = maximumText,
    )
    val allValid = fieldDefs.all { errorFor(it) == null }

    Dialog(onDismissRequest = onClose) {
        Surface(modifier = modifier.semantics { contentDescription = spec.label }, shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("⌁", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text(spec.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(spec.prompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    fieldDefs.forEach { field ->
                        val error = if (field.name in touched) errorFor(field) else null
                        OutlinedTextField(
                            value = values[field.name].orEmpty(),
                            onValueChange = { next -> values = values + (field.name to next) },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = field.label },
                            label = { Text(field.label) },
                            supportingText = { Text(error ?: field.assistiveText.orEmpty()) },
                            isError = error != null,
                            singleLine = true,
                            visualTransformation = if (field.validation == CommandInputValidation.PIN) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType(field.validation), imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { touched = touched + field.name }),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onClose) { Text(cancelText) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = allValid && !loading,
                        onClick = {
                            touched = fieldDefs.map { it.name }.toSet()
                            if (fieldDefs.all { errorFor(it) == null }) onSubmit(values)
                        },
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(sendText)
                    }
                }
            }
        }
    }
}

private fun validateField(
    value: String,
    validation: CommandInputValidation,
    min: Double?,
    max: Double?,
    required: String,
    pin: String,
    whole: String,
    valid: String,
    minimum: String,
    maximum: String,
): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return required
    return when (validation) {
        CommandInputValidation.PIN -> if (Regex("^\\d{4}$").matches(trimmed)) null else pin
        CommandInputValidation.NUMBER -> {
            val number = trimmed.toIntOrNull() ?: return whole
            if (number.toString() != trimmed) return whole
            rangeError(number.toDouble(), min, max, minimum, maximum)
        }
        CommandInputValidation.DECIMAL -> {
            val number = trimmed.toDoubleOrNull() ?: return valid
            rangeError(number, min, max, minimum, maximum)
        }
        CommandInputValidation.TEXT -> null
    }
}

private fun rangeError(value: Double, min: Double?, max: Double?, minimum: String, maximum: String): String? = when {
    min != null && value < min -> minimum.replace("{min}", trimNumber(min))
    max != null && value > max -> maximum.replace("{max}", trimNumber(max))
    else -> null
}

private fun keyboardType(validation: CommandInputValidation): KeyboardType = when (validation) {
    CommandInputValidation.PIN, CommandInputValidation.NUMBER -> KeyboardType.Number
    CommandInputValidation.DECIMAL -> KeyboardType.Decimal
    CommandInputValidation.TEXT -> KeyboardType.Text
}

private fun trimNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
