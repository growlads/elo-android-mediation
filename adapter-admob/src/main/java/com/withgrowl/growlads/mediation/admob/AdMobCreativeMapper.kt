package com.withgrowl.growlads.mediation.admob

import com.withgrowl.growlandroidsdk.GrowlAd
import com.withgrowl.growlandroidsdk.mediation.tracking.AdRenderer
import com.withgrowl.growlandroidsdk.mediation.tracking.AdTracker

/**
 * Asset bundle extracted from an AdMob `NativeAd` — the inputs to
 * [AdMobCreativeMapper]. Kept separate from `NativeAd` so the mapper
 * stays unit-testable without the AdMob SDK in classpath at test time.
 */
public data class AdMobNativeAssets(
    val identifier: String,
    val headline: String?,
    val body: String?,
    val imageUrl: String?,
    val clickUrl: String?,
)

/**
 * Maps an AdMob native ad's assets onto Growl's [GrowlAd] shape.
 *
 * Default mapping (mirrors iOS):
 * - title       = headline
 * - description = body (may be null)
 * - imageUrl    = first image URL
 *
 * Returns `null` to reject the creative — currently when the headline is
 * blank, since `GrowlAd.title` is required and AdMob occasionally returns
 * partial creatives.
 *
 * `clickUrl` on `GrowlAd` is non-null `String` — passes empty when AdMob
 * doesn't surface a URL because the click is handled via
 * `NativeAdView.callToActionView` registration, not URL launch.
 */
public object AdMobCreativeMapper {

    public fun makeCreative(
        assets: AdMobNativeAssets,
        tracker: AdTracker,
        renderer: AdRenderer,
    ): GrowlAd? {
        val headline = assets.headline?.takeIf { it.isNotBlank() } ?: return null
        return GrowlAd(
            id = assets.identifier,
            title = headline,
            description = assets.body,
            imageUrl = assets.imageUrl,
            clickUrl = assets.clickUrl ?: "",
            tracker = tracker,
            renderer = renderer,
        )
    }
}
