package com.teslasync.widgetprimitives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Status level for a [StatusCell]. Mirrors web `StatusCell.status`. */
enum class CellStatus { OK, WARNING, ERROR, INACTIVE, UNKNOWN }

/** One status tile in [WidgetStatusGrid]. Mirrors web `StatusCell`. */
data class StatusCell(
    val id: String,
    val label: String,
    val status: CellStatus,
    val value: String? = null,
    val icon: (@Composable () -> Unit)? = null,
)

/**
 * `WidgetStatusGrid` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetStatusGrid.tsx`.
 *
 * A grid of status tiles, each tinted by its [CellStatus] with a corner dot.
 * Column count is [cols] (2/3/4), collapsing to 2 in `compact` mode.
 */
@Composable
fun WidgetStatusGrid(
    cells: List<StatusCell>,
    modifier: Modifier = Modifier,
    cols: Int = 2,
    compact: Boolean = false,
    emptyMessage: String = "No status data available",
) {
    if (cells.isEmpty()) {
        WidgetEmptyState(message = emptyMessage, modifier = modifier)
        return
    }

    val resolvedCols = if (compact) 2 else cols

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(resolvedCols).forEach { rowCells ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCells.forEach { cell ->
                    StatusTile(cell = cell, compact = compact, modifier = Modifier.weight(1f))
                }
                repeat(resolvedCols - rowCells.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatusTile(cell: StatusCell, compact: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, dot, borderC) = when (cell.status) {
        CellStatus.OK -> Triple(Color(0x1A10B981), Color(0xFF10B981), Color(0x3310B981))
        CellStatus.WARNING -> Triple(Color(0x1AF59E0B), Color(0xFFF59E0B), Color(0x33F59E0B))
        CellStatus.ERROR -> Triple(Color(0x1AEF4444), Color(0xFFEF4444), Color(0x33EF4444))
        CellStatus.INACTIVE, CellStatus.UNKNOWN ->
            Triple(scheme.surfaceVariant.copy(alpha = 0.3f), scheme.surfaceVariant, scheme.outlineVariant.copy(alpha = 0.4f))
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderC, RoundedCornerShape(8.dp))
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(8.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cell.icon?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cell.label,
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compact && cell.value != null) {
                    Text(
                        text = cell.value,
                        color = scheme.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
