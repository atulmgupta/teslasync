package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Saved form draft surfaced by [SessionExpiringModal]. */
data class ExpiringDraftSummary(
    val label: String,
    val savedAtMillis: Long? = null,
)

/**
 * Native parity surface for `web/src/components/feedback/SessionExpiringModal.tsx`.
 *
 * Renders only for session-mode, expiring-soon, not-yet-expired sessions. It
 * shows a live caller-supplied countdown, unsaved draft inventory, sign-out
 * affordance and a stay-signed-in action. Dismiss maps to [onStaySignedIn].
 */
@Composable
fun SessionExpiringModal(
    mode: SessionAuthMode,
    isExpiringSoon: Boolean,
    hasExpired: Boolean,
    expiresInSeconds: Int?,
    drafts: List<ExpiringDraftSummary>,
    onStaySignedIn: () -> Unit,
    onSignOutNow: () -> Unit,
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
    title: String = "Your session is about to expire",
    bodyTemplate: String = "You will be signed out in %s.",
    unsavedTitle: String = "Unsaved drafts",
    unsavedBody: String = "Sign out will keep these drafts in your browser, but you must sign in again to finish them.",
    moreDraftsTemplate: String = "+%d more",
    signOutLabel: String = "Sign out now",
    refreshingLabel: String = "Refreshing…",
    stayLabel: String = "Stay signed in",
    iconContentDescription: String = "Session countdown",
) {
    val open = mode == SessionAuthMode.SESSION && isExpiringSoon && !hasExpired
    if (!open) return

    val countdown = formatCountdown(expiresInSeconds ?: 0)

    AlertDialog(
        modifier = modifier.semantics { contentDescription = title },
        onDismissRequest = { if (!refreshing) onStaySignedIn() },
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1AF59E0B))
                            .semantics { contentDescription = iconContentDescription },
                        contentAlignment = Alignment.Center,
                    ) { Text("⏱", color = Color(0xFFFBBF24)) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(bodyTemplate.format(countdown), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (drafts.isNotEmpty()) {
                    Surface(
                        color = Color(0x0AF59E0B),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠", color = Color(0xFFFBBF24))
                                Text(unsavedTitle.uppercase(), color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(unsavedBody, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            drafts.take(5).forEach { draft ->
                                Text("• ${draft.label}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                            if (drafts.size > 5) {
                                Text(moreDraftsTemplate.format(drafts.size - 5), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSignOutNow, enabled = !refreshing) { Text(signOutLabel) }
        },
        confirmButton = {
            Button(onClick = onStaySignedIn, enabled = !refreshing) { Text(if (refreshing) refreshingLabel else stayLabel) }
        },
    )
}

private fun formatCountdown(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}
