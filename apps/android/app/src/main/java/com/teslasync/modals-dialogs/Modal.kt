package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Width preset for [Modal], matching the web `sm | md | lg | full` API. */
enum class ModalSize { SM, MD, LG, FULL }

/**
 * `Modal` — native parity for `web/src/components/ui/Modal.tsx`.
 *
 * Renders nothing while [visible] is false. When visible, shows a modal scrim,
 * traps system back/outside dismiss through [onDismiss], renders an optional
 * title row with a 44dp close target, and scrolls body content inside a bounded
 * Material 3 surface. All displayed strings are caller-supplied.
 */
@Composable
fun Modal(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    size: ModalSize = ModalSize.MD,
    ariaLabel: String? = null,
    closeContentDescription: String = "Close",
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f))
                .clickable(
                    enabled = dismissOnClickOutside,
                    role = Role.Button,
                    onClickLabel = closeContentDescription,
                    onClick = onDismiss,
                )
                .semantics { contentDescription = ariaLabel ?: title ?: closeContentDescription },
            contentAlignment = Alignment.Center,
        ) {
            ModalSurface(
                modifier = modifier,
                title = title,
                size = size,
                closeContentDescription = closeContentDescription,
                onDismiss = onDismiss,
                footer = footer,
                content = content,
            )
        }
    }
}

@Composable
private fun ModalSurface(
    modifier: Modifier,
    title: String?,
    size: ModalSize,
    closeContentDescription: String,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    val maxWidth = when (size) {
        ModalSize.SM -> 384.dp
        ModalSize.MD -> 560.dp
        ModalSize.LG -> 704.dp
        ModalSize.FULL -> 1100.dp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = maxWidth)
            .fillMaxHeight(if (size == ModalSize.FULL) 0.92f else 0.9f)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .semantics { role = Role.Dialog },
        shape = if (size == ModalSize.FULL) RoundedCornerShape(18.dp) else RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 18.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onClick = {},
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(44.dp).semantics { contentDescription = closeContentDescription },
                    ) {
                        Text(text = "×", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = if (size == ModalSize.FULL) Dp.Infinity else 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
            ) { content() }

            if (footer != null) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) { footer() }
            } else {
                Spacer(modifier = Modifier.size(2.dp))
            }
        }
    }
}
