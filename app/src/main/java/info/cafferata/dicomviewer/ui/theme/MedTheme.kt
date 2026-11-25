package info.cafferata.dicomviewer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design tokens — same palette as the iOS 2.2 redesign. */
object Med {
    val bg = Color(0xFF05080F)
    val surface = Color(0xFF0F1A2E)
    val card = Color.White.copy(alpha = 0.06f)
    val cardLine = Color.White.copy(alpha = 0.11f)
    val border = Color.White.copy(alpha = 0.10f)
    val glass = Color(0xFF222937).copy(alpha = 0.88f)
    val glassLine = Color.White.copy(alpha = 0.16f)
    val accent = Color(0xFF00C2CB)      // clinical teal
    val accentText = Color(0xFF3FD9E0)  // teal for text on dark (better contrast)
    val onAccent = Color(0xFF03201F)
    val blue = Color(0xFF2F80ED)
    val heart = Color(0xFFFF7A8A)
    val danger = Color(0xFFE05252)
    val warn = Color(0xFFF59E0B)
    val measure = Color(0xFFFFD166)
    val textPri = Color(0xFFEEF3FA)
    val textSec = Color(0xFF9AA8BF)
    val textDim = Color(0xFF6F7F99)
}

@Composable
fun DicomViewerTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = Med.bg,
        surface = Med.bg,
        surfaceContainer = Color(0xFF111827),
        primary = Med.accent,
        onPrimary = Med.onAccent,
        secondaryContainer = Color.White.copy(alpha = 0.12f),
        onSecondaryContainer = Med.accentText,
        onSurface = Med.textPri,
        onSurfaceVariant = Med.textSec,
        error = Med.danger,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Content card: lightly translucent surface with a thin line. */
fun Modifier.medCard(radius: Dp = 20.dp): Modifier =
    this.clip(RoundedCornerShape(radius))
        .background(Med.card)
        .border(1.dp, Med.cardLine, RoundedCornerShape(radius))

/** Floating control surface ("glass") for the viewer and header buttons. */
fun Modifier.medGlass(radius: Dp = 22.dp): Modifier =
    this.clip(RoundedCornerShape(radius))
        .background(Med.glass)
        .border(1.dp, Med.glassLine, RoundedCornerShape(radius))

fun Modifier.medGlassCircle(): Modifier =
    this.clip(CircleShape)
        .background(Med.glass)
        .border(1.dp, Med.glassLine, CircleShape)

@Composable
fun MedBadge(text: String, color: Color = Med.accentText) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun MedDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Med.border),
    )
}
