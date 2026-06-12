package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Session monitor mode supplied by callers of [SessionExpiredModal]. */
enum class SessionAuthMode { OPEN, SESSION }

/**
 * Native parity surface for `web/src/components/feedback/SessionExpiredModal.tsx`.
 *
 * Suppresses itself in open mode, hard-blocks when the upstream session has
 * expired or an API-expired event was observed, and exposes a single sign-in
 * action. Backdrop/back dismiss is intentionally ignored while visible.
 */
@Composable
fun SessionExpiredModal(
    mode: SessionAuthMode,
    hasExpired: Boolean,
    eventTriggered: Boolean,
    onSignInAgain: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Session expired",
    body: String = "For your security, your session has timed out. Sign in again to pick up where you left off.",
    signInLabel: String = "Sign in again",
    iconContentDescription: String = "Session expired",
) {
    if (mode == SessionAuthMode.OPEN) return
    if (!hasExpired && !eventTriggered) return

    AlertDialog(
        modifier = modifier.semantics { contentDescription = title },
        onDismissRequest = {},
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .semantics { contentDescription = iconContentDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔒", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSignInAgain, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { Text(signInLabel) }
        },
    )
}
