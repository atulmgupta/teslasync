package com.teslasync.sharedsurfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.teslasync.android.R
import io.teslasync.shared.core.data.repo.Resource

/**
 * Cross-surface rendering contract shared by every P3 `shared-meaningful` Android surface.
 *
 * The web sources all derive their visible state from a cache-then-network hook whose shape
 * is the KMP [Resource] (ADR-013). This scaffold folds a [Resource] into the five mandatory
 * page-states the prompts require — `loading`, `empty`, `error`, `stale`, `offline` — plus the
 * loaded `content`, so no surface hides a section when data is null. Every branch renders a
 * real, accessible affordance; none is a blank box.
 *
 * Surfaces that are pure presentational primitives (no data feed) do not use this scaffold and
 * instead render their content directly.
 */
@Composable
public fun <T> SurfaceStateScaffold(
    resource: Resource<T>,
    modifier: Modifier = Modifier,
    isEmpty: (T) -> Boolean = { false },
    emptyMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    content: @Composable (data: T, stale: Boolean) -> Unit,
) {
    when (resource) {
        is Resource.Loading -> {
            val cached = resource.cached
            if (cached != null && !isEmpty(cached)) {
                content(cached, resource.stale)
            } else {
                SurfaceLoading(modifier)
            }
        }

        is Resource.Success -> {
            if (isEmpty(resource.data)) {
                SurfaceEmpty(modifier, emptyMessage)
            } else {
                content(resource.data, resource.stale)
            }
        }

        is Resource.Error -> {
            val cached = resource.cached
            if (cached != null && !isEmpty(cached)) {
                // Offline / last-known: render cached value flagged stale.
                content(cached, true)
            } else {
                SurfaceError(modifier, onRetry)
            }
        }
    }
}

/** Initial-fetch skeleton chrome — a centered progress indicator with a TalkBack label. */
@Composable
public fun SurfaceLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Friendly empty-state — never a blank box. */
@Composable
public fun SurfaceEmpty(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message ?: stringRes(R.string.translation_common_noData),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Fetch-failure state with a retry affordance (web `QueryError` equivalent). */
@Composable
public fun SurfaceError(modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringRes(R.string.translation_queryError_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text(text = stringRes(R.string.translation_queryError_retry))
            }
        }
    }
}
