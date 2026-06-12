package com.teslasync.widgetprimitives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Badge intent for a [DetailEntry] — parity with web `badge.variant`. */
enum class DetailBadgeVariant { SUCCESS, WARNING, ERROR, NEUTRAL }

/** A label/value (+ optional badge) row in [WidgetDetailCard]. */
data class DetailEntry(
    val label: String,
    val value: String?,
    val badgeText: String? = null,
    val badgeVariant: DetailBadgeVariant = DetailBadgeVariant.NEUTRAL,
    val mono: Boolean = false,
)

/**
 * `WidgetDetailCard` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetDetailCard.tsx`.
 *
 * A vertical key/value list with optional per-row badges and monospaced
 * values. In `compact` mode only the first four entries are shown (parity
 * with web slice). Renders [WidgetEmptyState] when there are no entries.
 */
@Composable
fun WidgetDetailCard(
    entries: List<DetailEntry>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    emptyMessage: String = "No details available",
) {
    if (entries.isEmpty()) {
        WidgetEmptyState(message = emptyMessage, modifier = modifier)
        return
    }

    val visible = if (compact) entries.take(4) else entries
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        visible.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.label.uppercase(),
                    color = muted,
                    fontSize = 10.sp,
                    letterSpacing = 0.06.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.value ?: "—",
                        color = primary,
                        fontSize = 14.sp,
                        fontFamily = if (entry.mono) FontFamily.Monospace else FontFamily.Default,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.badgeText != null) {
                        DetailBadgeChip(entry.badgeText, entry.badgeVariant)
                    }
                }
            }
            if (index < visible.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun DetailBadgeChip(text: String, variant: DetailBadgeVariant) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (variant) {
        DetailBadgeVariant.SUCCESS -> Color(0x1A10B981) to Color(0xFF34D399)
        DetailBadgeVariant.WARNING -> Color(0x1AF59E0B) to Color(0xFFFBBF24)
        DetailBadgeVariant.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        DetailBadgeVariant.NEUTRAL -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
