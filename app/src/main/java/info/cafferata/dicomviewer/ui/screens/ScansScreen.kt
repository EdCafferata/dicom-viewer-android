package info.cafferata.dicomviewer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cafferata.dicomviewer.R
import info.cafferata.dicomviewer.model.DicomFileInfo
import info.cafferata.dicomviewer.model.SeriesGroup
import info.cafferata.dicomviewer.store.DicomFileViewModel
import info.cafferata.dicomviewer.ui.theme.Med
import info.cafferata.dicomviewer.ui.theme.MedBadge
import info.cafferata.dicomviewer.ui.theme.medCard
import info.cafferata.dicomviewer.ui.theme.medGlassCircle
import info.cafferata.dicomviewer.util.ThumbCache
import info.cafferata.dicomviewer.util.asFileSize
import info.cafferata.dicomviewer.util.timeAgo

/** Short list title for a bundled demo. */
@Composable
fun demoTitle(file: DicomFileInfo): String = when (file.name) {
    "cag_voor_ingreep" -> stringResource(R.string.demo_before)
    "cag_tijdens_ingreep" -> stringResource(R.string.demo_during)
    "cag_na_ingreep" -> stringResource(R.string.demo_after)
    else -> file.name
}

@Composable
fun ScreenHeader(title: String, subtitle: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Med.textPri, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Med.textSec, fontSize = 15.sp)
        }
        Box(modifier = Modifier.padding(bottom = 2.dp)) { trailing() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Med.textPri, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScansScreen(
    viewModel: DicomFileViewModel,
    thumbs: ThumbCache,
    isParsing: Boolean,
    selectedKey: String?,
    onOpenFile: (DicomFileInfo, String) -> Unit,
    onOpenSeries: (SeriesGroup) -> Unit,
    onTipJar: () -> Unit,
    onOpenLearn: () -> Unit,
) {
    var fileToDelete by remember { mutableStateOf<DicomFileInfo?>(null) }
    var seriesToDelete by remember { mutableStateOf<SeriesGroup?>(null) }
    var demosHidden by rememberSaveable { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFile(uri, uri.lastPathSegment ?: "scan.dcm")
    }
    val import = { importLauncher.launch(arrayOf("*/*")) }

    val demos = viewModel.bundledDemos
    val subtitle = if (viewModel.files.isEmpty()) {
        stringResource(R.string.n_sample_scans, demos.size)
    } else {
        val n = if (viewModel.series.isEmpty()) viewModel.files.size else viewModel.series.size
        if (n == 1) stringResource(R.string.one_own_scan) else stringResource(R.string.n_own_scans, n)
    }
    val groepen = viewModel.series.ifEmpty {
        viewModel.files.map { SeriesGroup(id = it.key, description = it.name, modality = "", files = listOf(it)) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Med.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenHeader(stringResource(R.string.tab_scans), subtitle) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = onTipJar, modifier = Modifier.size(44.dp).medGlassCircle()) {
                            Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.tip_jar), tint = Med.heart)
                        }
                        IconButton(onClick = import, modifier = Modifier.size(44.dp).medGlassCircle()) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.import_scan), tint = Med.textPri)
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
            }

            if (viewModel.files.isEmpty()) {
                item { ImportCard(onImport = import) }
            } else {
                if (viewModel.recent.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.recently_opened), Modifier.padding(top = 8.dp)) }
                    items(viewModel.recent, key = { "r" + it.key }) { file ->
                        ScanRow(file, file.name, thumbs, selected = selectedKey == file.key, onClick = { onOpenFile(file, file.name) })
                    }
                }
                item { SectionTitle(stringResource(R.string.my_scans), Modifier.padding(top = 8.dp)) }
                items(groepen, key = { "g" + it.id }) { groep ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        val first = groep.files[0]
                        ScanRow(
                            file = first,
                            title = if (groep.isSeries) groep.description else first.name,
                            thumbs = thumbs,
                            series = groep.takeIf { it.isSeries },
                            selected = selectedKey == first.key,
                            onClick = { if (groep.isSeries) onOpenSeries(groep) else onOpenFile(first, first.name) },
                            onLongClick = { menu = true },
                        )
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (groep.isSeries) stringResource(R.string.delete_series_n, groep.files.size)
                                        else stringResource(R.string.delete),
                                        color = Med.danger,
                                    )
                                },
                                onClick = {
                                    menu = false
                                    if (groep.isSeries) seriesToDelete = groep else fileToDelete = first
                                },
                            )
                        }
                    }
                }
            }

            if (demos.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionTitle(stringResource(R.string.sample_scans))
                            Text(stringResource(R.string.sample_scans_sub), color = Med.textSec, fontSize = 13.sp)
                        }
                        TextButton(onClick = { demosHidden = !demosHidden }) {
                            Text(
                                stringResource(if (demosHidden) R.string.show else R.string.hide),
                                color = Med.accentText, fontSize = 15.sp,
                            )
                        }
                    }
                }
                if (!demosHidden) {
                    items(demos, key = { "d" + it.key }) { file ->
                        val title = demoTitle(file)
                        ScanRow(file, title, thumbs, selected = selectedKey == file.key, onClick = { onOpenFile(file, title) })
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Med.blue.copy(alpha = 0.14f))
                        .border(1.dp, Med.blue.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                        .combinedClickable(onClick = onOpenLearn)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color(0xFF8DB8FF))
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.learn_tip), color = Color(0xFFD6E4FF), fontSize = 14.sp)
                }
            }
        }

        if (isParsing) LoadingOverlay()
    }

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.delete_file_q)) },
            text = { Text(stringResource(R.string.delete_file_msg, file.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(file); fileToDelete = null }) {
                    Text(stringResource(R.string.delete), color = Med.danger)
                }
            },
            dismissButton = { TextButton(onClick = { fileToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    seriesToDelete?.let { groep ->
        AlertDialog(
            onDismissRequest = { seriesToDelete = null },
            title = { Text(stringResource(R.string.delete_series_q)) },
            text = { Text(stringResource(R.string.delete_series_msg, groep.description, groep.files.size)) },
            confirmButton = {
                TextButton(onClick = { groep.files.forEach { viewModel.delete(it) }; seriesToDelete = null }) {
                    Text(stringResource(R.string.delete), color = Med.danger)
                }
            },
            dismissButton = { TextButton(onClick = { seriesToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ImportCard(onImport: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().medCard(24.dp).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).background(Med.accent.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.NoteAdd, contentDescription = null, tint = Med.accentText) }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(stringResource(R.string.open_own_scan), color = Med.textPri, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.open_own_scan_sub), color = Med.textSec, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.size(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .clip(CircleShape)
                .background(Med.accent)
                .combinedClickable(onClick = onImport),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.choose_dicom), color = Med.onAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScanRow(
    file: DicomFileInfo,
    title: String,
    thumbs: ThumbCache,
    series: SeriesGroup? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    LaunchedEffect(file.key) { thumbs.load(file) }
    val info = thumbs.info[file.key]
    val modality = series?.modality?.takeIf { it.isNotEmpty() } ?: info?.modality.orEmpty()
    val meta = when {
        series != null -> stringResource(R.string.series_meta, series.files.size, series.totalSize.asFileSize())
        info != null && info.frames > 1 -> stringResource(R.string.cine_frames, info.frames)
        info != null && info.size.isNotEmpty() -> info.size
        file.isDemo -> ""
        else -> file.fileSize.asFileSize()
    } + if (!file.isDemo && series == null) " · " + file.modifiedDate.timeAgo() else ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .medCard()
            .then(if (selected) Modifier.border(2.dp, Med.accent, RoundedCornerShape(20.dp)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0B0F16))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val img = info?.image
            if (img != null) {
                Image(img.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(if (series != null) Icons.Filled.Layers else Icons.Filled.GraphicEq, contentDescription = null, tint = Med.accentText)
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Med.textPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (modality.isNotEmpty()) {
                    MedBadge(modality)
                    Spacer(Modifier.size(6.dp))
                }
                Text(meta, color = Med.textSec, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Med.textDim)
    }
}

@Composable
private fun LoadingOverlay() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(min = 160.dp).clip(RoundedCornerShape(24.dp)).background(Med.glass).padding(28.dp),
        ) {
            CircularProgressIndicator(color = Med.accentText)
            Spacer(Modifier.size(14.dp))
            Text(stringResource(R.string.loading_scan), color = Med.textPri, fontSize = 15.sp)
        }
    }
}
