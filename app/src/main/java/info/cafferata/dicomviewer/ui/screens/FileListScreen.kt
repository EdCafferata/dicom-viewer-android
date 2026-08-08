package info.cafferata.dicomviewer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cafferata.dicomviewer.model.DicomFileInfo
import info.cafferata.dicomviewer.model.SeriesGroup
import info.cafferata.dicomviewer.store.DicomFileViewModel
import info.cafferata.dicomviewer.ui.theme.Med
import info.cafferata.dicomviewer.ui.theme.MedBadge
import info.cafferata.dicomviewer.ui.theme.MedDivider
import info.cafferata.dicomviewer.util.asFileSize
import info.cafferata.dicomviewer.util.timeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: DicomFileViewModel,
    isParsing: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onOpenFile: (DicomFileInfo) -> Unit,
    onOpenSeries: (SeriesGroup) -> Unit,
    onTipJar: () -> Unit = {},
) {
    var fileToDelete by remember { mutableStateOf<DicomFileInfo?>(null) }
    var seriesToDelete by remember { mutableStateOf<SeriesGroup?>(null) }
    var demosHidden by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFile(uri, uri.lastPathSegment ?: "scan.dcm")
    }

    Box(modifier = Modifier.fillMaxSize().background(Med.bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Header(onImport = { importLauncher.launch(arrayOf("*/*")) }, onTipJar = onTipJar)
            MedDivider()

            if (viewModel.files.isEmpty()) {
                EmptyState(
                    demos = viewModel.bundledDemos,
                    demosHidden = demosHidden,
                    onToggleDemos = { demosHidden = !demosHidden },
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                    onOpenFile = onOpenFile,
                )
            } else {
                FileList(
                    viewModel = viewModel,
                    onOpenFile = onOpenFile,
                    onOpenSeries = onOpenSeries,
                    onDeleteFile = { fileToDelete = it },
                    onDeleteSeries = { seriesToDelete = it },
                )
            }
        }

        if (isParsing) {
            LoadingOverlay()
        }
    }

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Bestand verwijderen?") },
            text = { Text("${file.name} wordt permanent verwijderd.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(file); fileToDelete = null }) {
                    Text("Verwijder", color = Med.danger)
                }
            },
            dismissButton = { TextButton(onClick = { fileToDelete = null }) { Text("Annuleer") } },
        )
    }

    seriesToDelete?.let { groep ->
        AlertDialog(
            onDismissRequest = { seriesToDelete = null },
            title = { Text("Serie verwijderen?") },
            text = { Text("${groep.description} — ${groep.files.size} bestanden worden permanent verwijderd.") },
            confirmButton = {
                TextButton(onClick = {
                    groep.files.forEach { viewModel.delete(it) }
                    seriesToDelete = null
                }) { Text("Verwijder", color = Med.danger) }
            },
            dismissButton = { TextButton(onClick = { seriesToDelete = null }) { Text("Annuleer") } },
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Bestand niet ondersteund") },
            text = { Text(errorMessage) },
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun Header(onImport: () -> Unit, onTipJar: () -> Unit = {}) {
    val med = Med
    val rood = Color(0xFFCC1A1A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(med.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = med.accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text("DICOM VIEWER", color = med.textSec, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
            Text("Medical Imaging", color = med.textPri, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onTipJar, modifier = Modifier
            .size(36.dp)
            .background(rood.copy(alpha = 0.12f), CircleShape)) {
            Icon(Icons.Filled.Favorite, contentDescription = "Steun de ontwikkelaar", tint = rood, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.size(10.dp))
        IconButton(onClick = onImport, modifier = Modifier
            .size(36.dp)
            .background(med.accent.copy(alpha = 0.12f), CircleShape)) {
            Icon(Icons.Filled.Add, contentDescription = "Bestand importeren", tint = med.accent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FileList(
    viewModel: DicomFileViewModel,
    onOpenFile: (DicomFileInfo) -> Unit,
    onOpenSeries: (SeriesGroup) -> Unit,
    onDeleteFile: (DicomFileInfo) -> Unit,
    onDeleteSeries: (SeriesGroup) -> Unit,
) {
    val med = Med
    val groepen = viewModel.series.ifEmpty {
        viewModel.files.map { SeriesGroup(id = it.key, description = it.name, modality = "", files = listOf(it)) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (viewModel.recent.isNotEmpty()) {
            item { SectionHeader("LAATST GEOPEND") }
            items(viewModel.recent, key = { it.id }) { file ->
                FileRow(file = file, onClick = {
                    viewModel.registreerOpening(file)
                    onOpenFile(file)
                })
            }
        }

        item {
            SectionHeader(
                if (viewModel.series.isEmpty()) "${viewModel.files.size} BESTANDEN"
                else "${viewModel.series.size} ITEMS · ${viewModel.files.size} BESTANDEN",
                title = "RECENTE SCANS",
            )
        }
        items(groepen, key = { it.id }) { groep ->
            if (groep.isSeries) {
                SeriesRow(groep = groep, onClick = { onOpenSeries(groep) }, onDelete = { onDeleteSeries(groep) })
            } else {
                val file = groep.files[0]
                FileRow(
                    file = file,
                    onClick = { viewModel.registreerOpening(file); onOpenFile(file) },
                    onDelete = { onDeleteFile(file) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(trailing: String, title: String = "") {
    val med = Med
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(med.bg)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Text(title.ifEmpty { trailing }, color = med.textSec, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.weight(1f))
        if (title.isNotEmpty()) Text(trailing, color = med.textDim, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SeriesRow(groep: SeriesGroup, onClick: () -> Unit, onDelete: () -> Unit) {
    val med = Med
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(med.card)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(med.blue.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Layers, contentDescription = null, tint = med.blue) }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(groep.description, color = med.textPri, fontSize = 14.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (groep.modality.isNotEmpty()) {
                        MedBadge(groep.modality)
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                    MedBadge("SERIE", med.blue.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("${groep.files.size} slices · ${groep.totalSize.asFileSize()}", color = med.textSec, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Verwijder serie", tint = med.textDim, modifier = Modifier.size(16.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = med.textDim, modifier = Modifier.size(14.dp))
        }
        MedDivider()
    }
}

@Composable
private fun FileRow(file: DicomFileInfo, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    val med = Med
    val (icon, color) = when (file.ext) {
        "DCM", "DICOM" -> Icons.Filled.GraphicEq to med.accent
        "PNG", "JPG", "JPEG" -> Icons.Filled.Photo to med.blue
        else -> Icons.Filled.Description to med.textSec
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(med.card)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = color) }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, color = med.textPri, fontSize = 14.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedBadge(file.ext)
                    Spacer(modifier = Modifier.size(6.dp))
                    if (file.isDemo) {
                        MedBadge("DEMO", med.blue.copy(alpha = 0.7f))
                    } else {
                        Text("${file.fileSize.asFileSize()} · ${file.modifiedDate.timeAgo()}", color = med.textSec, fontSize = 11.sp)
                    }
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Verwijder", tint = med.textDim, modifier = Modifier.size(16.dp))
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = med.textDim, modifier = Modifier.size(14.dp))
        }
        MedDivider()
    }
}

@Composable
private fun EmptyState(
    demos: List<DicomFileInfo>,
    demosHidden: Boolean,
    onToggleDemos: () -> Unit,
    onImport: () -> Unit,
    onOpenFile: (DicomFileInfo) -> Unit,
) {
    val med = Med
    Column(modifier = Modifier.fillMaxSize().background(med.bg).padding(top = 36.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(80.dp).background(med.accent.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = med.accent.copy(alpha = 0.7f), modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.size(20.dp))
            Text("EIGEN SCAN TOEVOEGEN", color = med.textSec, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                "Importeer een DICOM of medisch beeldbestand.",
                color = med.textSec, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
            Spacer(modifier = Modifier.size(20.dp))
            Row(
                modifier = Modifier
                    .background(med.accent, RoundedCornerShape(8.dp))
                    .clickable(onClick = onImport)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = med.bg, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("BESTAND IMPORTEREN", color = med.bg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
            }
        }

        if (demos.isNotEmpty()) {
            Spacer(modifier = Modifier.size(32.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("VOORBEELDBESTANDEN", color = med.textSec, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (demosHidden) "TOON" else "VERBERG",
                    color = med.accent, fontSize = 10.sp, letterSpacing = 1.sp,
                    modifier = Modifier.clickable(onClick = onToggleDemos),
                )
            }
            if (!demosHidden) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    demos.forEach { file ->
                        FileRow(file = file, onClick = { onOpenFile(file) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    val med = Med
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.background(med.surface, RoundedCornerShape(16.dp)).padding(32.dp),
        ) {
            CircularProgressIndicator(color = med.accent)
            Spacer(modifier = Modifier.height(16.dp))
            Text("LADEN…", color = med.textSec, fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}
