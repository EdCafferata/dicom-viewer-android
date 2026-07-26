package info.cafferata.dicomviewer.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Decodes encapsulated (compressed) DICOM pixel data — JPEG, JPEG Lossless, JPEG 2000. */
internal object EncapsulatedDecoder {

    fun decodeJpeg(blob: ByteArray): List<Bitmap> {
        // Split on SOI/EOI markers first — handles multi-frame and single-frame alike.
        // A direct decode would only return the first frame for concatenated
        // multi-frame blobs (JPEG Lossless and JPEG 2000 multi-frame).
        val split = splitAndDecode(blob)
        if (split.isNotEmpty()) return split
        // Fallback for blobs without standard SOI/EOI delimiters.
        val img = decodeOne(blob)
        return if (img != null) listOf(img) else emptyList()
    }

    // Searches for JPEG (FF D8) and JPEG 2000 (FF 4F) start markers + FF D9 end marker.
    private fun splitAndDecode(data: ByteArray): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()
        var pos = 0
        while (pos < data.size) {
            val soi = findEarliestStart(data, pos) ?: break
            val eoi = indexOf(data, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), soi + 2)
            if (eoi != -1) {
                decodeOne(data.copyOfRange(soi, eoi + 2))?.let { frames.add(it) }
                pos = eoi + 2
            } else {
                decodeOne(data.copyOfRange(soi, data.size))?.let { frames.add(it) }
                break
            }
        }
        return frames
    }

    private fun findEarliestStart(data: ByteArray, from: Int): Int? {
        val soiIdx = indexOf(data, byteArrayOf(0xFF.toByte(), 0xD8.toByte()), from)
        val jp2Idx = indexOf(data, byteArrayOf(0xFF.toByte(), 0x4F.toByte()), from)
        return when {
            soiIdx == -1 && jp2Idx == -1 -> null
            soiIdx == -1 -> jp2Idx
            jp2Idx == -1 -> soiIdx
            else -> minOf(soiIdx, jp2Idx)
        }
    }

    private fun indexOf(data: ByteArray, marker: ByteArray, from: Int): Int {
        if (from < 0) return -1
        outer@ for (i in from..data.size - marker.size) {
            for (j in marker.indices) {
                if (data[i + j] != marker[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun decodeOne(data: ByteArray): Bitmap? {
        // Detect JPEG Lossless (SOF3 = FF C3) in the first 300 bytes → skip BitmapFactory,
        // which "succeeds" on the JFIF header but returns garbage/null for SOF3.
        val probe = minOf(data.size - 1, 299)
        var hasSof3 = false
        for (i in 0 until probe) {
            if ((data[i].toInt() and 0xFF) == 0xFF && (data[i + 1].toInt() and 0xFF) == 0xC3) {
                hasSof3 = true
                break
            }
        }
        if (!hasSof3) {
            BitmapFactory.decodeByteArray(data, 0, data.size)?.let { return it }
        }
        return JpegLosslessDecoder.decode(data)
    }
}
