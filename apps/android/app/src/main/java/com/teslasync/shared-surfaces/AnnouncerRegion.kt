package com.teslasync.sharedsurfaces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import com.teslasync.android.theme.TeslaSyncTheme

/**
 * Native Android port of web `components/a11y/AnnouncerRegion.tsx`.
 *
 * Global mount point for the two visually-hidden live regions (one polite, one assertive) that
 * [Announcer] writes into. Mount exactly once near the root of the app shell. The two regions are
 * siblings with static priorities because some screen readers ignore live-region urgency changes
 * after the first announcement (the same reasoning as the web source).
 *
 * Emits the `view.opened` diagnostics event with the surface slug per the P1/S11 contract.
 */
private const val SLUG = "AnnouncerRegion"

@Composable
public fun AnnouncerRegion(announcer: Announcer = Announcer) {
    TrackSurfaceView(SLUG)

    val polite by announcer.polite.collectAsState()
    val assertive by announcer.assertive.collectAsState()

    VisuallyHidden(text = polite, liveRegion = true, priority = AnnouncePriority.Polite)
    VisuallyHidden(text = assertive, liveRegion = true, priority = AnnouncePriority.Assertive)
}

@Preview
@Composable
private fun AnnouncerRegionPreview() {
    TeslaSyncTheme {
        // Live regions are visually empty by design; the preview proves it composes.
        AnnouncerRegion(remember { Announcer })
    }
}
