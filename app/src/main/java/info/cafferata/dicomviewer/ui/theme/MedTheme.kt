package info.cafferata.dicomviewer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Med {
    val bg = Color(0xFF080E1A)
    val surface = Color(0xFF0F1A2E)
    val card = Color(0xFF162035)
    val border = Color(0xFF1E3050)
    val accent = Color(0xFF00C2CB) // clinical teal
    val blue = Color(0xFF2F80ED)
    val danger = Color(0xFFE05252)
    val warn = Color(0xFFF59E0B)
    val textPri = Color(0xFFE8EEF7)
    val textSec = Color(0xFF5A7099)
    val textDim = Color(0xFF2E4066)
}

@Composable
fun DicomViewerTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = Med.bg,
        surface = Med.surface,
        primary = Med.accent,
        error = Med.danger,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun MedBadge(text: String, color: Color = Med.accent) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun MedDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Med.border),
    )
}
