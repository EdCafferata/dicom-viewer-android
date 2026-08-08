package info.cafferata.dicomviewer.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Play Billing-wrapper voor de tip jar (Android-equivalent van de StoreKit 2
 * TipStore op iOS): laadt de drie fooi-producten, koopt ze, en houdt bij welke
 * al gekocht zijn (niet-verbruikbaar, blijft staan als vinkje net als op iOS).
 */
class TipStore(context: Context) : PurchasesUpdatedListener {
    private val _products = mutableStateOf<List<ProductDetails>>(emptyList())
    val products: State<List<ProductDetails>> = _products

    private val _purchased = mutableStateOf<Set<String>>(emptySet())
    val purchased: State<Set<String>> = _purchased

    private val _purchasing = mutableStateOf<String?>(null)
    val purchasing: State<String?> = _purchasing

    private val _failed = mutableStateOf(false)
    val failed: State<Boolean> = _failed

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    laadProducten()
                    laadAankopen()
                } else {
                    _failed.value = true
                }
            }

            override fun onBillingServiceDisconnected() {
                // Volgende actie van de gebruiker triggert een herverbinding via startConnection.
            }
        })
    }

    fun laadProducten() {
        val productList = TipProductID.ALL.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val gevonden = queryProductDetailsResult.productDetailsList
                if (gevonden.isEmpty()) {
                    _failed.value = true
                } else {
                    _products.value = gevonden.sortedBy {
                        it.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L
                    }
                }
            } else {
                _failed.value = true
            }
        }
    }

    fun koop(activity: Activity, product: ProductDetails) {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        _purchasing.value = product.productId
        billingClient.launchBillingFlow(activity, flowParams)
    }

    /** Probeert het product opnieuw te laden als het nog niet beschikbaar was, zelfde als iOS' purchaseByID. */
    fun koopByID(activity: Activity, productId: String) {
        val bestaand = _products.value.firstOrNull { it.productId == productId }
        if (bestaand != null) {
            koop(activity, bestaand)
            return
        }
        val productList = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(productList)).build()
        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val gevonden = queryProductDetailsResult.productDetailsList.firstOrNull()
                if (gevonden != null) {
                    _products.value = _products.value + gevonden
                    koop(activity, gevonden)
                }
            }
        }
    }

    fun herstelAankopen() {
        laadAankopen()
    }

    private fun laadAankopen() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

            val gekocht = purchases
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .flatMap { it.products }
                .toSet()
            _purchased.value = _purchased.value + gekocht

            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgeParams) { _ -> }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _purchasing.value = null
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            laadAankopen()
        }
        // Bij annuleren of fout: gewoon niets doen, zelfde gedrag als iOS' purchase().
    }
}
