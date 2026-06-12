package com.teslasync.modalsdialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Summary row data for [EntryDrawer], matching `DLQEntrySummary`. */
data class DLQEntrySummaryNative(
    val id: Long,
    val arrivedAt: String,
    val dlqTopic: String?,
    val parsedReason: String?,
    val parsedVin: String?,
    val parsedSourceTopic: String?,
    val parsedRedeliveries: Int?,
    val parseError: String?,
    val replayable: Boolean,
    val innerPayloadSize: Int,
    val rawPayloadSize: Int,
)

/** Full drawer payload data for [EntryDrawer], matching `DLQEntryFull`. */
data class DLQEntryFullNative(
    val id: Long,
    val arrivedAt: String,
    val dlqTopic: String?,
    val parsedReason: String?,
    val parsedVin: String?,
    val parsedSourceTopic: String?,
    val parsedRedeliveries: Int?,
    val parseError: String?,
    val replayable: Boolean,
    val innerPayloadSize: Int,
    val rawPayloadSize: Int,
    val innerPayloadB64: String,
    val rawPayloadB64: String,
)

/** Display copy for [EntryDrawer], resolved by the caller's i18n layer. */
data class EntryDrawerText(
    val title: String = "DLQ entry #%d",
    val titleFallback: String = "DLQ entry",
    val close: String = "Close",
    val replay: String = "Replay",
    val innerTab: String = "Inner payload",
    val rawTab: String = "Raw envelope",
    val copy: String = "Copy",
    val id: String = "ID",
    val arrived: String = "Arrived",
    val dlqTopic: String = "DLQ topic",
    val reason: String = "Reason",
    val vin: String = "VIN",
    val sourceTopic: String = "Source topic",
    val redeliveries: String = "Redeliveries",
    val parseError: String = "Parse error",
    val binaryPayload: String = "(non-UTF-8 binary, %d bytes — use the copy button to download base64)",
    val binaryEnvelope: String = "(non-UTF-8 envelope, %d bytes — use the copy button to download base64)",
    val empty: String = "No DLQ entry selected.",
    val loading: String = "Loading",
    val closeContentDescription: String = "Close",
)

private enum class PayloadTab { INNER, RAW }
private data class EntryHead(
    val id: Long,
    val arrivedAt: String,
    val dlqTopic: String?,
    val parsedReason: String?,
    val parsedVin: String?,
    val parsedSourceTopic: String?,
    val parsedRedeliveries: Int?,
    val parseError: String?,
    val replayable: Boolean,
    val innerPayloadSize: Int,
    val rawPayloadSize: Int,
)

/**
 * `EntryDrawer` — native parity for the DLQ Inspector entry drawer.
 *
 * Renders loading, empty and content states; uses cached summary fields while
 * the full entry is still loading; preserves inner/raw payload tabs; disables
 * replay when server replay is disabled, the entry is not replayable, loading,
 * or a replay is in flight. No network or mutation hook is owned here.
 */
@Composable
fun EntryDrawer(
    visible: Boolean,
    summary: DLQEntrySummaryNative?,
    full: DLQEntryFullNative?,
    loading: Boolean,
    replayEnabled: Boolean,
    replayInFlight: Boolean,
    onDismiss: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
    strings: EntryDrawerText = EntryDrawerText(),
) {
    var activeTab by remember { mutableStateOf(PayloadTab.INNER) }
    val head = full?.toHead() ?: summary?.toHead()
    val replayDisabled = !replayEnabled || head?.replayable != true || replayInFlight || loading

    Drawer(
        visible = visible,
        onDismiss = onDismiss,
        title = head?.let { strings.title.format(it.id) } ?: strings.titleFallback,
        closeContentDescription = strings.closeContentDescription,
        modifier = modifier,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onDismiss) { Text(strings.close) }
                Button(onClick = onReplay, enabled = !replayDisabled) {
                    if (replayInFlight) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(text = strings.replay, modifier = Modifier.padding(start = if (replayInFlight) 8.dp else 0.dp))
                }
            }
        },
    ) {
        when {
            loading && full == null -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = strings.loading }) }

            head != null -> EntryContent(
                head = head,
                full = full,
                activeTab = activeTab,
                onTabChange = { activeTab = it },
                strings = strings,
            )

            else -> Text(
                text = strings.empty,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntryContent(
    head: EntryHead,
    full: DLQEntryFullNative?,
    activeTab: PayloadTab,
    onTabChange: (PayloadTab) -> Unit,
    strings: EntryDrawerText,
) {
    val innerText = remember(full?.innerPayloadB64) { full?.innerPayloadB64?.decodeBase64Utf8().orEmpty() }
    val rawText = remember(full?.rawPayloadB64) { full?.rawPayloadB64?.decodeBase64Utf8().orEmpty() }
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyValue(strings.id, head.id.toString())
                KeyValue(strings.arrived, head.arrivedAt)
                KeyValue(strings.dlqTopic, head.dlqTopic.orDash())
                KeyValue(strings.reason, head.parsedReason.orDash())
                KeyValue(strings.vin, head.parsedVin.orDash())
                KeyValue(strings.sourceTopic, head.parsedSourceTopic.orDash())
                KeyValue(strings.redeliveries, head.parsedRedeliveries?.formatInt() ?: "—")
                KeyValue(strings.parseError, head.parseError.orDash(), muted = true)
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = if (activeTab == PayloadTab.INNER) 0 else 1) {
                    Tab(
                        selected = activeTab == PayloadTab.INNER,
                        onClick = { onTabChange(PayloadTab.INNER) },
                        text = { Text(strings.innerTab) },
                    )
                    Tab(
                        selected = activeTab == PayloadTab.RAW,
                        onClick = { onTabChange(PayloadTab.RAW) },
                        text = { Text(strings.rawTab) },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = {
                            val text = if (activeTab == PayloadTab.INNER) {
                                innerText.ifEmpty { full?.innerPayloadB64.orEmpty() }
                            } else {
                                rawText.ifEmpty { full?.rawPayloadB64.orEmpty() }
                            }
                            clipboard.setText(AnnotatedString(text))
                        },
                    ) { Text(strings.copy) }
                }
                PayloadBox(
                    text = if (activeTab == PayloadTab.INNER) {
                        innerText.ifEmpty { strings.binaryPayload.format(head.innerPayloadSize) }
                    } else {
                        rawText.ifEmpty { strings.binaryEnvelope.format(head.rawPayloadSize) }
                    },
                )
            }
        }
    }
}

@Composable
private fun KeyValue(label: String, value: String, muted: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            modifier = Modifier.weight(0.38f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.62f),
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PayloadBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
            .semantics { contentDescription = text.take(120) },
    ) {
        Text(text = text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
    }
}

private fun DLQEntrySummaryNative.toHead() = EntryHead(
    id = id,
    arrivedAt = arrivedAt,
    dlqTopic = dlqTopic,
    parsedReason = parsedReason,
    parsedVin = parsedVin,
    parsedSourceTopic = parsedSourceTopic,
    parsedRedeliveries = parsedRedeliveries,
    parseError = parseError,
    replayable = replayable,
    innerPayloadSize = innerPayloadSize,
    rawPayloadSize = rawPayloadSize,
)

private fun DLQEntryFullNative.toHead() = EntryHead(
    id = id,
    arrivedAt = arrivedAt,
    dlqTopic = dlqTopic,
    parsedReason = parsedReason,
    parsedVin = parsedVin,
    parsedSourceTopic = parsedSourceTopic,
    parsedRedeliveries = parsedRedeliveries,
    parseError = parseError,
    replayable = replayable,
    innerPayloadSize = innerPayloadSize,
    rawPayloadSize = rawPayloadSize,
)

private fun String?.orDash(): String = if (this.isNullOrBlank()) "—" else this

private fun Int.formatInt(): String = toString().reversed().chunked(3).joinToString(",").reversed()

private fun String.decodeBase64Utf8(): String {
    if (isBlank()) return ""
    val clean = filterNot { it.isWhitespace() }
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val bytes = mutableListOf<Byte>()
    var buffer = 0
    var bits = 0
    for (char in clean) {
        if (char == '=') break
        val value = table.indexOf(char)
        if (value < 0) return ""
        buffer = (buffer shl 6) or value
        bits += 6
        if (bits >= 8) {
            bits -= 8
            bytes.add(((buffer shr bits) and 0xFF).toByte())
        }
    }
    return try {
        bytes.toByteArray().decodeToString()
    } catch (_: Throwable) {
        ""
    }
}
