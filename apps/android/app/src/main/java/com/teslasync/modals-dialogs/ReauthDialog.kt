package com.teslasync.modalsdialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Mode used by [ReauthDialog], mirroring the web credential/open-mode split. */
enum class ReauthDialogMode { CREDENTIAL, CONFIRM }

/** Credential method tabs rendered by [ReauthDialog]. */
enum class ReauthMethod { PASSWORD, AUTHENTICATOR }

/** Non-network status supplied by the caller for parity with query-driven web states. */
enum class ReauthSurfaceState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

/** Server-side credential errors mapped to the same copy branches as the web dialog. */
enum class ReauthErrorKind { NOT_CONFIGURED, INVALID_PASSWORD, INVALID_TOTP, UNKNOWN }

/** Submission payload emitted by [ReauthDialog]. */
data class ReauthSubmission(
    val mode: ReauthDialogMode,
    val method: ReauthMethod? = null,
    val password: String? = null,
    val totpCode: String? = null,
)

/**
 * Native parity surface for `web/src/components/feedback/ReauthDialog.tsx`.
 *
 * Renders the sudo-style step-up dialog without networking. Callers provide the
 * resolved mode, TOTP availability, current status and submit/cancel handlers.
 * The credential branch supports password and authenticator methods; open mode
 * requires typing [confirmationToken] exactly before continuing.
 */
@Composable
fun ReauthDialog(
    open: Boolean,
    mode: ReauthDialogMode,
    path: String,
    onSubmit: (ReauthSubmission) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceState: ReauthSurfaceState = ReauthSurfaceState.CONTENT,
    totpTabAvailable: Boolean = true,
    submitting: Boolean = false,
    serverError: String? = null,
    serverErrorKind: ReauthErrorKind? = null,
    onRetry: (() -> Unit)? = null,
    confirmationToken: String = "CONFIRM",
    passwordTabLabel: String = "Password",
    authenticatorTabLabel: String = "Authenticator",
    typedConfirmationMismatchError: String = "Type CONFIRM exactly to confirm.",
    passwordRequiredError: String = "Enter your password to continue.",
    totpRequiredError: String = "Enter the 6-digit code from your authenticator.",
    notConfiguredError: String = "Step-up reauth is not configured on this server. Ask your administrator to set TESLASYNC_SUDO_PASSWORD or TESLASYNC_SUDO_TOTP_SECRET.",
    invalidPasswordError: String = "Password did not match.",
    invalidTotpError: String = "Authenticator code was rejected.",
    unknownError: String = "Reauthentication failed.",
    confirmModeTitle: String = "Confirm sensitive action",
    credentialModeTitle: String = "Confirm your identity",
    confirmModeBody: String = "This is a destructive action. Type CONFIRM to continue.",
    credentialModeBody: String = "For your security, please re-enter your password or authenticator code before this action runs.",
    reauthMethodLabel: String = "Reauth method",
    passwordLabel: String = "Password",
    authenticatorCodeLabel: String = "Authenticator code",
    typedConfirmationLabel: String = "Type CONFIRM to confirm",
    helperText: String = "Your reauth lasts 5 minutes; rapid follow-up actions will not re-prompt.",
    cancelLabel: String = "Cancel",
    continueLabel: String = "Continue",
    confirmLabel: String = "Confirm",
    loadingLabel: String = "Loading…",
    emptyLabel: String = "No reauthentication challenge is active.",
    errorLabel: String = "Could not load reauthentication options.",
    retryLabel: String = "Retry",
    staleLabel: String = "Reauthentication options may be out of date.",
    offlineLabel: String = "Offline — cached reauthentication options shown.",
) {
    if (!open) return

    var activeMethod by remember(path, open) { mutableStateOf(ReauthMethod.PASSWORD) }
    var password by remember(path, open) { mutableStateOf("") }
    var totp by remember(path, open) { mutableStateOf("") }
    var confirmText by remember(path, open) { mutableStateOf("") }
    var localError by remember(path, open) { mutableStateOf<String?>(null) }

    LaunchedEffect(totpTabAvailable, activeMethod) {
        if (!totpTabAvailable && activeMethod == ReauthMethod.AUTHENTICATOR) {
            activeMethod = ReauthMethod.PASSWORD
        }
    }

    val title = if (mode == ReauthDialogMode.CONFIRM) confirmModeTitle else credentialModeTitle
    val mappedServerError = when (serverErrorKind) {
        ReauthErrorKind.NOT_CONFIGURED -> notConfiguredError
        ReauthErrorKind.INVALID_PASSWORD -> invalidPasswordError
        ReauthErrorKind.INVALID_TOTP -> invalidTotpError
        ReauthErrorKind.UNKNOWN -> serverError ?: unknownError
        null -> serverError
    }
    val displayedError = mappedServerError ?: localError

    AlertDialog(
        modifier = modifier.semantics { contentDescription = title },
        onDismissRequest = { if (!submitting) onCancel() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusBanner(surfaceState, loadingLabel, emptyLabel, errorLabel, staleLabel, offlineLabel, onRetry, retryLabel)
                Text(
                    text = if (mode == ReauthDialogMode.CONFIRM) confirmModeBody else credentialModeBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (mode == ReauthDialogMode.CREDENTIAL) {
                    Text(reauthMethodLabel, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = activeMethod == ReauthMethod.PASSWORD,
                            onClick = { if (!submitting) activeMethod = ReauthMethod.PASSWORD },
                            label = { Text(passwordTabLabel) },
                            enabled = !submitting,
                        )
                        if (totpTabAvailable) {
                            FilterChip(
                                selected = activeMethod == ReauthMethod.AUTHENTICATOR,
                                onClick = { if (!submitting) activeMethod = ReauthMethod.AUTHENTICATOR },
                                label = { Text(authenticatorTabLabel) },
                                enabled = !submitting,
                            )
                        }
                    }
                    if (activeMethod == ReauthMethod.PASSWORD) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !submitting,
                            label = { Text(passwordLabel) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                    } else {
                        OutlinedTextField(
                            value = totp,
                            onValueChange = { next -> totp = next.filter(Char::isDigit).take(8) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !submitting,
                            label = { Text(authenticatorCodeLabel) },
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        )
                    }
                    Text(helperText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                        label = { Text(typedConfirmationLabel) },
                        singleLine = true,
                    )
                }
                if (displayedError != null) {
                    Text(displayedError.ifBlank { unknownError }, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel, enabled = !submitting) { Text(cancelLabel) }
        },
        confirmButton = {
            Button(
                enabled = !submitting && surfaceState != ReauthSurfaceState.LOADING,
                onClick = {
                    localError = null
                    if (mode == ReauthDialogMode.CONFIRM) {
                        if (confirmText.trim() != confirmationToken) {
                            localError = typedConfirmationMismatchError
                        } else {
                            onSubmit(ReauthSubmission(mode = ReauthDialogMode.CONFIRM))
                        }
                    } else if (activeMethod == ReauthMethod.PASSWORD) {
                        if (password.trim().isEmpty()) localError = passwordRequiredError
                        else onSubmit(ReauthSubmission(mode = ReauthDialogMode.CREDENTIAL, method = activeMethod, password = password))
                    } else {
                        if (totp.trim().isEmpty()) localError = totpRequiredError
                        else onSubmit(ReauthSubmission(mode = ReauthDialogMode.CREDENTIAL, method = activeMethod, totpCode = totp))
                    }
                },
            ) {
                if (submitting) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                Text(if (mode == ReauthDialogMode.CONFIRM) continueLabel else confirmLabel)
            }
        },
    )
}

@Composable
private fun StatusBanner(
    state: ReauthSurfaceState,
    loadingLabel: String,
    emptyLabel: String,
    errorLabel: String,
    staleLabel: String,
    offlineLabel: String,
    onRetry: (() -> Unit)?,
    retryLabel: String,
) {
    when (state) {
        ReauthSurfaceState.CONTENT -> Unit
        ReauthSurfaceState.LOADING -> AssistChip(onClick = {}, enabled = false, label = { Text(loadingLabel) })
        ReauthSurfaceState.EMPTY -> AssistChip(onClick = {}, enabled = false, label = { Text(emptyLabel) })
        ReauthSurfaceState.STALE -> AssistChip(onClick = { onRetry?.invoke() }, label = { Text(staleLabel) })
        ReauthSurfaceState.OFFLINE -> AssistChip(onClick = {}, enabled = false, label = { Text(offlineLabel) })
        ReauthSurfaceState.ERROR -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(errorLabel, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (onRetry != null) TextButton(onClick = onRetry) { Text(retryLabel) }
        }
    }
}
