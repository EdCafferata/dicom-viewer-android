package info.cafferata.dicomviewer

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import info.cafferata.dicomviewer.billing.TipStore
import info.cafferata.dicomviewer.model.DicomError
import info.cafferata.dicomviewer.model.DicomFileInfo
import info.cafferata.dicomviewer.model.ParsedDicom
import info.cafferata.dicomviewer.model.SeriesGroup
import info.cafferata.dicomviewer.parser.DicomParser
import info.cafferata.dicomviewer.store.DicomFileViewModel
import info.cafferata.dicomviewer.ui.screens.FileListScreen
import info.cafferata.dicomviewer.ui.screens.TipJarCoachMark
import info.cafferata.dicomviewer.ui.screens.TipJarScreen
import info.cafferata.dicomviewer.ui.screens.ViewerScreen
import info.cafferata.dicomviewer.ui.theme.DicomViewerTheme
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val fileViewModel: DicomFileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DicomViewerTheme {
                DicomViewerApp(fileViewModel)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DicomViewerApp(viewModel: DicomFileViewModel) {
    var parsedDicom by remember { mutableStateOf<ParsedDicom?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val tipStore = remember { TipStore(context) }
    var showTipJar by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("dicomviewer", android.content.Context.MODE_PRIVATE) }
    var showCoachMark by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!prefs.getBoolean("tipJarCoachMarkShown", false)) {
            delay(800)
            showCoachMark = true
            prefs.edit().putBoolean("tipJarCoachMarkShown", true).apply()
        }
    }

    fun openFile(file: DicomFileInfo) {
        isParsing = true
        scope.launch {
            try {
                val parsed = withContext(Dispatchers.Default) {
                    DicomParser.parse(viewModel.readBytes(file))
                }
                isParsing = false
                parsedDicom = parsed
            } catch (e: DicomError) {
                isParsing = false
                errorMessage = e.message ?: "Dit bestandsformaat wordt niet ondersteund."
            } catch (e: Exception) {
                isParsing = false
                errorMessage = e.message ?: "Onbekende fout bij het openen van dit bestand."
            }
        }
    }

    /** Opens a whole series: parses each file (in instance order) and concatenates all frames — the viewer shows them with a slider/cine player. */
    fun openSeries(groep: SeriesGroup) {
        groep.files.firstOrNull()?.let { viewModel.registreerOpening(it) }
        isParsing = true
        scope.launch {
            val (eerste, alleFrames) = withContext(Dispatchers.Default) {
                var basis: ParsedDicom? = null
                val frames = mutableListOf<Bitmap>()
                for (file in groep.files) {
                    val parsed = runCatching { DicomParser.parse(viewModel.readBytes(file)) }.getOrNull() ?: continue
                    if (basis == null) basis = parsed
                    frames.addAll(parsed.frames)
                }
                basis to frames
            }
            isParsing = false
            if (eerste == null || alleFrames.isEmpty()) {
                errorMessage = "Geen leesbare beelden in deze serie."
            } else {
                parsedDicom = eerste.copy(frames = alleFrames)
            }
        }
    }

    fun exportFrame(bitmap: Bitmap, format: String) {
        val ext = if (format == "PNG") "png" else "jpg"
        val compressFormat = if (format == "PNG") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "DICOM-frame-${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { bitmap.compress(compressFormat, 92, it) }
        val uri: Uri = FileProvider.getUriForFile(context, "info.cafferata.dicomviewer.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (format == "PNG") "image/png" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    if (parsedDicom != null) {
        ViewerScreen(
            parsed = parsedDicom!!,
            onExport = { bitmap, format -> exportFrame(bitmap, format) },
            onDismiss = { parsedDicom = null },
        )
    } else {
        FileListScreen(
            viewModel = viewModel,
            isParsing = isParsing,
            errorMessage = errorMessage,
            onDismissError = { errorMessage = null },
            onOpenFile = { openFile(it) },
            onOpenSeries = { openSeries(it) },
            onTipJar = { showTipJar = true },
        )
    }

    if (showTipJar) {
        TipJarScreen(store = tipStore, onDismiss = { showTipJar = false })
    }

    if (showCoachMark) {
        TipJarCoachMark(
            onDonate = { showCoachMark = false; showTipJar = true },
            onDismiss = { showCoachMark = false },
        )
    }
}
