package info.cafferata.dicomviewer.util

fun Long.asFileSize(): String {
    if (this < 1024) return "$this B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = this.toDouble() / 1024
    for (unit in units) {
        if (value < 1024 || unit == units.last()) return "%.1f %s".format(value, unit)
        value /= 1024
    }
    return "%.1f %s".format(value, units.last())
}
