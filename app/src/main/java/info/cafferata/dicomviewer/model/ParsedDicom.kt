package info.cafferata.dicomviewer.model

import android.graphics.Bitmap
import info.cafferata.dicomviewer.parser.PixelRenderer

/**
 * Pixel spacing in mm. [opDetector] = taken from Imager Pixel Spacing: the size then holds at the
 * detector plane, not in the patient (with X-ray/angio the real size is slightly smaller).
 */
data class PixelSpacing(val rij: Double, val kolom: Double, val opDetector: Boolean) {
    companion object {
        fun lees(tekst: String, opDetector: Boolean): PixelSpacing? {
            val delen = tekst.split("\\").mapNotNull { it.trim().toDoubleOrNull() }
            val r = delen.firstOrNull()?.takeIf { it > 0 } ?: return null
            val k = delen.getOrNull(1)?.takeIf { it > 0 } ?: r
            return PixelSpacing(r, k, opDetector)
        }
    }
}

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
    /** mm between pixels, for measuring. From Pixel Spacing (0028,0030), else Imager Pixel Spacing (0018,1164). */
    val pixelSpacing: PixelSpacing? = null,
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
