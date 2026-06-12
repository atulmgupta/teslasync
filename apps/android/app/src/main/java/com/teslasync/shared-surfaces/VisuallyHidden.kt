package com.teslasync.sharedsurfaces

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teslasync.android.theme.TeslaSyncTheme

/**
 * Native Android port of web `components/a11y/VisuallyHidden.tsx`.
 *
 * Renders content that is invisible to sighted users but exposed to assistive technologies
 * (TalkBack, Switch Access, Braille displays). On the web this is the canonical replacement for
 * ad-hoc `sr-only` spans; on Android there is no off-screen CSS, so the idiomatic equivalent is a
 * zero-footprint node that carries the announcement purely through the Compose semantics tree.
 *
 * The web component's `liveRegion` shorthand maps to [LiveRegionMode]: `polite` defers to the
 * user's current AT activity, `assertive` interrupts (reserve for genuine errors). The semantics
 * node uses [stateDescription] so a *changed* [text] is re-announced — mirroring the web
 * `aria-live` + `aria-atomic` pairing — and [invisibleToUser] keeps the node out of the visual
 * tree while remaining accessible.
 *
 * @param text the message to expose to assistive technology. Empty string announces nothing.
 * @param priority live-region urgency; [LiveRegionMode.Polite] by default.
 */
public enum class AnnouncePriority { Polite, Assertive }

@Composable
public fun VisuallyHidden(
    text: String,
    modifier: Modifier = Modifier,
    liveRegion: Boolean = false,
    priority: AnnouncePriority = AnnouncePriority.Polite,
) {
    Box(
        modifier = modifier
            .size(1.dp)
            .semantics {
                invisibleToUser()
                if (liveRegion) {
                    this.liveRegion = when (priority) {
                        AnnouncePriority.Assertive -> LiveRegionMode.Assertive
                        AnnouncePriority.Polite -> LiveRegionMode.Polite
                    }
                }
                // stateDescription re-fires when text changes, the aria-atomic analogue.
                stateDescription = text
            },
    )
}

@Preview
@Composable
private fun VisuallyHiddenPreview() {
    TeslaSyncTheme {
        VisuallyHidden(text = "3 items archived", liveRegion = true)
    }
}
