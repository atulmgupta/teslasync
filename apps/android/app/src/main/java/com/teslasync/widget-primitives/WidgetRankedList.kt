package com.teslasync.widgetprimitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Badge intent for a [RankedItem]. */
enum class RankedBadgeVariant { SUCCESS, WARNING, ERROR, NEUTRAL }

/** One ranked entry in [WidgetRankedList]. Mirrors web `RankedItem`. */
data class RankedItem(
    val id: String,
    val label: String,
    val value: Double,
    val formattedValue: String,
    val badgeText: String? = null,
    val badgeVariant: RankedBadgeVariant = RankedBadgeVariant.NEUTRAL,
    val barColor: Color? = null,
)

/**
 * `WidgetRankedList` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetRankedList.tsx`.
 *
 * A leaderboard: items sorted by value descending, each row showing its rank,
 * label, optional badge and formatted value, with an optional proportional
 * background bar. Truncated to [maxItems] (default 3 compact / 5 otherwise).
 */
@Composable
fun WidgetRankedList(
    items: List<RankedItem>,
    modifier: Modifier = Modifier,
    maxItems: Int? = null,
    compact: Boolean = false,
    showBars: Boolean = true,
    emptyMessage: String = "No data available",
) {
    val limit = maxItems ?: if (compact) 3 else 5
    val hideBars = compact || !showBars

    val visible = remember(items, limit) {
        items.sortedByDescending { it.value }.take(limit)
    }
    val maxValue = remember(visible) { visible.maxOfOrNull { it.value } ?: 0.0 }

    if (visible.isEmpty()) {
        WidgetEmptyState(message = emptyMessage, modifier = modifier)
        return
    }

    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface
    val defaultBar = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        visible.forEachIndexed { index, item ->
            val barFraction = if (maxValue > 0) (item.value / maxValue).toFloat() else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (!hideBars) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(barFraction)
                            .background((item.barColor ?: defaultBar).copy(alpha = 0.15f)),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        color = muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(20.dp),
                    )
                    Text(
                        text = item.label,
                        color = primary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.badgeText != null) {
                        RankedBadgeChip(item.badgeText, item.badgeVariant)
                    }
                    Text(
                        text = item.formattedValue,
                        color = primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankedBadgeChip(text: String, variant: RankedBadgeVariant) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (variant) {
        RankedBadgeVariant.SUCCESS -> Color(0x1A10B981) to Color(0xFF34D399)
        RankedBadgeVariant.WARNING -> Color(0x1AF59E0B) to Color(0xFFFBBF24)
        RankedBadgeVariant.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        RankedBadgeVariant.NEUTRAL -> scheme.surfaceVariant to scheme.onSurfaceVariant
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
