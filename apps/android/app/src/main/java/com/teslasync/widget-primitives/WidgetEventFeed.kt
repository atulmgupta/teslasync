package com.teslasync.widgetprimitives

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Severity tint for an [EventFeedItem] — parity with web `severity`. */
enum class EventSeverity { INFO, WARNING, CRITICAL }

/** One row in [WidgetEventFeed]. Mirrors the web `EventFeedItem` interface. */
data class EventFeedItem(
    val id: String,
    val title: String,
    val timestampMillis: Long,
    val dotColor: Color,
    val subtitle: String? = null,
    val severity: EventSeverity = EventSeverity.INFO,
    val icon: (@Composable () -> Unit)? = null,
    /** Optional drill-through; when set the whole row is clickable. */
    val onClick: (() -> Unit)? = null,
)

/**
 * `WidgetEventFeed` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetEventFeed.tsx`.
 *
 * A reverse-chronological timeline of events. Items are sorted newest-first
 * and truncated to [maxItems] (default 3 in compact, 10 otherwise — parity
 * with web). Relative time formatting is delegated to [formatTime] so all
 * date strings flow through the caller's i18n/date facade (parity with the
 * web `useDateFormat` hook).
 */
@Composable
fun WidgetEventFeed(
    items: List<EventFeedItem>,
    formatTime: (epochMillis: Long) -> String,
    modifier: Modifier = Modifier,
    maxItems: Int? = null,
    compact: Boolean = false,
    emptyMessage: String = "No events yet",
) {
    val limit = maxItems ?: if (compact) 3 else 10
    val sorted = remember(items, limit) {
        items.sortedByDescending { it.timestampMillis }.take(limit)
    }

    if (sorted.isEmpty()) {
        WidgetEmptyState(message = emptyMessage, modifier = modifier)
        return
    }

    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        sorted.forEach { item ->
            val rowModifier = Modifier
                .fillMaxWidth()
                .let { if (item.onClick != null) it.clickable(onClick = item.onClick) else it }
                .padding(vertical = 8.dp)
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(item.dotColor),
                ) {
                    item.icon?.invoke()
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.subtitle != null) {
                        Text(
                            text = item.subtitle,
                            color = muted,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = formatTime(item.timestampMillis),
                    color = muted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
