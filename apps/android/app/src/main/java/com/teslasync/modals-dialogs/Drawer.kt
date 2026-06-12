package com.teslasync.modalsdialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Side from which [Drawer] enters, matching the web `left | right` prop. */
enum class DrawerSide { LEFT, RIGHT }

/** Width preset for [Drawer]; [MD] matches the web `max-w-md` default. */
enum class DrawerSize { SM, MD, LG, FULL }

/**
 * `Drawer` — native parity for `web/src/components/ui/Drawer.tsx`.
 *
 * Presents a modal scrim plus a side-attached, scrollable panel. The optional
 * header appears only when [title] is provided, the close affordance mirrors the
 * web drawer's dismiss button, and [footer] is pinned below the scroll body.
 */
@Composable
fun Drawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    side: DrawerSide = DrawerSide.RIGHT,
    size: DrawerSize = DrawerSize.MD,
    closeContentDescription: String = "Close",
    panelContentDescription: String = title ?: "Panel",
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.46f))
                .clickable(role = Role.Button, onClickLabel = closeContentDescription, onClick = onDismiss),
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally { width -> if (side == DrawerSide.RIGHT) width else -width },
                exit = slideOutHorizontally { width -> if (side == DrawerSide.RIGHT) width else -width },
                modifier = Modifier.align(if (side == DrawerSide.RIGHT) Alignment.CenterEnd else Alignment.CenterStart),
            ) {
                DrawerPanel(
                    modifier = modifier,
                    title = title,
                    size = size,
                    closeContentDescription = closeContentDescription,
                    panelContentDescription = panelContentDescription,
                    onDismiss = onDismiss,
                    footer = footer,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun DrawerPanel(
    modifier: Modifier,
    title: String?,
    size: DrawerSize,
    closeContentDescription: String,
    panelContentDescription: String,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    val maxWidth = when (size) {
        DrawerSize.SM -> 320.dp
        DrawerSize.MD -> 448.dp
        DrawerSize.LG -> 640.dp
        DrawerSize.FULL -> 900.dp
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .widthIn(max = maxWidth)
            .semantics {
                role = Role.Dialog
                contentDescription = panelContentDescription
            },
        tonalElevation = 6.dp,
        shadowElevation = 20.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onClick = {},
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
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
                    ) { Text(text = "×", style = MaterialTheme.typography.headlineSmall) }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) { content() }

            if (footer != null) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) { footer() }
            }
        }
    }
}
