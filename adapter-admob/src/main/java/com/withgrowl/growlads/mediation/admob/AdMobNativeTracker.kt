package com.withgrowl.growlads.mediation.admob

import android.os.Bundle
import com.google.android.gms.ads.nativead.NativeAd
import com.withgrowl.growlandroidsdk.mediation.tracking.AdTracker

/**
 * AdMob counts impressions when [NativeAd.recordImpression] is called and
 * counts clicks when the user taps a registered CTA view inside a
 * `NativeAdView`. The tracker forwards Growl's `trackImpression` to AdMob;
 * `trackRender` and `trackClick` are no-ops because the relevant signals
 * fire automatically through `NativeAdView` registration in
 * [AdMobNativeAdRenderer].
 *
 * Mirrors iOS `AdMobNativeTracker`.
 */
public class AdMobNativeTracker(
    private val nativeAd: NativeAd,
) : AdTracker {

    override suspend fun trackRender() {
        // No-op: AdMob has no separate "render" event. Impression counts flow
        // through recordImpression after the NativeAdView is attached and
        // ≥50% visible.
    }

    override suspend fun trackImpression() {
        // Empty Bundle — manual impression recording for surfaces where
        // AdMob's automatic detection might not fire (e.g. when our
        // visibility tracker considers the ad visible before AdMob's own
        // view-tree walker does).
        nativeAd.recordImpression(Bundle.EMPTY)
    }

    override suspend fun trackClick() {
        // No-op: clicks fire via NativeAdView's setCallToActionView()
        // registration, not via NativeAd directly.
    }
}
