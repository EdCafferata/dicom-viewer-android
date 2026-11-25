package info.cafferata.dicomviewer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateMapOf
import info.cafferata.dicomviewer.model.DicomFileInfo
import info.cafferata.dicomviewer.parser.DicomParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Thumbnail + frame count per file, made in the background. Parsing a multi-frame study is slow,
 * so results are also kept on disk (cacheDir/thumbs) and only made once per file.
 */
class ThumbCache(private val context: Context, private val readBytes: (DicomFileInfo) -> ByteArray) {

    data class Info(val image: Bitmap?, val frames: Int, val modality: String, val size: String)

    val info = mutableStateMapOf<String, Info>()
    private val busy = mutableSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dir: File get() = File(context.cacheDir, "thumbs").apply { mkdirs() }

    fun load(file: DicomFileInfo) {
        val key = file.key
        if (info.containsKey(key) || !busy.add(key)) return
        scope.launch {
            val result = runCatching { make(file) }.getOrElse { Info(null, 0, "", "") }
            withContext(Dispatchers.Main) {
                info[key] = result
                busy.remove(key)
            }
        }
    }

    private fun cacheKey(file: DicomFileInfo): String =
        "${file.key}-${file.fileSize}-${file.modifiedDate}".map { if (it.isLetterOrDigit() || it == '-') it else '_' }.joinToString("")

    private fun make(file: DicomFileInfo): Info {
        val key = cacheKey(file)
        val meta = File(dir, "$key.json")
        val png = File(dir, "$key.png")
        if (meta.exists()) {
            val j = JSONObject(meta.readText())
            return Info(
                if (png.exists()) BitmapFactory.decodeFile(png.path) else null,
                j.optInt("frames"), j.optString("modality"), j.optString("size"),
            )
        }
        val result = if (file.fileSize in 1 until 40_000_000 || file.isDemo) {
            val parsed = DicomParser.parse(readBytes(file))
            val first = parsed.frames.firstOrNull()
            Info(first?.let { scale(it) }, parsed.frames.size, parsed.modality, "${parsed.columns} × ${parsed.rows}")
        } else {
            Info(null, 0, "", "")
        }
        result.image?.let { bmp -> FileOutputStream(png).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) } }
        meta.writeText(JSONObject().put("frames", result.frames).put("modality", result.modality).put("size", result.size).toString())
        return result
    }

    private fun scale(src: Bitmap): Bitmap {
        val max = 160
        val f = minOf(1f, max.toFloat() / maxOf(src.width, src.height))
        return Bitmap.createScaledBitmap(src, (src.width * f).toInt().coerceAtLeast(1), (src.height * f).toInt().coerceAtLeast(1), true)
    }
}
