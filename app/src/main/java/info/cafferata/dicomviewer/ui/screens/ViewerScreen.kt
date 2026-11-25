package info.cafferata.dicomviewer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cafferata.dicomviewer.R
import info.cafferata.dicomviewer.model.ParsedDicom
import info.cafferata.dicomviewer.model.WlPreset
import info.cafferata.dicomviewer.ui.theme.Med
import info.cafferata.dicomviewer.ui.theme.medGlass
import info.cafferata.dicomviewer.ui.theme.medGlassCircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val invertMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

@Composable
private fun presetTitle(p: WlPreset): String = when (p.naam) {
    "Abdomen" -> stringResource(R.string.preset_abdomen)
    "Long" -> stringResource(R.string.preset_lung)
    "Bot" -> stringResource(R.string.preset_bone)
    "Hersenen" -> stringResource(R.string.preset_brain)
    else -> p.naam
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    parsed: ParsedDicom,
    title: String,
    embedded: Boolean = false,
    onExport: (Bitmap, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var frameIdx by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var fps by remember { mutableStateOf(10) }
    var showOverlay by remember { mutableStateOf(true) }
    var inverted by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showFpsMenu by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    var presetFrames by remember { mutableStateOf<List<Bitmap>?>(null) }
    var presetNaam by remember { mutableStateOf("Auto") }
    var huidigWc by remember { mutableStateOf(0.0) }
    var huidigWw by remember { mutableStateOf(0.0) }
    var isRendering by remember { mutableStateOf(false) }

    // Measuring (#4): points in image pixels (x = column, y = row)
    var measuring by remember { mutableStateOf(false) }
    var points by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val frames = presetFrames ?: parsed.frames
    val frame = frames.getOrNull(min(frameIdx, frames.size - 1))
    val isCine = frames.size > 1

    LaunchedEffect(isPlaying, fps, frames.size) {
        if (isPlaying) {
            while (true) {
                delay(1000L / fps)
                frameIdx = (frameIdx + 1) % frames.size
            }
        }
    }

    fun pasPresetToe(p: WlPreset?) {
        if (p == null) { presetFrames = null; presetNaam = "Auto"; return }
        isRendering = true
        val (rawC, rawW) = parsed.huNaarRaw(p.center, p.width)
        scope.launch {
            val nieuw = withContext(Dispatchers.Default) { parsed.render(rawC, rawW) }
            isRendering = false
            if (nieuw.isNotEmpty()) {
                presetFrames = nieuw; presetNaam = p.naam; huidigWc = p.center; huidigWw = p.width
            }
        }
    }

    val shownWc = if (presetFrames == null) parsed.windowCenter else huidigWc
    val shownWw = if (presetFrames == null) parsed.windowWidth else huidigWw
    val nf1 = remember { NumberFormat.getNumberInstance().apply { minimumFractionDigits = 1; maximumFractionDigits = 1 } }
    val distanceText: String? = if (points.size == 2) {
        val dx = (points[1].x - points[0].x).toDouble()
        val dy = (points[1].y - points[0].y).toDouble()
        val ps = parsed.pixelSpacing
        if (ps != null) nf1.format(sqrt((dx * ps.kolom) * (dx * ps.kolom) + (dy * ps.rij) * (dy * ps.rij))) + " mm"
        else "${sqrt(dx * dx + dy * dy).toInt()} px"
    } else null
    val measureHint = when {
        points.isEmpty() -> stringResource(R.string.measure_start)
        points.size == 1 -> stringResource(R.string.measure_end)
        parsed.pixelSpacing == null -> stringResource(R.string.measure_no_spacing)
        parsed.pixelSpacing.opDetector -> stringResource(R.string.measure_detector)
        else -> stringResource(R.string.measure_file)
    }
    val headTitle = title.ifEmpty { parsed.modality.ifEmpty { stringResource(R.string.scan_fallback) } }
    val headDetails = listOfNotNull(
        parsed.modality.takeIf { it.isNotEmpty() },
        "${parsed.columns} × ${parsed.rows}",
        if (isCine) stringResource(R.string.n_frames, frames.size) else null,
    ).joinToString(" · ")
    val imageDescription = stringResource(R.string.medical_image)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (frame != null) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 8f)
                            offsetX += pan.x * scale
                            offsetY += pan.y * scale
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Fitted image rectangle, so taps can be mapped to image pixels.
                val density = LocalDensity.current
                val boxW = with(density) { maxWidth.toPx() }
                val boxH = with(density) { maxHeight.toPx() }
                val fit = min(boxW / parsed.columns, boxH / parsed.rows)
                val imgW = parsed.columns * fit
                val imgH = parsed.rows * fit
                Box(
                    modifier = Modifier
                        .size(with(density) { imgW.toDp() }, with(density) { imgH.toDp() })
                        .semantics { contentDescription = imageDescription }
                        .pointerInput(measuring) {
                            detectTapGestures(
                                onDoubleTap = { if (!measuring) { scale = 1f; offsetX = 0f; offsetY = 0f } },
                                onTap = { pos ->
                                    if (measuring) {
                                        val p = Offset(
                                            (pos.x / fit).coerceIn(0f, parsed.columns.toFloat()),
                                            (pos.y / fit).coerceIn(0f, parsed.rows.toFloat()),
                                        )
                                        points = if (points.size >= 2) listOf(p) else points + p
                                    } else {
                                        showOverlay = !showOverlay
                                    }
                                },
                            )
                        },
                ) {
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = if (inverted) ColorFilter.colorMatrix(invertMatrix) else null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (points.isNotEmpty()) {
                        val s = max(scale, 1f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pts = points.map { Offset(it.x * fit, it.y * fit) }
                            if (pts.size == 2) drawLine(Med.measure, pts[0], pts[1], strokeWidth = 2.dp.toPx() / s)
                            pts.forEach {
                                drawCircle(Color.Black.copy(alpha = 0.35f), radius = 7.dp.toPx() / s, center = it)
                                drawCircle(Med.measure, radius = 7.dp.toPx() / s, center = it, style = Stroke(2.dp.toPx() / s))
                            }
                        }
                        if (distanceText != null) {
                            val mid = Offset((points[0].x + points[1].x) / 2 * fit, (points[0].y + points[1].y) / 2 * fit)
                            Text(
                                distanceText,
                                color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = mid.x - 40.dp.toPx()
                                        translationY = mid.y - 30.dp.toPx() / s
                                        scaleX = 1f / s; scaleY = 1f / s
                                    }
                                    .background(Med.measure, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Med.warn, modifier = Modifier.size(36.dp))
                Spacer(Modifier.size(12.dp))
                Text(stringResource(R.string.no_image_data), color = Med.textSec, fontSize = 15.sp)
            }
        }

        if (showOverlay) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!embedded) {
                            IconButton(onClick = { isPlaying = false; onDismiss() }, modifier = Modifier.size(44.dp).medGlassCircle()) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Med.textPri)
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).height(44.dp).medGlass(22.dp).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(headTitle, color = Med.textPri, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(headDetails, color = Color(0xFFAEB9CB), fontSize = 11.sp, maxLines = 1)
                        }
                        IconButton(onClick = { showInfo = true }, modifier = Modifier.size(44.dp).medGlassCircle()) {
                            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.scan_details), tint = Med.textPri)
                        }
                    }
                    if (parsed.windowWidth > 0) {
                        Spacer(Modifier.size(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WlChip("W", shownWw)
                            WlChip("L", shownWc)
                            if (inverted) {
                                Text(stringResource(R.string.inverted), color = Med.textPri, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.medGlass(12.dp).padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                    }
                    if (measuring) {
                        Spacer(Modifier.size(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().medGlass(18.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Filled.Straighten, contentDescription = null, tint = Med.measure)
                            Spacer(Modifier.size(10.dp))
                            Text(measureHint, color = Med.textPri, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (points.isNotEmpty()) {
                                TextButton(onClick = { points = emptyList() }) {
                                    Text(stringResource(R.string.clear), color = Med.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Control panel
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .medGlass(30.dp)
                        .padding(14.dp),
                ) {
                    if (parsed.kanHervensteren) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            PresetChip(stringResource(R.string.preset_auto), presetFrames == null, !isRendering) { pasPresetToe(null) }
                            WlPreset.alle.forEach { p ->
                                PresetChip(presetTitle(p), presetFrames != null && presetNaam == p.naam, !isRendering) { pasPresetToe(p) }
                            }
                            if (isRendering) CircularProgressIndicator(color = Med.accentText, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                    if (isCine) {
                        Column {
                            Slider(
                                value = frameIdx.toFloat(),
                                onValueChange = { frameIdx = it.toInt() },
                                valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Med.accent, inactiveTrackColor = Color.White.copy(alpha = 0.16f)),
                            )
                            Row {
                                Text(stringResource(R.string.frame_x_of_y, frameIdx + 1, frames.size), color = Color(0xFFAEB9CB), fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Text(stringResource(R.string.fps_n, fps), color = Color(0xFFAEB9CB), fontSize = 12.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundButton(Icons.Filled.Contrast, stringResource(R.string.invert_contrast), active = inverted, size = 44) { inverted = !inverted }
                        Spacer(Modifier.size(8.dp))
                        RoundButton(
                            Icons.Filled.Straighten,
                            stringResource(if (measuring) R.string.stop_measuring else R.string.measure_distance),
                            active = measuring, size = 44,
                        ) { measuring = !measuring; if (!measuring) points = emptyList() }
                        Spacer(Modifier.weight(1f))
                        if (isCine) {
                            RoundButton(Icons.Filled.SkipPrevious, null, size = 46) { isPlaying = false; frameIdx = max(0, frameIdx - 1) }
                            Spacer(Modifier.size(10.dp))
                            IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(62.dp).clip(CircleShape).background(Med.accent)) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                    tint = Med.onAccent, modifier = Modifier.size(30.dp),
                                )
                            }
                            Spacer(Modifier.size(10.dp))
                            RoundButton(Icons.Filled.SkipNext, null, size = 46) { isPlaying = false; frameIdx = min(frames.size - 1, frameIdx + 1) }
                            Spacer(Modifier.weight(1f))
                            Box {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))
                                        .clickable { showFpsMenu = true }
                                        .semantics { contentDescription = "fps" },
                                    contentAlignment = Alignment.Center,
                                ) { Text("$fps", color = Med.textPri, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                DropdownMenu(expanded = showFpsMenu, onDismissRequest = { showFpsMenu = false }) {
                                    listOf(5, 10, 15, 24, 30).forEach { f ->
                                        DropdownMenuItem(text = { Text(stringResource(R.string.fps_menu, f)) }, onClick = { fps = f; showFpsMenu = false })
                                    }
                                }
                            }
                        } else {
                            Text(stringResource(R.string.pinch_hint), color = Color(0xFFAEB9CB), fontSize = 12.sp, modifier = Modifier.weight(3f))
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        ModalBottomSheet(onDismissRequest = { showInfo = false }, containerColor = Color(0xFF111827)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(headTitle, color = Med.textPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Med.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (parsed.patientName.isNotEmpty()) InfoLine(stringResource(R.string.patient), parsed.patientName)
                    if (parsed.modality.isNotEmpty()) InfoLine(stringResource(R.string.modality), parsed.modality)
                    InfoLine(stringResource(R.string.size), "${parsed.columns} × ${parsed.rows}")
                    InfoLine(stringResource(R.string.frames), "${frames.size}")
                    if (parsed.windowWidth > 0) InfoLine(stringResource(R.string.wl), "%.0f / %.0f".format(shownWw, shownWc))
                    parsed.pixelSpacing?.let { ps ->
                        val nf3 = NumberFormat.getNumberInstance().apply { minimumFractionDigits = 3; maximumFractionDigits = 3 }
                        InfoLine(stringResource(if (ps.opDetector) R.string.pixel_spacing_detector else R.string.pixel_spacing), nf3.format(ps.kolom) + " mm")
                    }
                }
                if (frame != null) {
                    Text(stringResource(R.string.export), color = Med.textSec, fontSize = 13.sp)
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Med.card)) {
                        Text(stringResource(R.string.export_png), color = Med.accentText, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth().clickable { showInfo = false; onExport(frame, "PNG") }.padding(16.dp))
                        Text(stringResource(R.string.export_jpeg), color = Med.accentText, fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth().clickable { showInfo = false; onExport(frame, "JPEG") }.padding(16.dp))
                    }
                    Text(stringResource(R.string.export_footer), color = Med.textDim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, color = Med.textSec, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Med.textPri, fontSize = 15.sp)
    }
}

@Composable
private fun WlChip(label: String, value: Double) {
    Row(modifier = Modifier.medGlass(12.dp).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = Med.textPri, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(4.dp))
        Text("%.0f".format(value), color = Med.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PresetChip(text: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) Med.textPri else Color.Transparent)
            .border(1.dp, if (active) Color.Transparent else Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (active) Color(0xFF0A0E15) else Color(0xFFDCE3EE), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String?,
    active: Boolean = false,
    size: Int,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size.dp).clip(CircleShape).background(if (active) Med.accent else Color.White.copy(alpha = 0.10f)),
    ) {
        Icon(icon, contentDescription = description, tint = if (active) Med.onAccent else Med.textPri)
    }
}
