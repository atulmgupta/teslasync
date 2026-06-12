package com.teslasync.sharedsurfaces

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.teslasync.android.di.LocalDiagnostics
import io.teslasync.shared.core.diagnostics.TelemetryEvent

/**
 * Thin i18n facade for the shared-meaningful surfaces (P1/S10 contract).
 *
 * Every visible string in a surface MUST resolve through this facade rather than a hardcoded
 * English literal. It wraps Compose's [stringResource] against the auto-generated
 * `res/values*/strings.xml` catalog (keys are `translation_*`), so a single swap point exists
 * if the catalog wiring changes and so the lint that forbids raw literals has one allowed call.
 */
@Composable
public fun stringRes(@StringRes id: Int): String = stringResource(id)

/** Formatted variant for catalog entries that take positional `%s` / `%d` arguments. */
@Composable
public fun stringRes(@StringRes id: Int, vararg formatArgs: Any): String =
    stringResource(id, *formatArgs)

/**
 * Emits the P1/S11 `view.opened` diagnostics event for a surface exactly once per composition,
 * carrying the surface [slug]. No-ops until diagnostics consent is granted (handled by the
 * shared [io.teslasync.shared.core.diagnostics.Telemetry] implementation).
 */
@Composable
public fun TrackSurfaceView(slug: String) {
    val diagnostics = LocalDiagnostics.current
    val context = LocalContext.current
    LaunchedEffect(slug) {
        diagnostics.telemetry.track(
            TelemetryEvent.ScreenView(
                screen = slug,
                platform = "android",
                appVersion = appVersionName(context),
            ),
        )
    }
}

private fun appVersionName(context: android.content.Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")
