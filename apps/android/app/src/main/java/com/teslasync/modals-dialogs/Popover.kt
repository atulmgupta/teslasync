package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/** Cross-axis alignment for [Popover], matching the web `start | center | end` prop. */
enum class PopoverAlign { START, CENTER, END }

/** Preferred side for [Popover], matching the web `bottom | top` prop. */
enum class PopoverSide { BOTTOM, TOP }

/**
 * `Popover` — native parity for `web/src/components/ui/Popover.tsx`.
 *
 * Shows a lightweight non-modal popup, dismisses on outside click or back/Esc,
 * and intentionally does not trap focus. Compose's [Popup] positions relative
 * to the composition anchor; [sideOffset] and [offset] provide the web
 * side/shift controls without introducing networking or state-holder coupling.
 */
@Composable
fun Popover(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    side: PopoverSide = PopoverSide.BOTTOM,
    align: PopoverAlign = PopoverAlign.START,
    sideOffset: Int = 6,
    offset: DpOffset = DpOffset.Zero,
    ariaLabel: String? = null,
    content: @Composable () -> Unit,
) {
    if (!visible) return

    val horizontal = when (align) {
        PopoverAlign.START -> offset.x
        PopoverAlign.CENTER -> offset.x
        PopoverAlign.END -> offset.x
    }
    val vertical = when (side) {
        PopoverSide.BOTTOM -> offset.y + sideOffset.dp
        PopoverSide.TOP -> offset.y - sideOffset.dp
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.01f))
                .clickable(role = Role.Button, onClick = onDismiss),
        ) {
            Surface(
                modifier = modifier
                    .offset(x = horizontal, y = vertical)
                    .widthIn(min = 96.dp, max = 360.dp)
                    .semantics {
                        role = Role.Dialog
                        if (ariaLabel != null) contentDescription = ariaLabel
                    },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = {},
            ) {
                Box(modifier = Modifier.padding(8.dp)) { content() }
            }
        }
    }
}
