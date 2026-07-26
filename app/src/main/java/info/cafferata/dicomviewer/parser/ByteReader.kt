package info.cafferata.dicomviewer.parser

import java.nio.charset.StandardCharsets

/** Little-endian byte cursor over a DICOM file's bytes. */
internal class ByteReader(val data: ByteArray, var offset: Int = 0) {

    val isAtEnd: Boolean get() = offset >= data.size
    val remaining: Int get() = maxOf(0, data.size - offset)

    fun readU16(): Int? {
        if (remaining < 2) return null
        val v = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        offset += 2
        return v
    }

    fun readU32(): Long? {
        if (remaining < 4) return null
        val v = (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
        offset += 4
        return v
    }

    fun readBytes(n: Int): ByteArray? {
        if (n < 0 || remaining < n) return null
        val out = data.copyOfRange(offset, offset + n)
        offset += n
        return out
    }

    fun skip(n: Int) {
        offset += minOf(maxOf(0, n), remaining)
    }

    fun readAscii(n: Int): String {
        val d = readBytes(n) ?: return ""
        return String(d, StandardCharsets.US_ASCII).trim()
    }
}
