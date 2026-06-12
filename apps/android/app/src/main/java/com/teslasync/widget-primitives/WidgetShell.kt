package com.teslasync.widgetprimitives

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Liveness state for a widget's freshness indicator, mirroring the web
 * `DataFreshness` props (`isFetching`/`isStale`/`isError`) plus the offline
 * branch required by the P3 state matrix.
 */
enum class WidgetFreshnessState { LIVE, FETCHING, STALE, ERROR, OFFLINE }

/** Help metadata for the optional "?" tooltip next to a widget title. */
data class WidgetHelp(
    val text: String,
    val learnMore: String? = null,
)

/** Pin affordance descriptor — parity with web `<PinButton>` in the shell header. */
data class WidgetPin(
    val isPinned: Boolean,
    val onToggle: () -> Unit,
    val pinLabel: String,
    val unpinLabel: String,
)

/**
 * `WidgetShell` — native parity for
 * `web/src/features/dashboard/widgets/WidgetShell.tsx`.
 *
 * The chrome wrapper used by every dashboard widget: optional title row with
 * icon, help tooltip, freshness indicator, pin button and actions, plus the
 * scrolling content area. Reproduces the web source's three render branches —
 * `loading` → skeleton, `error` → query-error, otherwise the titled/anonymous
 * layout — and the pulse-on-update glow.
 *
 * Pure presentational primitive: every user-facing string is injected by the
 * caller (resolved via the P1/S10 i18n facade).
 *
 * @param updatedAtMillis last successful update time (ms epoch); drives the
 *   pulse-on-change glow and the freshness label.
 * @param freshnessLabel already-formatted relative time (e.g. "12s ago"); when
 *   null no freshness chip is shown.
 */
@Composable
fun WidgetShell(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: (@Composable () -> Unit)? = null,
    loading: Boolean = false,
    error: String? = null,
    noPadding: Boolean = false,
    updatedAtMillis: Long? = null,
    freshness: WidgetFreshnessState = WidgetFreshnessState.LIVE,
    freshnessLabel: String? = null,
    onRefresh: (() -> Unit)? = null,
    refreshContentDescription: String = "Refresh",
    help: WidgetHelp? = null,
    pin: WidgetPin? = null,
    errorRetryLabel: String = "Retry",
    actions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (loading) {
        WidgetSkeleton(modifier = modifier.fillMaxSize())
        return
    }
    if (error != null) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetQueryError(message = error, retryLabel = errorRetryLabel, onRetry = onRefresh)
        }
        return
    }

    // Pulse-on-change glow: when updatedAtMillis changes to a newer value we
    // flash a subtle green border for 1.5s (parity with the web box-shadow).
    var justUpdated by remember { mutableStateOf(false) }
    var prevUpdatedAt by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(updatedAtMillis) {
        val u = updatedAtMillis
        if (u != null && u > 0 && prevUpdatedAt != null && prevUpdatedAt != u) {
            justUpdated = true
            delay(1500)
            justUpdated = false
        }
        prevUpdatedAt = u
    }
    val glow by animateColorAsState(
        targetValue = if (justUpdated) Color(0x2622C55E) else Color.Transparent,
        label = "WidgetShell.glow",
    )

    val showFreshness = freshnessLabel != null
    val freshnessEl: (@Composable () -> Unit)? = if (showFreshness) {
        { WidgetFreshnessChip(label = freshnessLabel!!, state = freshness, onRefresh = onRefresh, refreshContentDescription = refreshContentDescription) }
    } else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, glow, RoundedCornerShape(12.dp)),
    ) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon?.invoke()
                    Text(
                        text = title.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.08.sp,
                    )
                    if (help != null) {
                        WidgetHelpTooltip(help = help, title = title)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    freshnessEl?.invoke()
                    if (pin != null) WidgetPinButton(pin)
                    actions?.invoke()
                }
            }
        } else {
            // Anonymous (title-less) widget: overlay freshness top-right + optional actions row.
            if (freshnessEl != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) { freshnessEl() }
                }
            }
            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) { actions() }
            }
        }

        val contentModifier = if (noPadding) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState())
        }
        Box(modifier = contentModifier) { content() }
    }
}

@Composable
private fun WidgetFreshnessChip(
    label: String,
    state: WidgetFreshnessState,
    onRefresh: (() -> Unit)?,
    refreshContentDescription: String,
) {
    val color = when (state) {
        WidgetFreshnessState.LIVE -> Color(0xFF34D399)
        WidgetFreshnessState.FETCHING -> MaterialTheme.colorScheme.primary
        WidgetFreshnessState.STALE -> Color(0xFFFBBF24)
        WidgetFreshnessState.ERROR -> MaterialTheme.colorScheme.error
        WidgetFreshnessState.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        if (onRefresh != null) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = refreshContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun WidgetHelpTooltip(help: WidgetHelp, title: String) {
    androidx.compose.material3.TooltipBox(
        positionProvider = androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { androidx.compose.material3.PlainTooltip { Text(help.text) } },
        state = androidx.compose.material3.rememberTooltipState(),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = "More info about $title" },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "?", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WidgetPinButton(pin: WidgetPin) {
    IconButton(onClick = pin.onToggle, modifier = Modifier.size(20.dp)) {
        Text(
            text = if (pin.isPinned) "★" else "☆",
            fontSize = 12.sp,
            color = if (pin.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = if (pin.isPinned) pin.unpinLabel else pin.pinLabel
            },
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 * Shared widget-primitive chrome. These are the bundle-local building blocks
 * (loading / empty / error) reused by the sibling widget primitives. They are
 * intentionally lightweight and depend only on Compose + Material 3.
 * ──────────────────────────────────────────────────────────────────────── */

/** Loading placeholder with a shimmering tonal surface (parity with web `<Skeleton>`). */
@Composable
fun WidgetSkeleton(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "skeleton.alpha",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .semantics { contentDescription = "Loading" },
    )
}

/**
 * Friendly empty state — parity with the web `<EmptyState>` from
 * `@/components/feedback`. Always renders a message; never a blank box.
 */
@Composable
fun WidgetEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .semantics { contentDescription = message },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        icon?.invoke()
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

/** Error state with an optional retry affordance — parity with web `<QueryError>`. */
@Composable
fun WidgetQueryError(
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.semantics { contentDescription = message },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
        )
        if (onRetry != null) {
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(retryLabel) }
        }
    }
}
