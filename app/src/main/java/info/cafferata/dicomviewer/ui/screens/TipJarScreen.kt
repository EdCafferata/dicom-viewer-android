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
import info.cafferata.dicomviewer.ui.theme.medGlassCircle

private data class TipMeta(val id: String, val emoji: String, val naam: Int, val prijs: String, val omschrijving: Int, val kleur: Color)

private val tipMeta = listOf(
    TipMeta(TipProductID.SMALL, "☕", info.cafferata.dicomviewer.R.string.coffee, "€ 0,99", info.cafferata.dicomviewer.R.string.tip_small_desc, Color(0xFFB57A3D)),
    TipMeta(TipProductID.MEDIUM, "🍕", info.cafferata.dicomviewer.R.string.lunch, "€ 2,99", info.cafferata.dicomviewer.R.string.tip_medium_desc, Color(0xFFDC4F33)),
    TipMeta(TipProductID.LARGE, "🍽️", info.cafferata.dicomviewer.R.string.dinner, "€ 9,99", info.cafferata.dicomviewer.R.string.tip_large_desc, Color(0xFF3DA178)),
)

@Composable
fun TipJarScreen(store: TipStore, automatic: Boolean = false, onDontShowAgain: () -> Unit = {}, onDismiss: () -> Unit) {
    val med = Med
    val context = LocalContext.current
    val activity = context as? Activity

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(med.bg)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                TipJarHeader(onClose = onDismiss)
                TipJarContent(store = store, activity = activity, automatic = automatic, onDontShowAgain = { onDontShowAgain(); onDismiss() })
            }
        }
    }
}

@Composable
private fun TipJarHeader(onClose: () -> Unit) {
    val med = Med
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.support_maker), color = med.textPri, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onClose, modifier = Modifier.size(44.dp).medGlassCircle()) {
            Icon(Icons.Filled.Close, contentDescription = androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.close), tint = med.textPri)
        }
    }
}

@Composable
private fun TipJarContent(store: TipStore, activity: Activity?, automatic: Boolean, onDontShowAgain: () -> Unit) {
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
                Text(androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.thank_trust), color = med.textPri, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.tipjar_intro),
                    color = med.textSec, fontSize = 15.sp, textAlign = TextAlign.Center,
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
                    Text(androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.thanks), color = med.textSec, fontSize = 13.sp)
                }
            }
        }

        item {
            val ctx = LocalContext.current
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(48.dp)
                    .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(50))
                    .clickable { runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=info.cafferata.dicomviewer"))) } },
                contentAlignment = Alignment.Center,
            ) { Text("★  " + androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.rate_dicom), color = med.textPri, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                Text(
                    androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.restore),
                    color = med.textSec, fontSize = 13.sp,
                    modifier = Modifier.clickable { store.herstelAankopen() },
                )
                if (automatic) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(androidx.compose.ui.res.stringResource(info.cafferata.dicomviewer.R.string.dont_show_again), color = med.textSec, fontSize = 13.sp, modifier = Modifier.clickable { onDontShowAgain() })
                }
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
            .background(med.card, RoundedCornerShape(20.dp))
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
            Text(androidx.compose.ui.res.stringResource(meta.naam), color = med.textPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(androidx.compose.ui.res.stringResource(meta.omschrijving), color = med.textSec, fontSize = 13.sp)
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
