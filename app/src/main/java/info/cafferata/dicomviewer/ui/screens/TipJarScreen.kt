package info.cafferata.dicomviewer.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.ProductDetails
import info.cafferata.dicomviewer.billing.TipProductID
import info.cafferata.dicomviewer.billing.TipStore
import info.cafferata.dicomviewer.ui.theme.Med
import info.cafferata.dicomviewer.ui.theme.MedDivider

private data class TipMeta(val id: String, val emoji: String, val naam: String, val prijs: String, val omschrijving: String, val kleur: Color)

private val tipMeta = listOf(
    TipMeta(TipProductID.SMALL, "☕", "Koffie", "€ 0,99", "Een klein bedankje voor de ontwikkelaar", Color(0xFFB57A3D)),
    TipMeta(TipProductID.MEDIUM, "🍕", "Lunch", "€ 2,99", "Houd de ontwikkelaar goed gevoed", Color(0xFFDC4F33)),
    TipMeta(TipProductID.LARGE, "🍽️", "Diner", "€ 9,99", "Een uitgebreid diner voor de ontwikkelaar", Color(0xFF3DA178)),
)

@Composable
fun TipJarScreen(store: TipStore, onDismiss: () -> Unit) {
    val med = Med
    val context = LocalContext.current
    val activity = context as? Activity

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(med.bg)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                TipJarHeader(onClose = onDismiss)
                MedDivider()
                TipJarContent(store = store, activity = activity)
            }
        }
    }
}

@Composable
private fun TipJarHeader(onClose: () -> Unit) {
    val med = Med
    Row(
        modifier = Modifier.fillMaxWidth().background(med.surface).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("SUPPORT", color = med.textSec, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
            Text("Steun de ontwikkelaar", color = med.textPri, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Sluiten", tint = med.textDim)
        }
    }
}

@Composable
private fun TipJarContent(store: TipStore, activity: Activity?) {
    val med = Med
    val products by store.products
    val purchased by store.purchased
    val purchasing by store.purchasing
    val failed by store.failed

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp)) {
                Box(
                    modifier = Modifier.size(80.dp).background(Color(0xFFCC1A1A).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("☕", fontSize = 38.sp) }
                Spacer(modifier = Modifier.height(14.dp))
                Text("BEDANKT VOOR JE VERTROUWEN", color = med.textSec, fontSize = 10.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Deze app is gratis en zonder advertenties.\nEen kleine bijdrage helpt enorm.",
                    color = med.textSec, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        if (products.isEmpty() && !failed) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = med.accent)
                }
            }
        } else {
            items(tipMeta, key = { it.id }) { meta ->
                val product = products.firstOrNull { it.productId == meta.id }
                TipRow(
                    meta = meta,
                    prijsTekst = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: meta.prijs,
                    isPurchased = purchased.contains(meta.id),
                    isPurchasing = purchasing == meta.id,
                    onClick = {
                        if (activity != null) {
                            if (product != null) store.koop(activity, product) else store.koopByID(activity, meta.id)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (purchased.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFCC1A1A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dank je wel! Dit betekent veel.", color = med.textSec, fontSize = 12.sp)
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Aankopen herstellen",
                    color = med.textDim, fontSize = 12.sp,
                    modifier = Modifier.clickable { store.herstelAankopen() },
                )
            }
        }
    }
}

@Composable
private fun TipRow(meta: TipMeta, prijsTekst: String, isPurchased: Boolean, isPurchasing: Boolean, onClick: () -> Unit) {
    val med = Med
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(med.card, RoundedCornerShape(12.dp))
            .clickable(enabled = !isPurchased) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(meta.kleur, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(meta.emoji, fontSize = 22.sp) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(meta.naam, color = med.textPri, fontSize = 15.sp)
            Text(meta.omschrijving, color = med.textSec, fontSize = 12.sp)
        }
        when {
            isPurchased -> Icon(Icons.Filled.Check, contentDescription = "Gekocht", tint = med.accent, modifier = Modifier.size(18.dp))
            isPurchasing -> CircularProgressIndicator(color = med.accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else -> Text(
                prijsTekst,
                color = med.bg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(med.accent, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}
