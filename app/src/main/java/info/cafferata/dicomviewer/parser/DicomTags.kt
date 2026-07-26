package info.cafferata.dicomviewer.parser

/** Long-form Value Representations that carry a 2-byte reserved field + 4-byte length (explicit VR). */
internal val LONG_VRS = setOf("OB", "OD", "OF", "OL", "OV", "OW", "SQ", "UC", "UN", "UR", "UT", "SV", "UV")

/** Infers the VR for well-known tags when the dataset uses implicit VR little endian. */
internal fun implicitVr(group: Int, element: Int): String = when (group to element) {
    0x0028 to 0x0002, 0x0028 to 0x0010, 0x0028 to 0x0011,
    0x0028 to 0x0100, 0x0028 to 0x0101, 0x0028 to 0x0103 -> "US"
    0x0028 to 0x0008 -> "IS"
    0x0028 to 0x1050, 0x0028 to 0x1051 -> "DS"
    0x7FE0 to 0x0010 -> "OW"
    else -> "UN"
}
