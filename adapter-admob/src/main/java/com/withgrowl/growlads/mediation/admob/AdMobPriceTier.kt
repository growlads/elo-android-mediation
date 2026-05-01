package com.withgrowl.growlads.mediation.admob

/**
 * One AdMob ad unit configured at a fixed eCPM floor.
 *
 * AdMob does not surface a programmatic bid price for native ads. The
 * adapter loads tiers in order (highest eCPM first) and returns the first
 * fill — that tier's [eCpm] becomes the bid value reported to the
 * mediator. *Which* tier fills is driven by AdMob's actual demand at each
 * floor, so the auction reflects real demand within the resolution of the
 * configured tiers.
 */
public data class AdMobPriceTier(
    val adUnitId: String,
    val eCpm: Double,
) {
    init {
        require(adUnitId.isNotBlank()) { "AdMobPriceTier.adUnitId must not be blank" }
        require(eCpm >= 0.0) { "AdMobPriceTier.eCpm must be non-negative; got $eCpm" }
    }
}
