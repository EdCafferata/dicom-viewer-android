package info.cafferata.dicomviewer.model

/** One DICOM series: files sharing the same Series Instance UID, sorted by Instance Number. Standalone files each get their own group. */
data class SeriesGroup(
    val id: String,               // seriesUid, or file key for standalone files
    val description: String,      // seriesDescription or filename
    val modality: String,
    val files: List<DicomFileInfo>, // sorted by instanceNumber
) {
    val isSeries: Boolean get() = files.size > 1
    val totalSize: Long get() = files.sumOf { it.fileSize }
}
