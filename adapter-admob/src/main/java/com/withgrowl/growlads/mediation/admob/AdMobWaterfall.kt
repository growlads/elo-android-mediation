package com.withgrowl.growlads.mediation.admob

import com.withgrowl.growlandroidsdk.GrowlAd
import com.withgrowl.growlandroidsdk.mediation.AdBid
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sequential price-tier waterfall. Loads tiers in order; returns the first
 * fill as an [AdBid] using that tier's [AdMobPriceTier.eCpm] as the bid
 * value. Returns `null` if no tier fills before [timeoutMs] elapses.
 *
 * Mirrors iOS `AdMobWaterfall.firstFill`.
 */
internal object AdMobWaterfall {

    suspend fun firstFill(
        tiers: List<AdMobPriceTier>,
        timeoutMs: Long,
        loadAd: suspend (adUnitId: String) -> GrowlAd?,
    ): AdBid? {
        return withTimeoutOrNull(timeoutMs) {
            for (tier in tiers) {
                val ad = loadAd(tier.adUnitId) ?: continue
                return@withTimeoutOrNull AdBid(
                    networkId = "admob",
                    eCpm = tier.eCpm,
                    ad = ad,
                )
            }
            null
        }
    }
}
