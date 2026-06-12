package com.teslasync.widgetprimitives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

/** A single label/value/unit triple displayed in the [WidgetChartSummary] stat row. */
data class ChartSummaryStat(
    val label: String,
    val value: String,
    val unit: String? = null,
)

/**
 * `WidgetChartSummary` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetChartSummary.tsx`.
 *
 * Renders a compact stat row above a chart slot. The chart is injected as a
 * composable slot ([chart]) — exactly as the web source receives `chart` as a
 * `ReactNode` — so callers supply the native chart (Vico / Canvas) without the
 * primitive depending on a charting library.
 *
 * In `compact` mode the chart is hidden and only the stat row shows (parity
 * with the web `!compact && <chart/>` branch).
 *
 * @param isEmpty when true renders [WidgetEmptyState] with [emptyMessage].
 */
@Composable
fun WidgetChartSummary(
    stats: List<ChartSummaryStat>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    isEmpty: Boolean = false,
    emptyMessage: String = "No data available",
    chart: (@Composable () -> Unit)? = null,
) {
    if (isEmpty) {
        WidgetEmptyState(message = emptyMessage, modifier = modifier)
        return
    }

    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.fillMaxSize()) {
        if (stats.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
            ) {
                stats.forEach { stat ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stat.label,
                            color = muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                            Text(
                                text = stat.value,
                                color = primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (stat.unit != null) {
                                Text(
                                    text = stat.unit,
                                    color = muted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(start = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!compact && chart != null) {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { contentDescription = "Chart" },
                verticalArrangement = Arrangement.Center,
            ) {
                chart()
            }
        }
    }
}
