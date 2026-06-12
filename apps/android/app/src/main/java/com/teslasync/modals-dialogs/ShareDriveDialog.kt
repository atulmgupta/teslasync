package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Request payload for creating a drive share link. */
data class ShareDriveCreateRequest(
    val title: String?,
    val includeSpeed: Boolean,
    val includeTelemetry: Boolean,
    val expiresInDays: Int?,
)

/** Existing public share link row, already projected for native rendering. */
data class ShareDriveLink(
    val id: String,
    val token: String,
    val title: String?,
    val views: Int,
    val expiresAtLabel: String?,
    val isExpired: Boolean = false,
)

/** Native state wrapper for share-link queries and mutations. */
data class ShareDriveUiState(
    val sharesLoading: Boolean = false,
    val createLoading: Boolean = false,
    val errorMessage: String? = null,
    val stale: Boolean = false,
    val offline: Boolean = false,
)

/**
 * Native Android parity surface for the web `ShareDriveDialog`.
 *
 * Lets the user create a public drive report link, copy or open the generated
 * link, create another link, and review or revoke existing active share links.
 */
@Composable
fun ShareDriveDialog(
    open: Boolean,
    driveId: String,
    existingShares: List<ShareDriveLink>,
    shareUrlForToken: (String) -> String,
    onCreateShare: (ShareDriveCreateRequest) -> String?,
    onCopyLink: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onRevokeShare: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: ShareDriveUiState = ShareDriveUiState(),
    onRetry: (() -> Unit)? = null,
    titleText: String = "Share Drive",
    descriptionText: String = "Generate a public link to share this drive report. Anyone with the link can view the map, stats, and charts — no login required.",
    optionalTitleText: String = "Optional title (e.g., \"SF to LA Road Trip\")",
    includeSpeedText: String = "Include speed data",
    includeTelemetryText: String = "Include detailed telemetry (battery, power)",
    expiryText: String = "Link expires after",
    sevenDaysText: String = "7 days",
    thirtyDaysText: String = "30 days",
    ninetyDaysText: String = "90 days",
    neverText: String = "Never",
    generateText: String = "Generate Link",
    createdText: String = "Share link created!",
    copyText: String = "Copy Link",
    openLinkText: String = "Open link",
    createAnotherText: String = "Create another link",
    existingText: String = "Active Share Links",
    untitledText: String = "Untitled share",
    viewsText: String = "views",
    expiredText: String = "Expired",
    expiresOnText: String = "Expires {date}",
    noExpiryText: String = "No expiry",
    copyLinkText: String = "Copy link",
    revokeText: String = "Revoke",
    emptySharesText: String = "No active share links yet.",
    staleText: String = "Stale",
    offlineText: String = "Offline",
    loadingText: String = "Loading share links",
    retryText: String = "Retry",
    closeText: String = "Close",
) {
    if (!open) return
    var generatedUrl by remember(open, driveId) { mutableStateOf<String?>(null) }
    var includeSpeed by remember(open) { mutableStateOf(true) }
    var includeTelemetry by remember(open) { mutableStateOf(false) }
    var expiryDays by remember(open) { mutableStateOf("30") }
    var reportTitle by remember(open) { mutableStateOf("") }

    Dialog(onDismissRequest = { generatedUrl = null; reportTitle = ""; onClose() }) {
        Surface(modifier = modifier.semantics { contentDescription = titleText }, shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(titleText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { generatedUrl = null; reportTitle = ""; onClose() }) { Text(closeText) }
                }
                StateChip(state, staleText, offlineText)
                if (state.errorMessage != null) ErrorBlock(state.errorMessage, retryText, onRetry)
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val url = generatedUrl
                    if (url == null) {
                        Text(descriptionText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = reportTitle,
                            onValueChange = { reportTitle = it },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = optionalTitleText },
                            label = { Text(optionalTitleText) },
                            singleLine = true,
                        )
                        SwitchRow(includeSpeedText, includeSpeed) { includeSpeed = it }
                        SwitchRow(includeTelemetryText, includeTelemetry) { includeTelemetry = it }
                        SelectRow(expiryText, expiryDays, listOf("7" to sevenDaysText, "30" to thirtyDaysText, "90" to ninetyDaysText, "0" to neverText)) { expiryDays = it }
                        Button(
                            enabled = !state.createLoading,
                            onClick = {
                                val request = ShareDriveCreateRequest(
                                    title = reportTitle.trim().ifEmpty { null },
                                    includeSpeed = includeSpeed,
                                    includeTelemetry = includeTelemetry,
                                    expiresInDays = expiryDays.toIntOrNull()?.takeIf { it > 0 },
                                )
                                generatedUrl = onCreateShare(request)
                            },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = generateText },
                        ) {
                            if (state.createLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("🔗")
                            Spacer(Modifier.width(8.dp))
                            Text(generateText)
                        }
                    } else {
                        Text(createdText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        OutlinedTextField(value = url, onValueChange = {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onCopyLink(url) }, modifier = Modifier.weight(1f)) { Text(copyText) }
                            OutlinedButton(onClick = { onOpenLink(url) }, modifier = Modifier.semantics { contentDescription = openLinkText }) { Text("↗") }
                        }
                        TextButton(onClick = { generatedUrl = null }, modifier = Modifier.fillMaxWidth()) { Text(createAnotherText) }
                    }

                    Text(existingText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    when {
                        state.sharesLoading -> LoadingBlock(loadingText)
                        existingShares.isEmpty() -> Text(emptySharesText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> existingShares.forEach { share ->
                            ShareRow(
                                share = share,
                                url = shareUrlForToken(share.token),
                                onCopy = onCopyLink,
                                onRevoke = onRevokeShare,
                                untitledText = untitledText,
                                viewsText = viewsText,
                                expiredText = expiredText,
                                expiresOnText = expiresOnText,
                                noExpiryText = noExpiryText,
                                copyText = copyLinkText,
                                revokeText = revokeText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SelectRow(label: String, value: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == value }?.second ?: value
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(current) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (optionValue, optionLabel) ->
                    DropdownMenuItem(text = { Text(optionLabel) }, onClick = { expanded = false; onSelected(optionValue) })
                }
            }
        }
    }
}

@Composable
private fun ShareRow(
    share: ShareDriveLink,
    url: String,
    onCopy: (String) -> Unit,
    onRevoke: (String) -> Unit,
    untitledText: String,
    viewsText: String,
    expiredText: String,
    expiresOnText: String,
    noExpiryText: String,
    copyText: String,
    revokeText: String,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(share.title ?: untitledText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val expiry = when {
                    share.isExpired -> expiredText
                    share.expiresAtLabel != null -> expiresOnText.replace("{date}", share.expiresAtLabel)
                    else -> noExpiryText
                }
                Text("👁 ${share.views} $viewsText · $expiry", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { onCopy(url) }, modifier = Modifier.semantics { contentDescription = copyText }) { Text("⧉") }
            TextButton(onClick = { onRevoke(share.token) }, modifier = Modifier.semantics { contentDescription = revokeText }) { Text("🗑") }
        }
    }
}

@Composable
private fun StateChip(state: ShareDriveUiState, stale: String, offline: String) {
    val label = when {
        state.offline -> offline
        state.stale -> stale
        else -> null
    } ?: return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LoadingBlock(text: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorBlock(message: String, retry: String, onRetry: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) TextButton(onClick = onRetry) { Text(retry) }
    }
}
