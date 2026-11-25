package info.cafferata.dicomviewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import info.cafferata.dicomviewer.billing.TipStore
import info.cafferata.dicomviewer.model.DicomError
import info.cafferata.dicomviewer.model.DicomFileInfo
import info.cafferata.dicomviewer.model.ParsedDicom
import info.cafferata.dicomviewer.model.SeriesGroup
import info.cafferata.dicomviewer.parser.DicomParser
import info.cafferata.dicomviewer.store.DicomFileViewModel
import info.cafferata.dicomviewer.ui.screens.AboutScreen
import info.cafferata.dicomviewer.ui.screens.LearnScreen
import info.cafferata.dicomviewer.ui.screens.ScansScreen
import info.cafferata.dicomviewer.ui.screens.TipJarCoachMark
import info.cafferata.dicomviewer.ui.screens.TipJarScreen
import info.cafferata.dicomviewer.ui.screens.ViewerScreen
import info.cafferata.dicomviewer.ui.theme.DicomViewerTheme
import info.cafferata.dicomviewer.ui.theme.Med
import info.cafferata.dicomviewer.util.ThumbCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val PREFS = "dicomviewer"

class MainActivity : ComponentActivity() {

    private val fileViewModel: DicomFileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Count launches (for the tip jar every 50th launch), once per process start.
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (savedInstanceState == null) {
            prefs.edit().putInt("aantalStarts", prefs.getInt("aantalStarts", 0) + 1).apply()
        }
        setContent {
            DicomViewerTheme {
                DicomViewerApp(fileViewModel)
            }
        }
    }
}

/** Error from the parser → message in the device language. */
@Composable
private fun errorText(e: Throwable?): String = when (e) {
    is DicomError.NotDicom -> stringResource(R.string.err_not_dicom)
    is DicomError.MissingPixelData -> stringResource(R.string.err_no_pixels)
    is DicomError.BadDimensions -> stringResource(R.string.err_dims)
    is SeriesEmpty -> stringResource(R.string.err_no_images_series)
    else -> stringResource(R.string.err_format)
}

private object SeriesEmpty : Exception()

@Composable
private fun DicomViewerApp(viewModel: DicomFileViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    var tab by rememberSaveable { mutableStateOf(0) }
    var parsedDicom by remember { mutableStateOf<ParsedDicom?>(null) }
    var viewerTitle by remember { mutableStateOf("") }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var viewerId by remember { mutableStateOf(0) }
    var isParsing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    val tipStore = remember { TipStore(context) }
    val thumbs = remember { ThumbCache(context) { viewModel.readBytes(it) } }
    var showTipJar by remember { mutableStateOf(false) }
    var showTipJarAuto by remember { mutableStateOf(false) }
    var showCoachMark by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("tipJarCoachMarkShown", false)) {
            delay(800)
            showCoachMark = true
            prefs.edit().putBoolean("tipJarCoachMarkShown", true).apply()
        } else {
            // Every 50th launch: show the tip jar once — only to people who never tipped (#11, as on iOS).
            val n = prefs.getInt("aantalStarts", 0)
            if (n > 0 && n % 50 == 0 && !prefs.getBoolean("fooienpotNietMeerTonen", false)) {
                delay(1500)
                if (tipStore.purchased.value.isEmpty()) showTipJarAuto = true
            }
        }
    }

    fun openFile(file: DicomFileInfo, title: String) {
        viewModel.registreerOpening(file)
        isParsing = true
        scope.launch {
            try {
                val parsed = withContext(Dispatchers.Default) { DicomParser.parse(viewModel.readBytes(file)) }
                parsedDicom = parsed; viewerTitle = title; selectedKey = file.key; viewerId++
            } catch (e: Exception) {
                error = e
            } finally {
                isParsing = false
            }
        }
    }

    /** Opens a whole series: parses each file (in instance order) and concatenates all frames. */
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
                error = SeriesEmpty
            } else {
                parsedDicom = eerste.copy(frames = alleFrames)
                viewerTitle = groep.description; selectedKey = groep.files.first().key; viewerId++
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Med.bg)) {
        // Tablets / wide windows: scan list left, viewer right (as on iPad).
        val split = maxWidth >= 840.dp

        // System back: coach mark → viewer → other tab → exit.
        BackHandler(enabled = showCoachMark || (parsedDicom != null && !split) || tab != 0) {
            when {
                showCoachMark -> showCoachMark = false
                parsedDicom != null && !split -> { parsedDicom = null; selectedKey = null }
                else -> tab = 0
            }
        }

        if (parsedDicom != null && !split) {
            ViewerScreen(
                parsed = parsedDicom!!,
                title = viewerTitle,
                onExport = { b, f -> exportFrame(b, f) },
                onDismiss = { parsedDicom = null; selectedKey = null },
            )
        } else {
            Scaffold(
                containerColor = Med.bg,
                bottomBar = {
                    NavigationBar(containerColor = androidx.compose.ui.graphics.Color(0xFF111827)) {
                        listOf(
                            Triple(R.string.tab_scans, Icons.Filled.GridView, 0),
                            Triple(R.string.tab_learn, Icons.Filled.School, 1),
                            Triple(R.string.tab_about, Icons.Filled.Info, 2),
                        ).forEach { (label, icon, idx) ->
                            NavigationBarItem(
                                selected = tab == idx,
                                onClick = { tab = idx },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(stringResource(label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Med.accentText, selectedTextColor = Med.accentText,
                                    unselectedIconColor = Med.textSec, unselectedTextColor = Med.textSec,
                                    indicatorColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                                ),
                            )
                        }
                    }
                },
            ) { inner ->
                Box(modifier = Modifier.padding(inner).fillMaxSize()) {
                    when (tab) {
                        0 -> {
                            val list: @Composable () -> Unit = {
                                ScansScreen(
                                    viewModel = viewModel,
                                    thumbs = thumbs,
                                    isParsing = isParsing,
                                    selectedKey = if (split) selectedKey else null,
                                    onOpenFile = { f, t -> openFile(f, t) },
                                    onOpenSeries = { openSeries(it) },
                                    onTipJar = { showTipJar = true },
                                    onOpenLearn = { tab = 1 },
                                )
                            }
                            if (split) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.width(400.dp).fillMaxHeight()) { list() }
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        val p = parsedDicom
                                        if (p != null) {
                                            androidx.compose.runtime.key(viewerId) {
                                                ViewerScreen(parsed = p, title = viewerTitle, embedded = true,
                                                    onExport = { b, f -> exportFrame(b, f) }, onDismiss = {})
                                            }
                                        } else {
                                            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Med.accentText, modifier = Modifier.size(44.dp))
                                                Spacer(Modifier.size(14.dp))
                                                Text(stringResource(R.string.choose_scan), color = Med.textPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                                Text(stringResource(R.string.choose_scan_sub), color = Med.textSec, fontSize = 15.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                list()
                            }
                        }
                        1 -> LearnScreen()
                        else -> AboutScreen(tipStore = tipStore, versionName = versionName)
                    }
                }
            }
        }
    }

    error?.let { e ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(stringResource(R.string.file_unsupported)) },
            text = { Text(errorText(e)) },
            confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.ok)) } },
        )
    }

    if (showTipJar) TipJarScreen(store = tipStore, onDismiss = { showTipJar = false })
    if (showTipJarAuto) {
        TipJarScreen(
            store = tipStore,
            automatic = true,
            onDontShowAgain = { prefs.edit().putBoolean("fooienpotNietMeerTonen", true).apply() },
            onDismiss = { showTipJarAuto = false },
        )
    }
    if (showCoachMark) {
        TipJarCoachMark(
            onDonate = { showCoachMark = false; showTipJar = true },
            onDismiss = { showCoachMark = false },
        )
    }
}
