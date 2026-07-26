package info.cafferata.dicomviewer.parser

import info.cafferata.dicomviewer.model.DicomError
import info.cafferata.dicomviewer.model.DicomHeader
import info.cafferata.dicomviewer.model.ParsedDicom

object DicomParser {

    fun parse(data: ByteArray): ParsedDicom {
        // Validate DICOM magic at byte 128.
        if (data.size <= 132 ||
            data[128] != 0x44.toByte() || data[129] != 0x49.toByte() ||
            data[130] != 0x43.toByte() || data[131] != 0x4D.toByte()
        ) throw DicomError.NotDicom

        val r = ByteReader(data, offset = 132)

        var transferSyntax = "1.2.840.10008.1.2.1" // Explicit VR LE (default)
        var patientName = ""
        var modality = ""
        var rows = 0
        var columns = 0
        var bitsAllocated = 16
        var pixelRep = 0
        var nFrames = 1
        var samplesPerPixel = 1
        var photometric = "MONOCHROME2"
        var windowCenter = 0.0
        var windowWidth = 0.0
        var rescaleSlope = 1.0
        var rescaleIntercept = 0.0
        var rawPixels: ByteArray? = null
        var encBlob = ByteArray(0)
        var encapsulated = false
        var explicitVr = true
        var metaDone = false

        while (!r.isAtEnd) {
            val group = r.readU16() ?: break
            val element = r.readU16() ?: break

            // Sequence / item delimiters: skip the 4-byte length and continue.
            if (group == 0xFFFE) { r.skip(4); continue }

            // Switch to dataset VR mode once past the File Meta group (0002).
            if (group > 0x0002 && !metaDone) {
                metaDone = true
                explicitVr = transferSyntax != "1.2.840.10008.1.2"
            }

            val vr: String
            val valueLen: Long

            if (explicitVr || group == 0x0002) {
                val vrData = r.readBytes(2) ?: break
                vr = String(vrData, Charsets.US_ASCII)
                if (vr in LONG_VRS) {
                    r.skip(2)
                    valueLen = r.readU32() ?: break
                } else {
                    valueLen = (r.readU16() ?: break).toLong()
                }
            } else {
                vr = implicitVr(group, element)
                valueLen = r.readU32() ?: break
            }

            val undefined = valueLen == 0xFFFFFFFFL
            val isPixelData = group == 0x7FE0 && element == 0x0010

            // Skip sequences.
            if (vr == "SQ" || (undefined && !isPixelData)) {
                if (undefined) skipToDelimiter(r, explicitVr) else r.skip(valueLen.toInt())
                continue
            }

            // Pixel data.
            if (isPixelData) {
                if (undefined) {
                    encapsulated = true
                    encBlob = extractEncapsulated(r)
                } else {
                    rawPixels = r.readBytes(valueLen.toInt())
                }
                continue
            }

            val len = valueLen.toInt()

            when (group to element) {
                0x0002 to 0x0010 -> {
                    val s = r.readAscii(len)
                    if (s.isNotEmpty()) transferSyntax = s
                }
                0x0010 to 0x0010 -> {
                    val d = r.readBytes(len)
                    if (d != null) {
                        patientName = String(d, Charsets.UTF_8).trim()
                            .replace("^", " ").trim()
                    }
                }
                0x0008 to 0x0060 -> modality = r.readAscii(len)
                0x0028 to 0x0010 -> {
                    val d = r.readBytes(len)
                    if (d != null && d.size >= 2) rows = (d[0].toInt() and 0xFF) or ((d[1].toInt() and 0xFF) shl 8)
                }
                0x0028 to 0x0011 -> {
                    val d = r.readBytes(len)
                    if (d != null && d.size >= 2) columns = (d[0].toInt() and 0xFF) or ((d[1].toInt() and 0xFF) shl 8)
                }
                0x0028 to 0x0100 -> {
                    val d = r.readBytes(len)
                    if (d != null && d.size >= 2) bitsAllocated = (d[0].toInt() and 0xFF) or ((d[1].toInt() and 0xFF) shl 8)
                }
                0x0028 to 0x0103 -> {
                    val d = r.readBytes(len)
                    if (d != null && d.size >= 2) pixelRep = (d[0].toInt() and 0xFF) or ((d[1].toInt() and 0xFF) shl 8)
                }
                0x0028 to 0x0008 -> nFrames = maxOf(1, r.readAscii(len).toIntOrNull() ?: 1)
                0x0028 to 0x0002 -> {
                    val d = r.readBytes(len)
                    if (d != null && d.size >= 2) samplesPerPixel = (d[0].toInt() and 0xFF) or ((d[1].toInt() and 0xFF) shl 8)
                }
                0x0028 to 0x0004 -> photometric = r.readAscii(len)
                0x0028 to 0x1050 -> {
                    val s = r.readAscii(len)
                    windowCenter = (s.split("\\").firstOrNull() ?: s).toDoubleOrNull() ?: 0.0
                }
                0x0028 to 0x1051 -> {
                    val s = r.readAscii(len)
                    windowWidth = (s.split("\\").firstOrNull() ?: s).toDoubleOrNull() ?: 0.0
                }
                0x0028 to 0x1052 -> rescaleIntercept = r.readAscii(len).toDoubleOrNull() ?: 0.0
                0x0028 to 0x1053 -> rescaleSlope = r.readAscii(len).toDoubleOrNull() ?: 1.0
                else -> r.skip(len)
            }
        }

        if (rows <= 0 || columns <= 0) throw DicomError.BadDimensions

        val isSigned = pixelRep == 1
        val invert = photometric == "MONOCHROME1"

        val frames: List<android.graphics.Bitmap>
        var rawFrames: List<ByteArray> = emptyList()
        if (encapsulated) {
            frames = EncapsulatedDecoder.decodeJpeg(encBlob)
        } else {
            val pixels = rawPixels ?: throw DicomError.MissingPixelData
            if (windowWidth <= 0) {
                val (c, w) = PixelRenderer.autoWindow(pixels, bitsAllocated, isSigned, samplesPerPixel > 1)
                windowCenter = c; windowWidth = w
            }
            frames = PixelRenderer.decodeRaw(
                pixels, rows, columns, nFrames, bitsAllocated, samplesPerPixel,
                isSigned, invert, windowCenter, windowWidth,
            )
            // Keep the raw grayscale values around for Window/Level presets (no color).
            if (samplesPerPixel == 1) {
                rawFrames = PixelRenderer.sliceFrames(pixels, rows, columns, nFrames, bitsAllocated)
            }
        }
        if (frames.isEmpty()) throw DicomError.MissingPixelData

        return ParsedDicom(
            patientName = patientName,
            modality = modality,
            rows = rows, columns = columns,
            frames = frames,
            windowCenter = windowCenter,
            windowWidth = windowWidth,
            rawFrames = rawFrames,
            bitsAllocated = bitsAllocated,
            isSigned = isSigned,
            invert = invert,
            rescaleSlope = rescaleSlope,
            rescaleIntercept = rescaleIntercept,
        )
    }

    /**
     * Reads only the metadata tags needed to group series (Series navigator). Stops before
     * the pixel data, so this is fast enough to run over every file on each reload.
     */
    fun parseHeader(data: ByteArray): DicomHeader? {
        if (data.size <= 132 ||
            data[128] != 0x44.toByte() || data[129] != 0x49.toByte() ||
            data[130] != 0x43.toByte() || data[131] != 0x4D.toByte()
        ) return null

        val r = ByteReader(data, offset = 132)

        var transferSyntax = "1.2.840.10008.1.2.1"
        var patientName = ""
        var modality = ""
        var seriesUid = ""
        var seriesDescription = ""
        var instanceNumber = 0
        var explicitVr = true
        var metaDone = false

        while (!r.isAtEnd) {
            val group = r.readU16() ?: break
            val element = r.readU16() ?: break
            if (group == 0xFFFE) { r.skip(4); continue }

            if (group > 0x0002 && !metaDone) {
                metaDone = true
                explicitVr = transferSyntax != "1.2.840.10008.1.2"
            }
            // Everything we need lives before group 0028 — stop there.
            if (metaDone && group > 0x0020) break

            val vr: String
            val valueLen: Long
            if (explicitVr || group == 0x0002) {
                val vrData = r.readBytes(2) ?: break
                vr = String(vrData, Charsets.US_ASCII)
                if (vr in LONG_VRS) {
                    r.skip(2)
                    valueLen = r.readU32() ?: break
                } else {
                    valueLen = (r.readU16() ?: break).toLong()
                }
            } else {
                vr = implicitVr(group, element)
                valueLen = r.readU32() ?: break
            }

            val undefined = valueLen == 0xFFFFFFFFL
            if (vr == "SQ" || undefined) {
                if (undefined) skipToDelimiter(r, explicitVr) else r.skip(valueLen.toInt())
                continue
            }

            val len = valueLen.toInt()
            when (group to element) {
                0x0002 to 0x0010 -> {
                    val s = r.readAscii(len)
                    if (s.isNotEmpty()) transferSyntax = s
                }
                0x0010 to 0x0010 -> {
                    val d = r.readBytes(len)
                    if (d != null) {
                        patientName = String(d, Charsets.UTF_8).trim()
                            .replace("^", " ").trim()
                    }
                }
                0x0008 to 0x0060 -> modality = r.readAscii(len)
                0x0008 to 0x103E -> seriesDescription = r.readAscii(len)
                0x0020 to 0x000E -> seriesUid = r.readAscii(len)
                0x0020 to 0x0013 -> instanceNumber = r.readAscii(len).toIntOrNull() ?: 0
                else -> r.skip(len)
            }
        }

        return DicomHeader(
            patientName = patientName,
            modality = modality,
            seriesUid = seriesUid,
            seriesDescription = seriesDescription,
            instanceNumber = instanceNumber,
        )
    }

    // Skips tags until a sequence delimiter (E0DD) or item delimiter (E00D) is found.
    // Must know whether the dataset uses explicit or implicit VR.
    private fun skipToDelimiter(r: ByteReader, explicit: Boolean) {
        while (!r.isAtEnd) {
            val g = r.readU16() ?: break
            val e = r.readU16() ?: break

            if (g == 0xFFFE) {
                val l = r.readU32() ?: break
                if (e == 0xE0DD || e == 0xE00D) return // sequence or item end
                if (l == 0xFFFFFFFFL) skipToDelimiter(r, explicit) else r.skip(l.toInt())
                continue
            }

            val length: Long
            if (explicit) {
                val vrBytes = r.readBytes(2) ?: break
                val vr = String(vrBytes, Charsets.US_ASCII)
                if (vr in LONG_VRS) {
                    r.skip(2)
                    length = r.readU32() ?: break
                } else {
                    length = (r.readU16() ?: break).toLong()
                }
            } else {
                length = r.readU32() ?: break
            }

            if (length == 0xFFFFFFFFL) skipToDelimiter(r, explicit) else r.skip(length.toInt())
        }
    }

    // Collects all encapsulated items into one concatenated blob (skipping the BOT).
    // Items are often split across multiple FFFE,E000 chunks for one frame, so everything
    // is concatenated and the marker-based splitter finds the frame boundaries.
    private fun extractEncapsulated(r: ByteReader): ByteArray {
        val blob = java.io.ByteArrayOutputStream()
        var isFirst = true
        while (!r.isAtEnd) {
            val g = r.readU16() ?: break
            val e = r.readU16() ?: break
            if (g == 0xFFFE && e == 0xE0DD) { r.skip(4); break } // sequence delimiter
            if (g != 0xFFFE || e != 0xE000) break // must be an item
            val l = r.readU32() ?: break
            if (l == 0xFFFFFFFFL) continue
            if (isFirst) { isFirst = false; r.skip(l.toInt()); continue } // skip the BOT
            r.readBytes(l.toInt())?.let { blob.write(it) }
        }
        return blob.toByteArray()
    }
}
