package com.example.transportapp.feature.consignment.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.Caption
import com.example.transportapp.core.designsystem.component.GroupHeading
import com.example.transportapp.core.designsystem.component.TransportTextField
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.TransportTypeScale

@Composable
fun StatusUpdateSheet(
    biltyNo: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    viewModel: StatusUpdateSheetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.saved) {
        if (state.saved) onSave()
    }
    StatusUpdateSheetContent(
        state = state,
        biltyNo = biltyNo,
        onEvent = viewModel::onEvent,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun StatusUpdateSheetContent(
    state: StatusUpdateSheetUiState,
    biltyNo: String,
    onEvent: (StatusUpdateSheetEvent) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val isHold = state.isHold
    val primary = state.selected
    val context = androidx.compose.ui.platform.LocalContext.current
    var signaturePath by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.Path?>(null) }

    // S19 — the real capture paths: Gallery via the system Photo Picker, Camera via
    // TakePicture onto a FileProvider uri. Both funnel into PhotoPicked for import.
    val cameraUris = remember { mutableStateMapOf<android.net.Uri, java.io.File>() }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            val source = cameraUris.entries.firstOrNull { it.value.exists() }?.key
            if (source != null) onEvent(StatusUpdateSheetEvent.PhotoPicked(source))
        }
    }
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onEvent(StatusUpdateSheetEvent.PhotoPicked(it)) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 32.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
        )

        Text("Update status", style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp))
        Text(
            "$biltyNo${if (state.contextLine.isNotEmpty()) " · " + state.contextLine else ""}",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))
        GroupHeading("What happened", modifier = Modifier.padding(bottom = 8.dp))

        if (primary != null) {
            // Primary event card — the first §7.1-legal continuation.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .clickable { onEvent(StatusUpdateSheetEvent.SelectOption(primary)) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(primary.label, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(primary.detail, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Caption("Or choose another")
        Spacer(Modifier.height(8.dp))

        // The remaining legal events, hold-family options drawn in the error colours.
        state.options.filter { it != primary }.forEach { option ->
            Row {
                StatusChip(
                    label = option.label,
                    selected = state.selected == option,
                    error = option.holdPath,
                    onClick = { onEvent(StatusUpdateSheetEvent.SelectOption(option)) }
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        // Hold path
        if (isHold) {
            Spacer(Modifier.height(16.dp))
            GroupHeading("Why it's held", modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldReason.entries.forEach { reason ->
                    StatusChip(
                        label = reason.label,
                        selected = state.holdReason == reason,
                        error = true,
                        filled = true,
                        onClick = { onEvent(StatusUpdateSheetEvent.SelectHoldReason(reason)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        GroupHeading("Where", modifier = Modifier.padding(bottom = 8.dp))
        TransportTextField(
            value = state.location,
            onValueChange = { onEvent(StatusUpdateSheetEvent.ChangeLocation(it)) },
            label = "Location",
            leadingIcon = Icons.Rounded.LocationOn
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(StatusUpdateSheetEvent.UseMyLocation) }
                .padding(top = 8.dp)
                .align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text("Use my location", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text("Recorded to the nearest town, not an exact position.", style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(24.dp))
        GroupHeading(if (isHold) "Remark · Required" else "Remark · Optional", modifier = Modifier.padding(bottom = 8.dp))
        TransportTextField(
            value = state.remark,
            onValueChange = { onEvent(StatusUpdateSheetEvent.ChangeRemark(it)) },
            label = "Anything the office should know",
            singleLine = false,
            maxLines = 3
        )

        Spacer(Modifier.height(24.dp))
        GroupHeading("Photo · Optional", modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CaptureTile(Icons.Rounded.PhotoCamera, "Camera", enabled = !state.photoAttached, onClick = {
                // TakePicture writes to a FileProvider uri in cache; the importer copies it
                // into app files on the save path.
                val dir = java.io.File(context.cacheDir, "pod").apply { mkdirs() }
                val file = java.io.File(dir, "pod-${System.currentTimeMillis()}.jpg")
                val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                cameraUris[uri] = file
                cameraLauncher.launch(uri)
            })
            CaptureTile(Icons.Rounded.PhotoLibrary, "Gallery", enabled = !state.photoAttached, onClick = {
                galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
            Text(
                if (state.photoAttached) "Photo attached — it rides the POD." else "Stored on this phone and uploaded when there's signal.",
                style = TransportTypeScale.bodySmall,
                color = if (state.photoAttached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
        }

        // S15 POD capture: a delivered save requires the consignee's name and a signed pad.
        if (state.isDelivery) {
            Spacer(Modifier.height(24.dp))
            GroupHeading("Proof of delivery", modifier = Modifier.padding(bottom = 8.dp))
            TransportTextField(
                value = state.consigneeName,
                onValueChange = { onEvent(StatusUpdateSheetEvent.ChangeConsigneeName(it)) },
                label = "Received by (consignee name)"
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                com.example.transportapp.core.designsystem.component.SignaturePad(
                    modifier = Modifier.fillMaxSize(),
                    clearSignal = state.signatureClearSignal,
                    onPathChange = { signaturePath = it },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.TextButton(onClick = { onEvent(StatusUpdateSheetEvent.ClearSignature) }) {
                    Text("Clear signature", style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (isHold) {
            Text(
                "The office and the consignor are notified. This can't be undone, only followed by another event.",
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        if (state.error != null) {
            Text(
                state.error,
                style = TransportTypeScale.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        AppPrimaryButton(
            if (isHold) "Hold this consignment" else "Save update",
            onClick = {
                if (state.isDelivery) {
                    // S15: export the signed pad to a PNG first; the save carries its ref.
                    val path = signaturePath ?: return@AppPrimaryButton
                    val file = java.io.File(context.filesDir, "signatures").apply { mkdirs() }.resolve(
                        "sig-${biltyNo.replace("/", "-")}-${System.currentTimeMillis()}.png"
                    )
                    val bitmap = android.graphics.Bitmap.createBitmap(640, 320, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.save(); canvas.scale(2f, 2f)
                    val androidPath = android.graphics.Path()
                    path.asAndroidPath()
                    val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; style = android.graphics.Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true }
                    canvas.drawPath(androidPath, paint)
                    canvas.restore()
                    file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                    onEvent(StatusUpdateSheetEvent.SaveWithSignature("signatures/${file.name}"))
                } else {
                    onEvent(StatusUpdateSheetEvent.Save)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Saves offline. Syncs when you reconnect.",
            style = TransportTypeScale.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    selected: Boolean,
    error: Boolean,
    onClick: () -> Unit,
    filled: Boolean = false
) {
    val container = when {
        filled && selected -> MaterialTheme.colorScheme.errorContainer
        filled -> MaterialTheme.colorScheme.errorContainer
        selected && !error -> MaterialTheme.colorScheme.secondaryContainer
        selected && error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val labelColor = when {
        error -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected || error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(percent = 100))
            .then(if (selected || error) Modifier.border(1.dp, borderColor, RoundedCornerShape(percent = 100)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = TransportTypeScale.labelMedium, color = labelColor)
    }
}

@Composable
private fun CaptureTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .size(88.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Text(label, style = TransportTypeScale.labelMedium, color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline)
    }
}
