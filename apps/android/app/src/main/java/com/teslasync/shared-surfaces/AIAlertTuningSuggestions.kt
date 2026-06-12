package com.teslasync.sharedsurfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teslasync.android.R
import com.teslasync.android.theme.TeslaSyncTheme

/**
 * Native Android port of web `components/ai/AIAlertTuningSuggestions.tsx`.
 *
 * The web surface is an opt-in Helix panel that streams a typed `AlertRule` patch proposal for an
 * existing alert rule and lets the operator copy the proposed scalars onto the AlertStudio editor
 * form. It is descriptive-replay only: the panel NEVER persists — the canonical AlertStudio Save
 * button stays the sole write path (ADR-015 §I3 + §I8). This port reproduces that contract:
 *
 *  - **Off-mode gate** (web `withAiFeature`): the surface renders nothing when the per-feature AI
 *    toggle is off, mirroring the HOC that returns `null` in `ai_mode='off'` (ADR-015 §I5). The
 *    gate is hoisted as [aiFeatureEnabled] so the host decides via the shared feature-flag holder.
 *  - **No direct I/O**: the view is pure. The SSE lifecycle (start / cancel-on-ruleId-change /
 *    tool_result capture) lives in the injected P1/S8 state holder, which feeds this view a
 *    folded [AiFeatureStream] and the captured [AlertRuleDraftPatch]. The host wires `onSuggest`
 *    to the holder's `start()` and `onApplyDraft` to the AlertStudio editor merge — exactly like
 *    the web `onApplyDraft` callback.
 *  - **Computed disabled** (web Rule W1-A): the action is gated by [computeCanStart] /
 *    [AiFeatureStream.isBusy], never a literal disabled flag.
 *
 * Streaming-only AI features have no cache-then-network [io.teslasync.shared.core.data.repo.Resource]
 * feed, so the `stale` / `offline` page-states from the generic prompt checklist do not apply here
 * — the web source has no such branch either. The reproduced lifecycle branches are the ones the
 * web source actually renders: gated-off (absent), idle, streaming (thinking), error, and
 * done-with / done-without a captured proposal. Every non-absent branch renders a real, accessible
 * affordance — never a blank box.
 */

private const val SLUG = "AIAlertTuningSuggestions"

/** withAiFeature registry slug — drives the root test tag, kept in lockstep with the web id. */
private const val FEATURE_ID = "alert-tuning-suggestions"
private const val ROOT_TEST_TAG = "ai-feature-$FEATURE_ID-root"
private const val SUGGEST_TEST_TAG = "ai-feature-$FEATURE_ID-suggest"
private const val APPLY_TEST_TAG = "ai-feature-$FEATURE_ID-apply"

/**
 * The subset of `AlertRule` scalars the LLM is allowed to propose — the Kotlin mirror of the web
 * `AlertRuleDraftPatch` interface, itself mirroring `internal/ai/tools/alert_tuning.go`
 * `AlertRulePatchProposal`. Keeping it narrow prevents the panel from over-writing fields the user
 * did not consent to changing (e.g. signal name, vehicle scope). All members are nullable so an
 * absent field is omitted from both the preview and the applied merge.
 */
public data class AlertRuleDraftPatch(
    val valueNum: Double? = null,
    val valueMin: Double? = null,
    val valueMax: Double? = null,
    val cooldownMin: Int? = null,
    val severity: String? = null,
    val triggerMode: String? = null,
    val op: String? = null,
) {
    /** True when the LLM proposed no usable scalar — used to render the done-without-proposal state. */
    public fun isEmpty(): Boolean =
        valueNum == null &&
            valueMin == null &&
            valueMax == null &&
            cooldownMin == null &&
            severity.isNullOrEmpty() &&
            triggerMode.isNullOrEmpty() &&
            op.isNullOrEmpty()
}

/** User-facing AI stream lifecycle — the Kotlin mirror of web `AiStreamState`. */
public enum class AiStreamPhase { Idle, Streaming, PausedConfirm, Done, Error }

/**
 * The narrow slice of the shared AI-stream holder this surface reads — the Kotlin analogue of the
 * web `AIFeatureStream`. The host folds its `useAiStream`-equivalent holder into this shape so the
 * view never touches the network or the raw SSE frames.
 *
 * @property phase current lifecycle phase.
 * @property text accumulated `delta` text streamed so far (empty until the first delta arrives).
 * @property error human-readable failure message when [phase] is [AiStreamPhase.Error]; else null.
 */
public data class AiFeatureStream(
    val phase: AiStreamPhase = AiStreamPhase.Idle,
    val text: String = "",
    val error: String? = null,
) {
    /** Mirrors the web `isBusy` guard: a stream in flight (or awaiting a confirm) blocks re-fire. */
    public val isBusy: Boolean
        get() = phase == AiStreamPhase.Streaming || phase == AiStreamPhase.PausedConfirm
}

/**
 * Computes whether the suggest action may fire — the Kotlin mirror of the web
 * `canStart={!!ruleId && stream.state !== 'paused-confirm'}` expression. Pure so the host (or a
 * unit test) can reuse it without composing the view.
 */
public fun computeCanStart(ruleId: Long, phase: AiStreamPhase): Boolean =
    ruleId != 0L && phase != AiStreamPhase.PausedConfirm

/**
 * Pure adapter that folds a `tool_result` SSE frame into an [AlertRuleDraftPatch], reproducing the
 * web `handleEvent` capture logic field-for-field. Returns null unless the frame is the successful
 * `draft_alert_rule_patch` tool result with `status == "ok"` and a non-null `proposed` map; numeric
 * fields are accepted only when actually numeric and string fields only when non-blank — so a
 * malformed or partial frame can never seed a half-populated form. This is the testable projection
 * the prompt's adapter unit test targets.
 */
public fun extractAlertRulePatch(
    name: String,
    ok: Boolean,
    status: String?,
    proposed: Map<String, Any?>?,
): AlertRuleDraftPatch? {
    if (name != "draft_alert_rule_patch" || !ok || status != "ok" || proposed == null) {
        return null
    }
    return AlertRuleDraftPatch(
        valueNum = (proposed["value_num"] as? Number)?.toDouble(),
        valueMin = (proposed["value_min"] as? Number)?.toDouble(),
        valueMax = (proposed["value_max"] as? Number)?.toDouble(),
        cooldownMin = (proposed["cooldown_min"] as? Number)?.toInt(),
        severity = (proposed["severity"] as? String)?.takeIf { it.isNotEmpty() },
        triggerMode = (proposed["trigger_mode"] as? String)?.takeIf { it.isNotEmpty() },
        op = (proposed["op"] as? String)?.takeIf { it.isNotEmpty() },
    )
}

/**
 * The AI alert-tuning suggestion surface.
 *
 * @param ruleId the `AlertRule.id` being tuned; `0` disables the action (web `!!ruleId`).
 * @param stream folded AI-stream lifecycle from the shared P1/S8 holder.
 * @param proposal the captured patch (null until a successful `tool_result` arrives).
 * @param onSuggest fires the stream; wired by the host to the holder's `start()`. Re-fires are a
 *   no-op while [AiFeatureStream.isBusy] (the host resets the captured proposal on (re)start).
 * @param onApplyDraft copies [proposal] onto the parent AlertStudio editor draft. The panel never
 *   writes to the API — the user clicks the editor's canonical Save next.
 * @param aiFeatureEnabled the per-feature off-mode gate; when false the surface is absent.
 */
@Composable
public fun AIAlertTuningSuggestions(
    ruleId: Long,
    stream: AiFeatureStream,
    proposal: AlertRuleDraftPatch?,
    onSuggest: () -> Unit,
    onApplyDraft: (AlertRuleDraftPatch) -> Unit,
    modifier: Modifier = Modifier,
    aiFeatureEnabled: Boolean = true,
) {
    // withAiFeature parity: absent (renders nothing) when the feature is off.
    if (!aiFeatureEnabled) {
        return
    }

    TrackSurfaceView(SLUG)

    val canStart = computeCanStart(ruleId, stream.phase)
    val isBusy = stream.isBusy

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ROOT_TEST_TAG),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()

            // Action row (web buttonPlacement="below": right-aligned beneath the header).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SuggestButton(
                    enabled = canStart && !isBusy,
                    busy = isBusy,
                    onClick = onSuggest,
                )
            }

            StreamOutput(stream)

            if (proposal != null && !proposal.isEmpty()) {
                ProposalPreview(
                    proposal = proposal,
                    applyEnabled = !isBusy,
                    onApply = { onApplyDraft(proposal) },
                )
            }
        }
    }
}

/** Title + Helix badge + descriptive subtitle (web AIFeatureCard header). */
@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringRes(R.string.translation_notifications_alertStudio_aiTuning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HelixBadge()
        }
        Text(
            text = stringRes(R.string.translation_notifications_alertStudio_aiTuning_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The small "Helix" brand pill (web `AIBadge`). */
@Composable
private fun HelixBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = stringRes(R.string.translation_notifications_alertStudio_aiTuning_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * The primary suggest action. While streaming it shows a progress spinner and the shared
 * "Helix is thinking…" label; otherwise the per-feature "Suggest tuning" label. The button is the
 * retry affordance for the error branch (re-firing the stream).
 */
@Composable
private fun SuggestButton(
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
) {
    val label = if (busy) {
        stringRes(R.string.translation_chatbot_thinking)
    } else {
        stringRes(R.string.translation_notifications_alertStudio_aiTuning_suggestButton)
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .testTag(SUGGEST_TEST_TAG)
            .semantics { stateDescription = label },
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(text = label)
    }
}

/**
 * The streaming-output region (web `AiOutputPanel`). Renders the error message with a polite-failure
 * treatment when the stream failed, the accumulated delta text once it begins arriving, and a
 * "thinking" hint while a stream is open but no text has streamed yet — never a blank box.
 */
@Composable
private fun StreamOutput(stream: AiFeatureStream) {
    when {
        stream.phase == AiStreamPhase.Error -> {
            Text(
                text = stream.error
                    ?: stringRes(R.string.translation_queryError_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        stream.text.isNotEmpty() -> {
            Text(
                text = stream.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        stream.phase == AiStreamPhase.Streaming -> {
            Text(
                text = stringRes(R.string.translation_chatbot_thinking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The captured-proposal block (web's typed-envelope `children` slot): an "Apply to form" action and
 * a review list of every proposed scalar. The field identifiers are rendered verbatim (technical
 * keys, not user copy — matching the web source, which does not translate them) so the operator
 * reviews exactly what will merge into the editor before saving.
 */
@Composable
private fun ProposalPreview(
    proposal: AlertRuleDraftPatch,
    applyEnabled: Boolean,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(
            onClick = onApply,
            enabled = applyEnabled,
            modifier = Modifier.testTag(APPLY_TEST_TAG),
        ) {
            Text(text = stringRes(R.string.translation_notifications_alertStudio_aiTuning_applyButton))
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringRes(R.string.translation_notifications_alertStudio_aiTuning_previewLabel),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            proposal.valueNum?.let { ProposalRow("value_num", it.toString()) }
            proposal.valueMin?.let { ProposalRow("value_min", it.toString()) }
            proposal.valueMax?.let { ProposalRow("value_max", it.toString()) }
            proposal.cooldownMin?.let { ProposalRow("cooldown_min", it.toString()) }
            proposal.severity?.takeIf { it.isNotEmpty() }?.let { ProposalRow("severity", it) }
            proposal.triggerMode?.takeIf { it.isNotEmpty() }?.let { ProposalRow("trigger_mode", it) }
            proposal.op?.takeIf { it.isNotEmpty() }?.let { ProposalRow("op", it) }
        }
    }
}

/** A single "key: value" review line in the proposal preview. */
@Composable
private fun ProposalRow(key: String, value: String) {
    Text(
        text = "$key: $value",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Idle — ready to suggest")
@Composable
private fun AIAlertTuningSuggestionsIdlePreview() {
    TeslaSyncTheme {
        AIAlertTuningSuggestions(
            ruleId = 42L,
            stream = AiFeatureStream(phase = AiStreamPhase.Idle),
            proposal = null,
            onSuggest = {},
            onApplyDraft = {},
        )
    }
}

@Preview(name = "Streaming — thinking")
@Composable
private fun AIAlertTuningSuggestionsStreamingPreview() {
    TeslaSyncTheme {
        AIAlertTuningSuggestions(
            ruleId = 42L,
            stream = AiFeatureStream(phase = AiStreamPhase.Streaming),
            proposal = null,
            onSuggest = {},
            onApplyDraft = {},
        )
    }
}

@Preview(name = "Error")
@Composable
private fun AIAlertTuningSuggestionsErrorPreview() {
    TeslaSyncTheme {
        AIAlertTuningSuggestions(
            ruleId = 42L,
            stream = AiFeatureStream(
                phase = AiStreamPhase.Error,
                error = "Helix is unavailable right now.",
            ),
            proposal = null,
            onSuggest = {},
            onApplyDraft = {},
        )
    }
}

@Preview(name = "Done — proposal captured")
@Composable
private fun AIAlertTuningSuggestionsProposalPreview() {
    TeslaSyncTheme {
        AIAlertTuningSuggestions(
            ruleId = 42L,
            stream = AiFeatureStream(phase = AiStreamPhase.Done),
            proposal = AlertRuleDraftPatch(
                valueNum = 18.0,
                cooldownMin = 120,
                severity = "warning",
                triggerMode = "sustained",
                op = "lt",
            ),
            onSuggest = {},
            onApplyDraft = {},
        )
    }
}
