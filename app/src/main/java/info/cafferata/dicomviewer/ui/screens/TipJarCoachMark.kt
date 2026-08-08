package info.cafferata.dicomviewer.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CoachTip(val emoji: String, val naam: String, val omschrijving: String, val prijs: String, val kleur: Color)

private val coachTips = listOf(
    CoachTip("☕", "Koffie", "Kleine bijdrage, grote glimlach", "€ 0,99", Color(0xFFB57A3D)),
    CoachTip("🍕", "Lunch", "Houd de ontwikkelaar goed gevoed", "€ 2,99", Color(0xFFDC4F33)),
    CoachTip("🍽️", "Diner", "Echt waardevol — hartstikke bedankt!", "€ 9,99", Color(0xFF3DA178)),
)

/** Eenmalige tutorial-overlay die naar het hartje in de header wijst — Android-equivalent van TipJarCoachMark.swift. */
@Composable
fun TipJarCoachMark(onDonate: () -> Unit, onDismiss: () -> Unit) {
    val rood = Color(0xFFCC1A1A)
    val transition = rememberInfiniteTransition(label = "coachmark")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = rood,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 66.dp)
                        .size(28.dp)
                        .alpha(0.4f + pulse * 0.6f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                coachTips.forEach { tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF17233A).copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                            .clickable { onDonate() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).background(tip.kleur, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text(tip.emoji, fontSize = 22.sp) }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tip.naam, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(tip.omschrijving, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                        Text(
                            tip.prijs,
                            color = Color(0xFF14243F), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color(0xFF00C2CC), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    "of tik ergens om te sluiten",
                    color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )
            }
        }
    }
}
