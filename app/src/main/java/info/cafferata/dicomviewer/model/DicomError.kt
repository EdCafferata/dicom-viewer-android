package info.cafferata.dicomviewer.model

sealed class DicomError(message: String) : Exception(message) {
    object NotDicom : DicomError("Not a valid DICOM file")
    object MissingPixelData : DicomError("No pixel data found")
    object BadDimensions : DicomError("Invalid image dimensions")
    class Parse(message: String) : DicomError(message)
}
