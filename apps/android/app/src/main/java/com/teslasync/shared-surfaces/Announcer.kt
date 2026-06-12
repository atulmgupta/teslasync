package com.teslasync.sharedsurfaces

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide accessibility announcer — the Android counterpart of the web `useAnnouncer()`
 * hook (`web/src/hooks/useAnnouncer.ts`).
 *
 * The web hook is a tiny in-memory pub/sub two screen-reader live regions subscribe to; it holds
 * no network state, so the faithful native port is likewise a singleton holding the latest polite
 * and assertive messages as [StateFlow]s. Feature code calls [announce]; [AnnouncerRegion]
 * observes the flows and pushes each message into a [VisuallyHidden] live region.
 *
 * Splitting by priority keeps each live region's urgency static, matching the web rationale that
 * some screen readers ignore live-region value changes after the first announcement.
 */
public object Announcer {
    private val _polite = MutableStateFlow("")
    private val _assertive = MutableStateFlow("")

    /** Latest polite message; deferred until the user finishes their current AT activity. */
    public val polite: StateFlow<String> = _polite.asStateFlow()

    /** Latest assertive message; interrupts the user — reserve for errors / urgent cues. */
    public val assertive: StateFlow<String> = _assertive.asStateFlow()

    /** Pushes [message] into the region for [priority]. Empty clears the region. */
    public fun announce(message: String, priority: AnnouncePriority = AnnouncePriority.Polite) {
        when (priority) {
            AnnouncePriority.Assertive -> _assertive.value = message
            AnnouncePriority.Polite -> _polite.value = message
        }
    }
}
