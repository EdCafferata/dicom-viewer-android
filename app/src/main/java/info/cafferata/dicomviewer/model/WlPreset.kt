package info.cafferata.dicomviewer.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

/** Window/Level preset in Hounsfield Units (CT). */
data class WlPreset(val naam: String, val icon: ImageVector, val center: Double, val width: Double) {
    companion object {
        val alle = listOf(
            WlPreset("Abdomen", Icons.Filled.SelfImprovement, 40.0, 400.0),
            WlPreset("Long", Icons.Filled.Air, -600.0, 1500.0),
            WlPreset("Bot", Icons.Filled.Accessibility, 300.0, 1500.0),
            WlPreset("Hersenen", Icons.Filled.Psychology, 40.0, 80.0),
        )
    }
}
