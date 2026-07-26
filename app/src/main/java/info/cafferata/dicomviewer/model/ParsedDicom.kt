package info.cafferata.dicomviewer.model

import android.graphics.Bitmap
import info.cafferata.dicomviewer.parser.PixelRenderer

data class ParsedDicom(
    val patientName: String,
    val modality: String,
    val rows: Int,
    val columns: Int,
    val frames: List<Bitmap>,
    val windowCenter: Double,
    val windowWidth: Double,
    // Raw pixel data per frame (grayscale only, uncompressed) so the viewer can
    // re-window with Window/Level presets. Empty for JPEG-compressed or color images —
    // presets aren't available then.
    val rawFrames: List<ByteArray> = emptyList(),
    val bitsAllocated: Int = 16,
    val isSigned: Boolean = false,
    val invert: Boolean = false,
    val rescaleSlope: Double = 1.0,   // (0028,1053) — for HU conversion
    val rescaleIntercept: Double = 0.0, // (0028,1052)
) {
    val kanHervensteren: Boolean get() = rawFrames.isNotEmpty()

    /** Re-renders the raw frames with a different window (center/width in raw pixel values — use huNaarRaw for HU presets). */
    fun render(center: Double, width: Double): List<Bitmap> =
        rawFrames.mapNotNull { raw ->
            PixelRenderer.renderFrame(raw, rows, columns, bitsAllocated, isSigned, invert, center, width)
        }

    /** Converts a window in Hounsfield Units to raw pixel values via Rescale Slope/Intercept (HU = raw × slope + intercept). */
    fun huNaarRaw(center: Double, width: Double): Pair<Double, Double> {
        val slope = if (rescaleSlope != 0.0) rescaleSlope else 1.0
        return Pair((center - rescaleIntercept) / slope, width / slope)
    }
}
