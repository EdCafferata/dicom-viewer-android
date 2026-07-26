package info.cafferata.dicomviewer.model

import java.io.File
import java.util.UUID

sealed class DicomSource {
    data class LocalFile(val file: File) : DicomSource()
    data class BundledDemo(val resId: Int, val displayName: String) : DicomSource()
}

data class DicomFileInfo(
    val source: DicomSource,
    val modifiedDate: Long,
    val fileSize: Long,
) {
    val id: String = UUID.randomUUID().toString()

    val name: String get() = when (source) {
        is DicomSource.LocalFile -> source.file.nameWithoutExtension
        is DicomSource.BundledDemo -> source.displayName
    }

    val ext: String get() = when (source) {
        is DicomSource.LocalFile -> source.file.extension.uppercase()
        is DicomSource.BundledDemo -> "DCM"
    }

    val isDemo: Boolean get() = source is DicomSource.BundledDemo

    /** A stable key to identify this file across reloads (path, or resource id for demos). */
    val key: String get() = when (source) {
        is DicomSource.LocalFile -> source.file.path
        is DicomSource.BundledDemo -> "demo:${source.resId}"
    }

    companion object {
        fun forFile(file: File) = DicomFileInfo(
            source = DicomSource.LocalFile(file),
            modifiedDate = file.lastModified(),
            fileSize = file.length(),
        )
    }
}
