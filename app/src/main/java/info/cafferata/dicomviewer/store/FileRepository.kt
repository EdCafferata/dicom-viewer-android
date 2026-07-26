package info.cafferata.dicomviewer.store

import android.content.Context
import android.net.Uri
import info.cafferata.dicomviewer.R
import info.cafferata.dicomviewer.model.DicomFileInfo
import info.cafferata.dicomviewer.model.DicomSource
import info.cafferata.dicomviewer.model.SeriesGroup
import info.cafferata.dicomviewer.parser.DicomParser
import java.io.File

/** File-system access for imported DICOM files, kept separate from Compose state (see [DicomFileViewModel]). */
class FileRepository(private val context: Context) {

    private val docsDir: File get() = context.filesDir

    fun listFiles(): List<DicomFileInfo> =
        docsDir.listFiles { f -> f.isFile && !f.name.startsWith(".") && f.name !in RESERVED_NAMES }
            ?.map { DicomFileInfo.forFile(it) }
            ?.sortedByDescending { it.modifiedDate }
            ?: emptyList()

    private companion object {
        // "profileInstalled" is an ART runtime marker the OS writes directly into
        // filesDir (baseline profile install state) — not an imported scan.
        val RESERVED_NAMES = setOf("profileInstalled")
    }

    val bundledDemos: List<DicomFileInfo> = listOf(
        Triple(R.raw.cag_voor_ingreep, "cag_voor_ingreep", 0),
        Triple(R.raw.cag_tijdens_ingreep, "cag_tijdens_ingreep", 0),
        Triple(R.raw.cag_na_ingreep, "cag_na_ingreep", 0),
    ).map { (resId, name, _) ->
        DicomFileInfo(
            source = DicomSource.BundledDemo(resId, name),
            modifiedDate = 0L,
            fileSize = 0L,
        )
    }

    fun readBytes(info: DicomFileInfo): ByteArray = when (val s = info.source) {
        is DicomSource.LocalFile -> s.file.readBytes()
        is DicomSource.BundledDemo -> context.resources.openRawResource(s.resId).use { it.readBytes() }
    }

    /** Copies a content:// URI into the app's private files dir. Returns the new file, or null on failure. */
    fun importFile(uri: Uri, suggestedName: String): File? {
        val safeName = suggestedName.substringAfterLast('/').ifBlank { "scan.dcm" }
        var dest = File(docsDir, safeName)
        // Guard against path traversal (e.g. a filename of "..").
        if (!dest.canonicalPath.startsWith(docsDir.canonicalPath)) return null
        var counter = 1
        val base = dest.nameWithoutExtension
        val ext = dest.extension
        while (dest.exists()) {
            dest = File(docsDir, if (ext.isNotEmpty()) "${base}_$counter.$ext" else "${base}_$counter")
            counter++
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest
        } catch (e: Exception) {
            null
        }
    }

    fun delete(info: DicomFileInfo) {
        (info.source as? DicomSource.LocalFile)?.file?.delete()
    }

    /** Groups files by Series Instance UID (headers only, no pixel data — safe to run on every reload). */
    fun groepeerSeries(bestanden: List<DicomFileInfo>): List<SeriesGroup> {
        val perUid = LinkedHashMap<String, MutableList<Pair<DicomFileInfo, Int>>>()
        val omschrijving = HashMap<String, String>()
        val modaliteit = HashMap<String, String>()
        val los = mutableListOf<SeriesGroup>()

        for (file in bestanden) {
            val header = runCatching { DicomParser.parseHeader(readBytes(file)) }.getOrNull()
            if (header != null && header.seriesUid.isNotEmpty()) {
                perUid.getOrPut(header.seriesUid) { mutableListOf() }.add(file to header.instanceNumber)
                if (!omschrijving.containsKey(header.seriesUid) && header.seriesDescription.isNotEmpty()) {
                    omschrijving[header.seriesUid] = header.seriesDescription
                }
                if (!modaliteit.containsKey(header.seriesUid) && header.modality.isNotEmpty()) {
                    modaliteit[header.seriesUid] = header.modality
                }
            } else {
                los.add(SeriesGroup(id = file.key, description = file.name, modality = "", files = listOf(file)))
            }
        }

        val groepen = perUid.entries.map { (uid, items) ->
            val sorted = items.sortedBy { it.second }
            SeriesGroup(
                id = uid,
                description = omschrijving[uid] ?: sorted.first().first.name,
                modality = modaliteit[uid] ?: "",
                files = sorted.map { it.first },
            )
        }

        return groepen + los
    }
}
