package com.teslasync.widgetprimitives

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * `WidgetMapView` — native parity for
 * `web/src/features/dashboard/widgets/shared/WidgetMapView.tsx`.
 *
 * A rounded map container. The actual map surface (Maps SDK for Android /
 * Maps-Compose `GoogleMap`, per the P3 guidelines) is injected via the
 * [mapContent] slot — mirroring how the web source wraps Leaflet's
 * `MapContainer` and passes overlays as `children`. Keeping the concrete map
 * out of this primitive means the widget-primitive bundle has no hard
 * dependency on a maps library; the component-library / page binds the real
 * map and camera.
 *
 * When [isEmpty] is true a friendly [WidgetEmptyState] is shown instead of the
 * map (parity with the web empty branch). In `compact` mode the caller is
 * expected to disable gestures on the injected map (exposed via [compact]).
 *
 * @param mapContent the map composable + overlays; receives [compact] so the
 *   caller can disable scroll/zoom/drag gestures on small tiles.
 */
@Composable
fun WidgetMapView(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    isEmpty: Boolean = false,
    emptyMessage: String = "No location data available",
    mapContent: @Composable BoxScope.(compact: Boolean) -> Unit,
) {
    if (isEmpty) {
        WidgetEmptyState(message = emptyMessage, modifier = modifier)
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .semantics { contentDescription = "Map" },
    ) {
        mapContent(compact)
    }
}
