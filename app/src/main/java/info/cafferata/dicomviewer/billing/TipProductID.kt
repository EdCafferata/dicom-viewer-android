package info.cafferata.dicomviewer.billing

/**
 * Product-ID's zoals aan te maken in Play Console > Producten met eenmalige betaling.
 * Zelfde drie tiers als de iOS-tip jar (info.cafferata.dicomplayer.tip.small/medium/large).
 */
object TipProductID {
    const val SMALL = "tip_small"
    const val MEDIUM = "tip_medium"
    const val LARGE = "tip_large"

    val ALL = listOf(SMALL, MEDIUM, LARGE)
}
