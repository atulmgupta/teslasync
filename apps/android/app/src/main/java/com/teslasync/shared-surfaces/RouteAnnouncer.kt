package com.teslasync.sharedsurfaces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.teslasync.android.theme.TeslaSyncTheme
import kotlinx.coroutines.delay

/** Default delay before reading the new screen title after a route change (web parity). */
private const val DEFAULT_ANNOUNCE_DELAY_MS = 100L
private const val SLUG = "RouteAnnouncer"

/**
 * Native Android port of web `components/a11y/RouteAnnouncer.tsx`.
 *
 * Navigation between Compose destinations is silent to TalkBack — the content swaps but the AT
 * user gets no spoken cue. WCAG 2.4.2 and the WAI-ARIA APG require a polite announcement carrying
 * the new screen's title on every route change. This surface observes [route] (the current
 * destination key) and [title] (the resolved screen title), and after a short [delayMs] settle
 * window pushes the title into a [VisuallyHidden] polite live region.
 *
 * The first composition is skipped because the platform already announces the initial screen,
 * matching the web first-render guard. A rotating zero-width-space suffix is appended so two
 * consecutive routes that resolve to the same title are still re-announced (screen readers skip
 * identical live-region text).
 *
 * Route changes use their own region (not [Announcer]) so a noisy imperative announcement does
 * not clobber a route change landing in the same frame — the same separation the web source keeps.
 */
@Composable
public fun RouteAnnouncer(
    route: String,
    title: String,
    delayMs: Long = DEFAULT_ANNOUNCE_DELAY_MS,
) {
    TrackSurfaceView(SLUG)

    var message by remember { mutableStateOf("") }
    var firstRender by remember { mutableStateOf(true) }
    var counter by remember { mutableStateOf(0) }

    LaunchedEffect(route) {
        if (firstRender) {
            firstRender = false
            return@LaunchedEffect
        }
        delay(delayMs)
        if (title.isEmpty()) {
            message = ""
            return@LaunchedEffect
        }
        counter = (counter + 1) % 4
        message = title + "\u200B".repeat(counter)
    }

    VisuallyHidden(text = message, liveRegion = true, priority = AnnouncePriority.Polite)
}

@Preview
@Composable
private fun RouteAnnouncerPreview() {
    TeslaSyncTheme {
        RouteAnnouncer(route = "/charging/123", title = "Charging Session — TeslaSync")
    }
}
