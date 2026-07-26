package info.cafferata.dicomviewer.parser

import android.graphics.Bitmap

/** Decodes JPEG Lossless (SOF3, ISO 10918-1) — the format Android's BitmapFactory can't handle either. */
internal object JpegLosslessDecoder {

    // Bit reader with JPEG stuffed-byte removal (ISO 10918-1 §F.1.2.3).
    private class Bits(val src: ByteArray, start: Int) {
        var pos = start
        var buf = 0
        var avail = 0

        fun read(n: Int): Int {
            if (n == 0) return 0
            while (avail < n) {
                if (pos >= src.size) {
                    buf = buf shl 8
                    avail += 8
                    continue
                }
                val b = src[pos].toInt() and 0xFF
                pos++
                if (b == 0xFF && pos < src.size) {
                    val nx = src[pos].toInt() and 0xFF
                    if (nx == 0x00) {
                        pos++
                    } else if (nx in 0xD0..0xD7) {
                        pos++
                        continue
                    }
                }
                buf = (buf shl 8) or b
                avail += 8
            }
            avail -= n
            return (buf shr avail) and ((1 shl n) - 1)
        }
    }

    // Huffman table with O(1) lookup via valOffset trick.
    private class HTable {
        val minCode = IntArray(17)
        val maxCode = IntArray(17) { -1 }
        val valOff = IntArray(17)
        var vals = IntArray(0)

        fun build(counts: IntArray, rawVals: ByteArray, base: Int) {
            val total = counts.sum()
            if (base + total > rawVals.size) return
            vals = IntArray(total) { rawVals[base + it].toInt() and 0xFF }
            var code = 0
            var idx = 0
            for (b in 1..16) {
                valOff[b] = idx - code
                if (counts[b - 1] > 0) {
                    minCode[b] = code
                    maxCode[b] = code + counts[b - 1] - 1
                    code += counts[b - 1]
                    idx += counts[b - 1]
                }
                code = code shl 1
            }
        }
    }

    fun decode(jpeg: ByteArray): Bitmap? {
        if (jpeg.size <= 4 || jpeg[0] != 0xFF.toByte() || jpeg[1] != 0xD8.toByte()) return null
        var i = 2
        var p = 8
        var h = 0
        var w = 0
        var pt = 0
        var sel = 1
        val htabs = HashMap<Int, HTable>()
        val compTd = HashMap<Int, Int>()
        var scanAt = -1

        fun u8(idx: Int) = jpeg[idx].toInt() and 0xFF

        while (i + 1 < jpeg.size) {
            if (jpeg[i] != 0xFF.toByte()) { i++; continue }
            val m = u8(i + 1)
            i += 2
            if (m == 0xD8) continue
            if (m == 0xD9) break
            if (m in 0xD0..0xD7) continue
            if (i + 2 > jpeg.size) break
            val segLen = (u8(i) shl 8) or u8(i + 1)
            val segEnd = i + segLen
            i += 2

            when (m) {
                0xC3 -> { // SOF3 — lossless frame header
                    if (i + 6 <= jpeg.size) {
                        p = u8(i)
                        h = (u8(i + 1) shl 8) or u8(i + 2)
                        w = (u8(i + 3) shl 8) or u8(i + 4)
                    }
                }
                0xC4 -> { // DHT — define Huffman table
                    var q = i
                    while (q < segEnd && q < jpeg.size) {
                        val th = u8(q) and 0x0F
                        q++
                        if (q + 16 > jpeg.size) break
                        val counts = IntArray(16) { u8(q + it) }
                        q += 16
                        val total = counts.sum()
                        if (q + total > jpeg.size) break
                        val ht = HTable()
                        ht.build(counts, jpeg, q)
                        q += total
                        htabs[th] = ht
                    }
                }
                0xDA -> { // SOS — start of scan
                    if (i < jpeg.size) {
                        val nc = u8(i)
                        var q = i + 1
                        repeat(nc) {
                            if (q + 1 < jpeg.size) {
                                compTd[u8(q)] = u8(q + 1) shr 4
                                q += 2
                            }
                        }
                        if (q + 2 < jpeg.size) {
                            sel = u8(q)
                            pt = u8(q + 2) and 0x0F
                        }
                        scanAt = q + 3
                    }
                }
            }
            i = segEnd
        }

        if (scanAt <= 0 || w <= 0 || h <= 0 || w > 16384 || h > 16384 || p !in 1..16 || p <= pt) return null
        val td = compTd.values.firstOrNull() ?: 0
        val ht = htabs[compTd[1] ?: td] ?: htabs[td] ?: return null

        val bits = Bits(jpeg, scanAt)
        val maxVal = (1 shl p) - 1
        val initPx = 1 shl (p - pt - 1)
        val out = IntArray(w * h)
        var ra = initPx

        for (row in 0 until h) {
            // JPEG Lossless line boundary: Ra resets to the first pixel of the previous row.
            if (row > 0) ra = out[(row - 1) * w]
            for (col in 0 until w) {
                var code = 0
                var ssss = 0
                for (b in 1..16) {
                    code = (code shl 1) or bits.read(1)
                    if (code <= ht.maxCode[b]) {
                        ssss = ht.vals[ht.valOff[b] + code]
                        break
                    }
                }
                val diff = if (ssss == 0) {
                    0
                } else {
                    val s = minOf(ssss, 16) // clamp: prevents bit-shift overflow on malformed input
                    val v = bits.read(s)
                    if (v >= (1 shl (s - 1))) v else v - (1 shl s) + 1
                }
                val idx = row * w + col
                val rb = if (row > 0) out[idx - w] else initPx
                val rc = if (row > 0 && col > 0) out[idx - w - 1] else initPx
                val pred = if (row == 0 && col == 0) {
                    initPx
                } else when (sel) {
                    1 -> ra
                    2 -> rb
                    3 -> rc
                    4 -> ra + rb - rc
                    5 -> ra + (rb - rc) / 2
                    6 -> rb + (ra - rc) / 2
                    7 -> (ra + rb) / 2
                    else -> ra
                }
                val px = (pred + diff) and maxVal
                out[idx] = px.coerceIn(0, 255)
                ra = px
            }
        }

        val argb = IntArray(w * h) { idx ->
            val gray = out[idx]
            (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }
        return Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888)
    }
}
