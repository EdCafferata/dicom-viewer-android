package info.cafferata.dicomviewer.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cafferata.dicomviewer.R
import info.cafferata.dicomviewer.billing.TipProductID
import info.cafferata.dicomviewer.billing.TipStore
import info.cafferata.dicomviewer.ui.theme.Med
import info.cafferata.dicomviewer.ui.theme.MedBadge
import info.cafferata.dicomviewer.ui.theme.MedDivider
import info.cafferata.dicomviewer.ui.theme.medCard

private data class Modality(val code: String, val name: Int, val short: Int, val long: Int)

private val modalities = listOf(
    Modality("CT", R.string.mod_ct, R.string.mod_ct_short, R.string.mod_ct_long),
    Modality("MR", R.string.mod_mr, R.string.mod_mr_short, R.string.mod_mr_long),
    Modality("XA", R.string.mod_xa, R.string.mod_xa_short, R.string.mod_xa_long),
    Modality("US", R.string.mod_us, R.string.mod_us_short, R.string.mod_us_long),
    Modality("CR", R.string.mod_cr, R.string.mod_cr_short, R.string.mod_cr_long),
    Modality("MG", R.string.mod_mg, R.string.mod_mg_short, R.string.mod_mg_long),
)

/** Centred, max-width scroll column so tablets don't stretch the cards. */
@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Med.bg), contentAlignment = Alignment.TopCenter) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) { content() }
    }
}

@Composable
fun LearnScreen() {
    var wlOpen by rememberSaveable { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<Modality?>(null) }

    ScreenColumn {
        ScreenHeader(stringResource(R.string.tab_learn), stringResource(R.string.learn_sub))

        Column(modifier = Modifier.fillMaxWidth().medCard(24.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.featured), color = Med.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.wl_title), color = Med.textPri, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.CenterStart) {
                val w = maxWidth
                Box(
                    modifier = Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(8.dp)).background(
                        Brush.horizontalGradient(0.30f to Color.Black, 0.62f to Color.White),
                    ),
                )
                Box(
                    modifier = Modifier.offset(x = w * 0.30f).size(w * 0.32f, 36.dp)
                        .border(2.dp, Med.accent, RoundedCornerShape(10.dp)),
                )
            }
            Row {
                Text(stringResource(R.string.dark), color = Med.textSec, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.window_word), color = Med.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.light), color = Med.textSec, fontSize = 12.sp)
            }
            Text(stringResource(R.string.wl_short), color = Color(0xFFC3CEDF), fontSize = 14.sp)
            if (wlOpen) Text(stringResource(R.string.wl_long), color = Color(0xFFC3CEDF), fontSize = 14.sp)
            Text(
                stringResource(if (wlOpen) R.string.less else R.string.read_more),
                color = Med.accentText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { wlOpen = !wlOpen },
            )
        }

        SectionTitle(stringResource(R.string.scan_types), Modifier.padding(top = 6.dp))
        modalities.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 96.dp).medCard().clickable { chosen = m }.padding(14.dp),
                    ) {
                        MedBadge(m.code)
                        Text(stringResource(m.name), color = Med.textPri, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(m.short), color = Med.textSec, fontSize = 12.sp)
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.viewer_tips), Modifier.padding(top = 6.dp))
        Column(modifier = Modifier.fillMaxWidth().medCard()) {
            TipLine(Icons.Filled.ZoomOutMap, stringResource(R.string.tip_pinch))
            MedDivider(Modifier.padding(start = 58.dp))
            TipLine(Icons.Filled.PanTool, stringResource(R.string.tip_drag))
            MedDivider(Modifier.padding(start = 58.dp))
            TipLine(Icons.Filled.TouchApp, stringResource(R.string.tip_double))
            MedDivider(Modifier.padding(start = 58.dp))
            TipLine(Icons.Filled.VisibilityOff, stringResource(R.string.tip_tap))
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Med.warn.copy(alpha = 0.12f))
                .border(1.dp, Med.warn.copy(alpha = 0.32f), RoundedCornerShape(18.dp)).padding(14.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF5B547))
            Spacer(Modifier.size(10.dp))
            Text(stringResource(R.string.not_diagnosis), color = Color(0xFFF6DDB0), fontSize = 13.sp)
        }
    }

    chosen?.let { m ->
        AlertDialog(
            onDismissRequest = { chosen = null },
            containerColor = Color(0xFF111827),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedBadge(m.code)
                    Text(stringResource(m.name), color = Med.textPri, fontWeight = FontWeight.ExtraBold)
                }
            },
            text = { Text(stringResource(m.long), color = Color(0xFFC3CEDF), fontSize = 16.sp) },
            confirmButton = { TextButton(onClick = { chosen = null }) { Text(stringResource(R.string.close), color = Med.accentText) } },
        )
    }
}

@Composable
private fun TipLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(Med.accent.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Med.accentText, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.size(12.dp))
        Text(text, color = Med.textPri, fontSize = 15.sp)
    }
}

@Composable
fun AboutScreen(tipStore: TipStore, versionName: String) {
    val context = LocalContext.current
    val activity = context as? Activity
    val products by tipStore.products
    val purchased by tipStore.purchased
    val purchasing by tipStore.purchasing

    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(15.dp)).border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(15.dp)),
            )
            Spacer(Modifier.size(14.dp))
            Column {
                Text("Dicom Viewer", color = Med.textPri, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.by_version, versionName), color = Med.textSec, fontSize = 14.sp)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().medCard(22.dp)) {
            Fact(stringResource(R.string.fact_no_account), stringResource(R.string.fact_no_account_sub))
            MedDivider(Modifier.padding(start = 60.dp))
            Fact(stringResource(R.string.fact_local), stringResource(R.string.fact_local_sub))
            MedDivider(Modifier.padding(start = 60.dp))
            Fact(stringResource(R.string.fact_offline), stringResource(R.string.fact_offline_sub))
        }

        // Tips
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Med.heart.copy(alpha = 0.08f))
                .border(1.dp, Med.heart.copy(alpha = 0.28f), RoundedCornerShape(22.dp)).padding(16.dp),
        ) {
            Text(stringResource(R.string.support_maker), color = Med.textPri, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(if (purchased.isEmpty()) R.string.tip_free else R.string.thanks),
                color = Color(0xFFE3C9CE), fontSize = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(TipProductID.SMALL, R.string.coffee, "€ 0,99"),
                    Triple(TipProductID.MEDIUM, R.string.lunch, "€ 2,99"),
                    Triple(TipProductID.LARGE, R.string.dinner, "€ 9,99"),
                ).forEach { (id, name, fallback) ->
                    val product = products.firstOrNull { it.productId == id }
                    val bought = id in purchased
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.07f)).border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                            .clickable(enabled = !bought && purchasing == null && activity != null) { activity?.let { tipStore.koopByID(it, id) } }
                            .padding(8.dp),
                    ) {
                        Text(stringResource(name), color = Med.textPri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        when {
                            purchasing == id -> CircularProgressIndicator(color = Med.textPri, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            bought -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Med.accentText)
                            else -> Text(
                                product?.oneTimePurchaseOfferDetails?.formattedPrice ?: fallback,
                                color = Med.textPri, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().medCard(22.dp)) {
            LinkLine(stringResource(R.string.help_contact)) { open("https://cafferata.info/dicom-viewer/en/support.html") }
            MedDivider(Modifier.padding(start = 16.dp))
            LinkLine(stringResource(R.string.privacy_policy)) { open("https://cafferata.info/dicom-viewer/en/privacy.html") }
            MedDivider(Modifier.padding(start = 16.dp))
            LinkLine(stringResource(R.string.rate_app)) { open("market://details?id=info.cafferata.dicomviewer") }
            MedDivider(Modifier.padding(start = 16.dp))
            LinkLine(stringResource(R.string.also_ios)) { open("https://apps.apple.com/app/id1483496527") }
        }

        Text(stringResource(R.string.disclaimer), color = Med.textDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun Fact(title: String, sub: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(Med.accent.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Check, contentDescription = null, tint = Med.accentText, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, color = Med.textPri, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Med.textSec, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LinkLine(title: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
    ) {
        Text(title, color = Med.textPri, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Med.textDim)
    }
}
