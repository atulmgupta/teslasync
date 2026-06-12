package com.teslasync.modalsdialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Modes offered by [GeofenceDrawer], matching the web drawer toolbar. */
enum class GeofenceMode { CIRCLE, POLYGON, RECTANGLE }

/** Persisted or drawn geofence shape consumed by [GeofenceDrawer]. */
data class DrawableGeofence(
    val id: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val radius: Double? = null,
    val polygon: List<Pair<Double, Double>> = emptyList(),
    val name: String? = null,
)

/** New or edited geometry emitted by [GeofenceDrawer]. */
data class NewGeofence(
    val shape: GeofenceMode,
    val lat: Double? = null,
    val lng: Double? = null,
    val radius: Double? = null,
    val polygon: List<Pair<Double, Double>> = emptyList(),
)

/**
 * Native parity surface for `web/src/components/maps/GeofenceDrawer.tsx`.
 *
 * Because this self-contained Compose surface cannot depend on a map SDK, it
 * exposes the same shape data and callbacks as a Material control panel: users
 * choose circle, polygon or rectangle, enter geometry, submit creates, and
 * persisted fences render with edit/delete affordances when callbacks exist.
 */
@Composable
fun GeofenceDrawer(
    fences: List<DrawableGeofence>,
    onCreate: (NewGeofence) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: ((String, NewGeofence) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    modes: List<GeofenceMode> = listOf(GeofenceMode.CIRCLE),
    color: Color = Color(0xFF22D3EE),
    title: String = "Geofences",
    circleLabel: String = "Circle",
    polygonLabel: String = "Polygon",
    rectangleLabel: String = "Rectangle",
    latitudeLabel: String = "Latitude",
    longitudeLabel: String = "Longitude",
    radiusLabel: String = "Radius meters",
    polygonLabelText: String = "Polygon points",
    polygonHelpText: String = "Enter points as lat,lng pairs separated by semicolons.",
    createLabel: String = "Create geofence",
    updateLabel: String = "Update",
    deleteLabel: String = "Delete",
    existingLabel: String = "Existing geofences",
    emptyLabel: String = "No geofences yet.",
    invalidGeometryLabel: String = "Enter valid geometry before continuing.",
    unnamedFenceLabel: String = "Geofence",
    circleDescriptionTemplate: String = "%s — %dm circle around %s, %s",
    polygonDescriptionTemplate: String = "%s — %d-vertex polygon",
) {
    val availableModes = modes.ifEmpty { listOf(GeofenceMode.CIRCLE) }
    var selectedMode by remember(availableModes) { mutableStateOf(availableModes.first()) }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("") }
    var polygon by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth().semantics { contentDescription = title }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availableModes.forEach { mode ->
                FilterChip(selected = selectedMode == mode, onClick = { selectedMode = mode; error = null }, label = { Text(mode.label(circleLabel, polygonLabelText = polygonLabel, rectangleLabel = rectangleLabel)) })
            }
        }
        if (selectedMode == GeofenceMode.CIRCLE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(lat, { lat = it }, latitudeLabel, Modifier.weight(1f))
                NumberField(lng, { lng = it }, longitudeLabel, Modifier.weight(1f))
            }
            NumberField(radius, { radius = it }, radiusLabel, Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(
                value = polygon,
                onValueChange = { polygon = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(polygonLabelText) },
                supportingText = { Text(polygonHelpText) },
            )
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            val geom = buildGeometry(selectedMode, lat, lng, radius, polygon)
            if (geom == null) error = invalidGeometryLabel else {
                error = null
                onCreate(geom)
            }
        }) { Text(createLabel) }
        HorizontalDivider()
        Text(existingLabel, style = MaterialTheme.typography.labelLarge)
        if (fences.isEmpty()) {
            Text(emptyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fences, key = { it.id }) { fence ->
                    FenceRow(fence, color, unnamedFenceLabel, updateLabel, deleteLabel, circleDescriptionTemplate, polygonDescriptionTemplate, onEdit, onDelete)
                }
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> onChange(next.filter { it.isDigit() || it == '.' || it == '-' }) },
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun FenceRow(
    fence: DrawableGeofence,
    color: Color,
    unnamedFenceLabel: String,
    updateLabel: String,
    deleteLabel: String,
    circleDescriptionTemplate: String,
    polygonDescriptionTemplate: String,
    onEdit: ((String, NewGeofence) -> Unit)?,
    onDelete: ((String) -> Unit)?,
) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(fence.name ?: unnamedFenceLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(describeFence(fence, unnamedFenceLabel, circleDescriptionTemplate, polygonDescriptionTemplate), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(modifier = Modifier.background(color), color = color, shape = RoundedCornerShape(4.dp)) { Text(" ", modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) }
                if (onEdit != null) {
                    OutlinedButton(onClick = { fence.toNewGeometry()?.let { onEdit(fence.id, it) } }) { Text(updateLabel) }
                }
                if (onDelete != null) {
                    Text(
                        text = deleteLabel,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { onDelete(fence.id) }.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun GeofenceMode.label(circleLabel: String, polygonLabelText: String, rectangleLabel: String): String = when (this) {
    GeofenceMode.CIRCLE -> circleLabel
    GeofenceMode.POLYGON -> polygonLabelText
    GeofenceMode.RECTANGLE -> rectangleLabel
}

private fun buildGeometry(mode: GeofenceMode, lat: String, lng: String, radius: String, polygon: String): NewGeofence? = when (mode) {
    GeofenceMode.CIRCLE -> {
        val la = lat.toDoubleOrNull()
        val ln = lng.toDoubleOrNull()
        val r = radius.toDoubleOrNull()
        if (la != null && ln != null && r != null && r > 0) NewGeofence(GeofenceMode.CIRCLE, lat = la, lng = ln, radius = r) else null
    }
    GeofenceMode.POLYGON, GeofenceMode.RECTANGLE -> {
        val pts = parsePoints(polygon)
        if (pts.size >= 3) NewGeofence(mode, polygon = pts) else null
    }
}

private fun parsePoints(raw: String): List<Pair<Double, Double>> = raw.split(';').mapNotNull { part ->
    val pieces = part.split(',').map { it.trim() }
    val la = pieces.getOrNull(0)?.toDoubleOrNull()
    val ln = pieces.getOrNull(1)?.toDoubleOrNull()
    if (la != null && ln != null) la to ln else null
}

private fun DrawableGeofence.toNewGeometry(): NewGeofence? = when {
    lat != null && lng != null && radius != null && radius > 0 -> NewGeofence(GeofenceMode.CIRCLE, lat = lat, lng = lng, radius = radius)
    polygon.size >= 3 -> NewGeofence(GeofenceMode.POLYGON, polygon = polygon)
    else -> null
}

private fun describeFence(
    fence: DrawableGeofence,
    unnamedFenceLabel: String,
    circleDescriptionTemplate: String,
    polygonDescriptionTemplate: String,
): String {
    val name = fence.name ?: unnamedFenceLabel
    return when {
        fence.lat != null && fence.lng != null && fence.radius != null -> circleDescriptionTemplate.format(name, fence.radius.toInt(), format4(fence.lat), format4(fence.lng))
        fence.polygon.size >= 3 -> polygonDescriptionTemplate.format(name, fence.polygon.size)
        else -> name
    }
}

private fun format4(value: Double): String = ((value * 10000.0).toLong() / 10000.0).toString()
