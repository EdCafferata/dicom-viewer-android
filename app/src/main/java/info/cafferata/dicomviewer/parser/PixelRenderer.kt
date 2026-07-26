package info.cafferata.dicomviewer.parser

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/** Renders raw (uncompressed) DICOM pixel data into Bitmaps, applying window/level. */
internal object PixelRenderer {

    fun autoWindow(data: ByteArray, bits: Int, isSigned: Boolean, isColor: Boolean): Pair<Double, Double> {
        if (isColor || bits != 16 || data.size < 2) return Pair(128.0, 256.0)
        var mn = Int.MAX_VALUE
        var mx = Int.MIN_VALUE
        var i = 0
        while (i <= data.size - 2) {
            val raw = (data[i].toInt() and 0xFF) or ((data[i + 1].toInt() and 0xFF) shl 8)
            val v = if (isSigned) raw.toShort().toInt() else raw
            if (v < mn) mn = v
            if (v > mx) mx = v
            i += 2
        }
        return Pair((mn + mx) / 2.0, max(1, mx - mn).toDouble())
    }

    /** Slices the raw pixel block into separate frames (for re-windowing). */
    fun sliceFrames(data: ByteArray, rows: Int, columns: Int, nFrames: Int, bits: Int): List<ByteArray> {
        val bytesPerSample = max(1, bits / 8)
        val frameSize = rows * columns * bytesPerSample
        if (frameSize <= 0 || data.size < frameSize) return emptyList()
        return (0 until nFrames).mapNotNull { f ->
            val start = f * frameSize
            if (start + frameSize > data.size) null else data.copyOfRange(start, start + frameSize)
        }
    }

    fun decodeRaw(
        data: ByteArray, rows: Int, columns: Int, nFrames: Int,
        bits: Int, samples: Int, isSigned: Boolean, invert: Boolean,
        center: Double, width: Double,
    ): List<Bitmap> {
        val bytesPerSample = max(1, bits / 8)
        if (rows <= 0 || columns <= 0 || rows > 16384 || columns > 16384) return emptyList()
        val frameSize = rows.toLong() * columns * samples * bytesPerSample
        if (frameSize <= 0 || data.size < frameSize) return emptyList()

        return (0 until nFrames).mapNotNull { f ->
            val start = f * frameSize
            if (start + frameSize > data.size) return@mapNotNull null
            makeImage(
                data.copyOfRange(start.toInt(), (start + frameSize).toInt()),
                rows, columns, bits, samples, isSigned, invert, center, width,
            )
        }
    }

    /** Public render of one raw frame with the given window — used for Window/Level presets. */
    fun renderFrame(
        data: ByteArray, rows: Int, columns: Int,
        bits: Int, isSigned: Boolean, invert: Boolean,
        center: Double, width: Double,
    ): Bitmap? = makeImage(data, rows, columns, bits, 1, isSigned, invert, center, width)

    private fun makeImage(
        data: ByteArray, rows: Int, columns: Int,
        bits: Int, samples: Int,
        isSigned: Boolean, invert: Boolean,
        center: Double, width: Double,
    ): Bitmap? {
        val isRgb = samples == 3
        val pixelCount = rows * columns
        if (pixelCount <= 0) return null
        val out = IntArray(pixelCount)
        val lo = center - width / 2
        val scale = 255.0 / max(1.0, width)

        if (isRgb) {
            for (i in 0 until pixelCount) {
                val idx = i * 3
                if (idx + 2 >= data.size) break
                val r = data[idx].toInt() and 0xFF
                val g = data[idx + 1].toInt() and 0xFF
                val b = data[idx + 2].toInt() and 0xFF
                out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        } else if (bits == 8) {
            for (i in 0 until min(pixelCount, data.size)) {
                var v = (((data[i].toInt() and 0xFF).toDouble()) - lo) * scale
                v = min(255.0, max(0.0, v))
                val gray = if (invert) (255.0 - v).toInt() else v.toInt()
                out[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        } else {
            for (i in 0 until pixelCount) {
                val idx = i * 2
                if (idx + 1 >= data.size) break
                val raw = (data[idx].toInt() and 0xFF) or ((data[idx + 1].toInt() and 0xFF) shl 8)
                val value = if (isSigned) raw.toShort().toDouble() else raw.toDouble()
                var v = (value - lo) * scale
                v = min(255.0, max(0.0, v))
                val gray = if (invert) (255.0 - v).toInt() else v.toInt()
                out[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        }

        return Bitmap.createBitmap(out, columns, rows, Bitmap.Config.ARGB_8888)
    }
}
