package info.cafferata.dicomviewer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cafferata.dicomviewer.model.ParsedDicom
import info.cafferata.dicomviewer.model.WlPreset
import info.cafferata.dicomviewer.ui.theme.Med
import info.cafferata.dicomviewer.ui.theme.MedBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun ViewerScreen(
    parsed: ParsedDicom,
    onExport: (Bitmap, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var frameIdx by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var fps by remember { mutableStateOf(10) }
    var showOverlay by remember { mutableStateOf(true) }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    var presetFrames by remember { mutableStateOf<List<Bitmap>?>(null) }
    var presetNaam by remember { mutableStateOf("Auto") }
    var huidigWc by remember { mutableStateOf(0.0) }
    var huidigWw by remember { mutableStateOf(0.0) }
    var isRendering by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var showFpsMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

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

    fun resetTransform() {
        scale = 1f; offsetX = 0f; offsetY = 0f
    }

    fun pasPresetToe(p: WlPreset) {
        isRendering = true
        val (rawC, rawW) = parsed.huNaarRaw(p.center, p.width)
        scope.launch {
            val nieuw = withContext(Dispatchers.Default) { parsed.render(rawC, rawW) }
            isRendering = false
            if (nieuw.isNotEmpty()) {
                presetFrames = if (p.naam == "Auto") null else nieuw
                presetNaam = p.naam
                huidigWc = p.center
                huidigWw = p.width
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 8f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { resetTransform() },
                            onTap = { showOverlay = !showOverlay },
                        )
                    },
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Med.warn, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.size(12.dp))
                Text("GEEN BEELDDATA", color = Med.textSec, fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }

        if (showOverlay) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    parsed = parsed,
                    frames = frames,
                    isCine = isCine,
                    presetNaam = presetNaam,
                    huidigWc = huidigWc,
                    huidigWw = huidigWw,
                    presetActive = presetFrames != null,
                    isRendering = isRendering,
                    showPresetMenu = showPresetMenu,
                    onTogglePresetMenu = { showPresetMenu = it },
                    onPickPreset = { p -> showPresetMenu = false; if (p == null) { presetFrames = null; presetNaam = "Auto" } else pasPresetToe(p) },
                    showExportMenu = showExportMenu,
                    onToggleExportMenu = { showExportMenu = it },
                    onExport = { fmt -> showExportMenu = false; frame?.let { onExport(it, fmt) } },
                    onDismiss = { isPlaying = false; onDismiss() },
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isCine) {
                    CineBar(
                        frameIdx = frameIdx,
                        frameCount = frames.size,
                        isPlaying = isPlaying,
                        fps = fps,
                        showFpsMenu = showFpsMenu,
                        onToggleFpsMenu = { showFpsMenu = it },
                        onSetFps = { fps = it; showFpsMenu = false },
                        onSeek = { frameIdx = it },
                        onStepBack = { isPlaying = false; frameIdx = max(0, frameIdx - 1) },
                        onStepForward = { isPlaying = false; frameIdx = min(frames.size - 1, frameIdx + 1) },
                        onTogglePlay = { isPlaying = !isPlaying },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    parsed: ParsedDicom,
    frames: List<Bitmap>,
    isCine: Boolean,
    presetNaam: String,
    huidigWc: Double,
    huidigWw: Double,
    presetActive: Boolean,
    isRendering: Boolean,
    showPresetMenu: Boolean,
    onTogglePresetMenu: (Boolean) -> Unit,
    onPickPreset: (WlPreset?) -> Unit,
    showExportMenu: Boolean,
    onToggleExportMenu: (Boolean) -> Unit,
    onExport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Sluiten", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
            }
            Spacer(modifier = Modifier.size(8.dp))

            if (parsed.windowWidth > 0) {
                InfoRow("WC", "%.0f".format(if (!presetActive) parsed.windowCenter else huidigWc))
                InfoRow("WW", "%.0f".format(if (!presetActive) parsed.windowWidth else huidigWw))
                Spacer(modifier = Modifier.size(4.dp))
            }

            if (parsed.kanHervensteren) {
                Box {
                    Row(
                        modifier = Modifier
                            .background(Med.accent.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                            .clickable(enabled = !isRendering) { onTogglePresetMenu(true) }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                    ) {
                        if (isRendering) {
                            CircularProgressIndicator(
                                color = Med.accent,
                                strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
                                modifier = Modifier.size(11.dp),
                            )
                        } else {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = Med.accent, modifier = Modifier.size(11.dp))
                        }
                        Spacer(modifier = Modifier.size(5.dp))
                        Text(presetNaam.uppercase(), color = Med.accent, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                    DropdownMenu(expanded = showPresetMenu, onDismissRequest = { onTogglePresetMenu(false) }) {
                        DropdownMenuItem(text = { Text("Auto (origineel)") }, onClick = { onPickPreset(null) })
                        WlPreset.alle.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.naam}  C${p.center.toInt()} W${p.width.toInt()}") },
                                onClick = { onPickPreset(p) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            if (parsed.patientName.isNotEmpty()) {
                Text(parsed.patientName, color = Color.White, fontSize = 13.sp)
            }
            Row {
                if (parsed.modality.isNotEmpty()) {
                    MedBadge(parsed.modality)
                    Spacer(modifier = Modifier.size(6.dp))
                }
                Text("${parsed.columns}×${parsed.rows}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            if (isCine) {
                Spacer(modifier = Modifier.size(4.dp))
                MedBadge("${frames.size} FRAMES", Med.blue)
            }
            if (frames.isNotEmpty()) {
                Spacer(modifier = Modifier.size(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .background(Med.accent.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                            .clickable { onToggleExportMenu(true) }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                    ) {
                        Text("EXPORT", color = Med.accent, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { onToggleExportMenu(false) }) {
                        DropdownMenuItem(text = { Text("Exporteer als PNG") }, onClick = { onExport("PNG") })
                        DropdownMenuItem(text = { Text("Exporteer als JPEG") }, onClick = { onExport("JPEG") })
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(label, color = Med.textSec, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.size(4.dp))
        Text(value, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun CineBar(
    frameIdx: Int,
    frameCount: Int,
    isPlaying: Boolean,
    fps: Int,
    showFpsMenu: Boolean,
    onToggleFpsMenu: (Boolean) -> Unit,
    onSetFps: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .navigationBarsPadding()
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("1", color = Med.textSec, fontSize = 10.sp)
            Slider(
                value = frameIdx.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..(frameCount - 1).coerceAtLeast(1).toFloat(),
                steps = maxOf(0, frameCount - 2),
                colors = SliderDefaults.colors(thumbColor = Med.accent, activeTrackColor = Med.accent),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Text("$frameCount", color = Med.textSec, fontSize = 10.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onStepBack, modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Vorig frame", tint = Color.White.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.size(16.dp))
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(48.dp).background(Med.accent, CircleShape)) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pauzeer" else "Speel af",
                    tint = Med.bg,
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            IconButton(onClick = onStepForward, modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Volgend frame", tint = Color.White.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("${frameIdx + 1} / $frameCount", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Row(
                    modifier = Modifier
                        .background(Med.accent.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                        .clickable { onToggleFpsMenu(true) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("$fps FPS", color = Med.accent, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                DropdownMenu(expanded = showFpsMenu, onDismissRequest = { onToggleFpsMenu(false) }) {
                    listOf(5, 10, 15, 24, 30).forEach { f ->
                        DropdownMenuItem(text = { Text("$f fps") }, onClick = { onSetFps(f) })
                    }
                }
            }
        }
    }
}
