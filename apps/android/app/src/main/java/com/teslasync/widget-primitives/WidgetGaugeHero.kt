package com.teslasync.widgetprimitives

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Gauge configuration for [WidgetGaugeHero]. Mirrors web `GaugeHeroConfig`. */
data class GaugeHeroConfig(
    val value: Double,
    val max: Double,
    val label: String,
    val unit: String,
    val color: Color,
)

/** A small stat shown beneath the gauge. Mirrors web `GaugeHeroStat`. */
data class GaugeHeroStat(
    val label: String,
    val value: String,
    val unit: String? = null,
)

/**
 * `WidgetGaugeHero` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetGaugeHero.tsx`.
 *
 * A radial gauge (drawn natively with Canvas — no charting dependency) with a
 * centered value/unit/label, plus an optional row of supporting stats and an
 * optional content slot. In `compact` mode the gauge shrinks and the stats /
 * children are hidden (parity with web).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WidgetGaugeHero(
    gauge: GaugeHeroConfig,
    modifier: Modifier = Modifier,
    stats: List<GaugeHeroStat> = emptyList(),
    compact: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val size = if (compact) 70.dp else 100.dp
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.surfaceVariant

    val fraction = if (gauge.max > 0) (gauge.value / gauge.max).coerceIn(0.0, 1.0) else 0.0
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.toFloat(),
        label = "WidgetGaugeHero.sweep",
    )

    Column(
        modifier = modifier.semantics {
            contentDescription = "${gauge.label}: ${gauge.value} ${gauge.unit}"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = this.size.minDimension * 0.12f
                val inset = strokeWidth / 2f
                val arcSize = androidx.compose.ui.geometry.Size(
                    this.size.width - strokeWidth,
                    this.size.height - strokeWidth,
                )
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                // 270° gauge (parity with RadialGauge): start at 135°, sweep 270°.
                drawArc(
                    color = track,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = gauge.color,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatGauge(gauge.value),
                    color = primary,
                    fontSize = if (compact) 16.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = gauge.unit, color = muted, fontSize = 10.sp)
            }
        }

        if (!compact) {
            Text(text = gauge.label, color = muted, fontSize = 11.sp)
        }

        if (!compact && stats.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                stats.forEach { stat ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stat.label,
                            color = muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stat.value,
                                color = primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (stat.unit != null) {
                                Text(
                                    text = stat.unit,
                                    color = muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!compact && content != null) content()
    }
}

private fun formatGauge(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else (Math.round(value * 10) / 10.0).toString()
