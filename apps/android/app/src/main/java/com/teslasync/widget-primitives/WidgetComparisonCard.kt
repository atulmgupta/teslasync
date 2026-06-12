package com.teslasync.widgetprimitives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * One comparison row for [WidgetComparisonCard]. Mirrors the web
 * `ComparisonMetric` interface.
 *
 * @param higherIsBetter when true an increase is shown as positive (green);
 *   when false a decrease is positive. Parity with web `Delta` direction.
 */
data class ComparisonMetric(
    val label: String,
    val current: Double,
    val previous: Double,
    val formattedCurrent: String,
    val unit: String? = null,
    val higherIsBetter: Boolean = true,
)

/**
 * `WidgetComparisonCard` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetComparisonCard.tsx`.
 *
 * A list of metrics each showing the current value and a percent delta vs the
 * previous period, colored by whether the change is an improvement. In
 * `compact` mode only the first two metrics are shown (parity with web slice).
 */
@Composable
fun WidgetComparisonCard(
    metrics: List<ComparisonMetric>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    emptyMessage: String = "No comparison data",
) {
    val visible = if (compact) metrics.take(2) else metrics

    if (visible.isEmpty()) {
        Text(
            text = emptyMessage,
            modifier = modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Column(modifier = modifier) {
        visible.forEachIndexed { index, metric ->
            ComparisonRow(metric)
            if (index < visible.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun ComparisonRow(metric: ComparisonMetric) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface

    val pctChange = if (metric.previous != 0.0) {
        (metric.current - metric.previous) / abs(metric.previous) * 100.0
    } else {
        0.0
    }
    val improved = if (metric.higherIsBetter) pctChange >= 0 else pctChange <= 0
    val deltaColor: Color = when {
        pctChange == 0.0 -> muted
        improved -> Color(0xFF34D399)
        else -> MaterialTheme.colorScheme.error
    }
    val arrow = when {
        pctChange > 0 -> "▲"
        pctChange < 0 -> "▼"
        else -> "—"
    }
    val deltaText = "$arrow ${formatNumber(abs(pctChange), 1)}%"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric.label,
                color = muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = metric.formattedCurrent,
                    color = primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metric.unit != null) {
                    Text(
                        text = metric.unit,
                        color = muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }
        }
        Text(
            text = deltaText,
            color = deltaColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatNumber(value: Double, decimals: Int): String {
    val factor = generateSequence(1.0) { it * 10 }.elementAt(decimals)
    val rounded = Math.round(value * factor) / factor
    val s = rounded.toString()
    val dot = s.indexOf('.')
    return when {
        decimals <= 0 -> Math.round(value).toString()
        dot < 0 -> "$s.${"0".repeat(decimals)}"
        else -> {
            val frac = s.length - dot - 1
            if (frac >= decimals) s.substring(0, dot + 1 + decimals)
            else s + "0".repeat(decimals - frac)
        }
    }
}
