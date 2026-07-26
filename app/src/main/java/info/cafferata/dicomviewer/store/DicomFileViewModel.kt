package info.cafferata.dicomviewer.store

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import info.cafferata.dicomviewer.model.DicomFileInfo
import info.cafferata.dicomviewer.model.SeriesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "dicom_viewer_prefs"
private const val RECENT_KEY = "recentGeopend"
private const val MAX_RECENT = 5

class DicomFileViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FileRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    var files by mutableStateOf<List<DicomFileInfo>>(emptyList())
        private set
    var series by mutableStateOf<List<SeriesGroup>>(emptyList())
        private set
    var recent by mutableStateOf<List<DicomFileInfo>>(emptyList())
        private set

    val bundledDemos: List<DicomFileInfo> get() = repo.bundledDemos

    init {
        reload()
    }

    fun reload() {
        files = repo.listFiles()
        herlaadRecent()
        viewModelScope.launch {
            val groepen = withContext(Dispatchers.Default) { repo.groepeerSeries(files) }
            series = groepen
        }
    }

    fun readBytes(info: DicomFileInfo): ByteArray = repo.readBytes(info)

    /** Records that a file was opened; keeps only its key (a path moves between app updates for demos). */
    fun registreerOpening(info: DicomFileInfo) {
        val namen = (prefs.getStringSet(RECENT_KEY, emptySet()) ?: emptySet())
            .toMutableList()
            .apply { remove(info.key) }
        namen.add(0, info.key)
        prefs.edit().putStringSet(RECENT_KEY, namen.take(MAX_RECENT).toSet()).apply()
        // SharedPreferences string sets don't preserve order, so also store an ordered CSV.
        prefs.edit().putString("${RECENT_KEY}_ordered", namen.take(MAX_RECENT).joinToString(",")).apply()
        herlaadRecent()
    }

    private fun herlaadRecent() {
        val ordered = prefs.getString("${RECENT_KEY}_ordered", "") ?: ""
        val keys = ordered.split(",").filter { it.isNotBlank() }
        val byKey = files.associateBy { it.key }
        recent = keys.mapNotNull { byKey[it] }
    }

    fun importFile(uri: Uri, suggestedName: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.importFile(uri, suggestedName) != null }
            if (ok) reload()
        }
    }

    fun delete(info: DicomFileInfo) {
        repo.delete(info)
        reload()
    }
}
