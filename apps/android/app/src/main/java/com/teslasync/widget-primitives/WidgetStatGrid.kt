package com.teslasync.widgetprimitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Trend direction for a [StatGridItem]. */
enum class StatTrend { UP, DOWN, FLAT }

/** One stat cell in [WidgetStatGrid]. Mirrors web `StatGridItem`. */
data class StatGridItem(
    val label: String,
    val value: String,
    val unit: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val trend: StatTrend? = null,
    val trendValue: String? = null,
    val valueColor: Color? = null,
)

/**
 * `WidgetStatGrid` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetStatGrid.tsx`.
 *
 * A responsive grid of [StatCard]-style cells. The column count is auto-derived
 * from the item count (divisible-by-3 → 3, by-4 → 4, else 2) unless [cols] is
 * supplied; in `compact` mode a single column is used (parity with web's
 * container-query collapse behaviour, adapted to a fixed column model since
 * Compose has no CSS container queries).
 */
@Composable
fun WidgetStatGrid(
    stats: List<StatGridItem>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    cols: Int? = null,
    emptyMessage: String = "No stats available",
) {
    if (stats.isEmpty()) {
        WidgetEmptyState(message = emptyMessage, modifier = modifier)
        return
    }

    val resolvedCols = if (compact) 1 else (cols ?: autoCols(stats.size))
    val gap = if (compact) 8.dp else 12.dp

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        stats.chunked(resolvedCols).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                rowItems.forEach { item ->
                    StatCell(item = item, modifier = Modifier.weight(1f))
                }
                // Pad the final partial row so cells keep a consistent width.
                repeat(resolvedCols - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCell(item: StatGridItem, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            item.icon?.invoke()
            Text(
                text = item.label,
                color = muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.value,
                color = item.valueColor ?: primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            if (item.unit != null) {
                Text(text = item.unit, color = muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        if (item.trend != null && item.trendValue != null) {
            val (arrow, color) = when (item.trend) {
                StatTrend.UP -> "▲" to Color(0xFF34D399)
                StatTrend.DOWN -> "▼" to MaterialTheme.colorScheme.error
                StatTrend.FLAT -> "—" to muted
            }
            Text(text = "$arrow ${item.trendValue}", color = color, fontSize = 11.sp)
        }
    }
}

private fun autoCols(count: Int): Int = when {
    count % 3 == 0 -> 3
    count % 4 == 0 -> 4
    else -> 2
}
