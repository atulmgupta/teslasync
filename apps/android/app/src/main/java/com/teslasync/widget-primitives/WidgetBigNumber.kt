package com.teslasync.widgetprimitives

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

/**
 * Severity / badge intent for [WidgetBigNumber], mirroring the web
 * `badge.variant` union (`success | warning | error | neutral`).
 */
enum class BigNumberBadgeVariant { SUCCESS, WARNING, ERROR, NEUTRAL }

/** A small chip rendered under the value. Parity with web `badge` prop. */
data class BigNumberBadge(
    val text: String,
    val variant: BigNumberBadgeVariant,
)

/**
 * `WidgetBigNumber` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetBigNumber.tsx`.
 *
 * A centered hero value with an optional unit, label, subtitle and badge.
 * Pure presentational primitive: all display strings are injected by the
 * caller (resolved through the P1/S10 i18n facade) so the primitive itself
 * contains no user-facing English literals.
 *
 * @param value the numeric value, or `null` to render [nullDisplay].
 * @param unit unit suffix shown next to the value (already localized/SI-formatted).
 * @param label small uppercase caption under the value.
 * @param subtitle secondary line under the label.
 * @param badge optional status chip.
 * @param valueColor color of the value text; defaults to the theme primary text color.
 * @param nullDisplay placeholder shown when [value] is `null`.
 * @param animated when true the value counts up to its target (parity with web `AnimatedNumber`).
 * @param decimals number of fraction digits to render for the value.
 */
@Composable
fun WidgetBigNumber(
    value: Double?,
    modifier: Modifier = Modifier,
    unit: String? = null,
    label: String? = null,
    subtitle: String? = null,
    badge: BigNumberBadge? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    nullDisplay: String = "—",
    animated: Boolean = true,
    decimals: Int = 0,
) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val target = (value ?: 0.0).toFloat()
    val animatedValue by animateFloatAsState(
        targetValue = target,
        label = "WidgetBigNumber.value",
    )
    val display: String = when {
        value == null -> nullDisplay
        animated -> formatNumber(animatedValue.toDouble(), decimals)
        else -> formatNumber(value, decimals)
    }

    // A single spoken description for accessibility so TalkBack reads the
    // composed metric as one phrase rather than disjoint fragments.
    val spoken = buildString {
        append(if (value == null) nullDisplay else formatNumber(value, decimals))
        if (unit != null) append(" ").append(unit)
        if (label != null) append(", ").append(label)
        if (subtitle != null) append(", ").append(subtitle)
        if (badge != null) append(", ").append(badge.text)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = display,
                color = if (value == null) mutedColor else valueColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    color = mutedColor,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        if (label != null) {
            Text(
                text = label.uppercase(),
                color = mutedColor,
                fontSize = 10.sp,
                letterSpacing = 0.08.sp,
            )
        }

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = mutedColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (badge != null) {
            BigNumberBadgeChip(badge)
        }
    }
}

@Composable
private fun BigNumberBadgeChip(badge: BigNumberBadge) {
    val (container, content) = badgeColors(badge.variant)
    androidx.compose.material3.Surface(
        color = container,
        contentColor = content,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    ) {
        Text(
            text = badge.text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun badgeColors(variant: BigNumberBadgeVariant): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (variant) {
        BigNumberBadgeVariant.SUCCESS -> Color(0x1A10B981) to Color(0xFF34D399)
        BigNumberBadgeVariant.WARNING -> Color(0x1AF59E0B) to Color(0xFFFBBF24)
        BigNumberBadgeVariant.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        BigNumberBadgeVariant.NEUTRAL -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
}

/** Formats a double with a fixed number of fraction digits without locale surprises. */
private fun formatNumber(value: Double, decimals: Int): String =
    if (decimals <= 0) value.roundToLong().toString()
    else {
        val factor = generateSequence(1.0) { it * 10 }.elementAt(decimals)
        val rounded = (value * factor).roundToLong() / factor
        rounded.toString().let { s ->
            val dot = s.indexOf('.')
            if (dot < 0) "$s.${"0".repeat(decimals)}"
            else {
                val frac = s.length - dot - 1
                if (frac >= decimals) s.substring(0, dot + 1 + decimals)
                else s + "0".repeat(decimals - frac)
            }
        }
    }
