package info.cafferata.dicomviewer.model

/** Lightweight header info for the series navigator — no pixel data, so a whole folder can be grouped quickly. */
data class DicomHeader(
    val patientName: String,
    val modality: String,
    val seriesUid: String,        // (0020,000E)
    val seriesDescription: String, // (0008,103E)
    val instanceNumber: Int,       // (0020,0013) — order within the series
)
