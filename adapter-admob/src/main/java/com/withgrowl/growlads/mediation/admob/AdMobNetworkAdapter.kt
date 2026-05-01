package com.withgrowl.growlads.mediation.admob

import android.content.Context
import com.google.ads.mediation.admob.AdMobAdapter as GoogleAdMobExtras
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.withgrowl.growlandroidsdk.GrowlAd
import com.withgrowl.growlandroidsdk.mediation.AdAdapterError
import com.withgrowl.growlandroidsdk.mediation.AdBid
import com.withgrowl.growlandroidsdk.mediation.AdBidRequest
import com.withgrowl.growlandroidsdk.mediation.AdConsent
import com.withgrowl.growlandroidsdk.mediation.AdNetworkAdapter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * AdMob adapter for Growl's mediation auction.
 *
 * ```
 * Growl.configure(GrowlConfiguration(
 *     ...,
 *     adapters = listOf(
 *         AdMobNetworkAdapter(priceTiers = listOf(
 *             AdMobPriceTier(adUnitId = "ca-app-pub-.../high",  eCpm = 5.00),
 *             AdMobPriceTier(adUnitId = "ca-app-pub-.../mid",   eCpm = 2.00),
 *             AdMobPriceTier(adUnitId = "ca-app-pub-.../floor", eCpm = 0.50),
 *         )),
 *     ),
 * ))
 * ```
 *
 * @param priceTiers ordered ladder of AdMob ad units + eCPM floors. The
 * adapter walks them top-down on each request and bids the first non-empty
 * fill into Elo's auction.
 * @param sponsoredLabel attribution label rendered above each AdMob
 * creative. Defaults to `"Sponsored"`; pass a localized string for non-
 * English markets (e.g. `"Werbung"`, `"広告"`).
 *
 * Mirrors iOS `AdMobNetworkAdapter`.
 */
public class AdMobNetworkAdapter(
    private val priceTiers: List<AdMobPriceTier>,
    private val sponsoredLabel: String = "Sponsored",
) : AdNetworkAdapter {

    init {
        require(priceTiers.isNotEmpty()) {
            "AdMobNetworkAdapter requires at least one AdMobPriceTier"
        }
    }

    override val networkId: String = "admob"

    private var startedContext: Context? = null

    override suspend fun start(context: Context, consent: AdConsent) {
        MobileAds.setRequestConfiguration(AdMobConsent.toRequestConfiguration(consent))
        suspendCancellableCoroutine<Unit> { cont ->
            MobileAds.initialize(context.applicationContext) {
                if (cont.isActive) cont.resume(Unit)
            }
        }
        startedContext = context.applicationContext
    }

    override suspend fun bid(request: AdBidRequest): AdBid? {
        val ctx = startedContext ?: throw AdAdapterError.NotStarted
        return AdMobWaterfall.firstFill(
            tiers = priceTiers,
            timeoutMs = request.timeoutMs,
            loadAd = { adUnitId -> loadNativeAd(ctx, adUnitId, request) },
        )
    }

    private suspend fun loadNativeAd(
        context: Context,
        adUnitId: String,
        request: AdBidRequest,
    ): GrowlAd? = suspendCancellableCoroutine { cont ->
        val gadRequestBuilder = AdRequest.Builder()
        AdMobConsent.nonPersonalizedAdParameters(request.consent)?.let { extras ->
            gadRequestBuilder.addNetworkExtrasBundle(GoogleAdMobExtras::class.java, extras)
        }
        val gadRequest = gadRequestBuilder.build()

        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd: NativeAd ->
                if (!cont.isActive) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                val tracker = AdMobNativeTracker(nativeAd)
                val renderer = AdMobNativeAdRenderer(nativeAd, sponsoredLabel = sponsoredLabel)
                val ad = AdMobCreativeMapper.makeCreative(
                    assets = AdMobNativeAssets(
                        identifier = stableCreativeId(nativeAd),
                        headline = nativeAd.headline,
                        body = nativeAd.body,
                        imageUrl = nativeAd.images.firstOrNull()?.uri?.toString(),
                        clickUrl = null,
                    ),
                    tracker = tracker,
                    renderer = renderer,
                )
                cont.resume(ad)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!cont.isActive) return
                    if (error.code == AdRequest.ERROR_CODE_NO_FILL) {
                        cont.resume(null)
                    } else {
                        cont.resumeWithException(
                            AdAdapterError.Network("AdMob load failed: code=${error.code} msg=${error.message}")
                        )
                    }
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        loader.loadAd(gadRequest)
        cont.invokeOnCancellation {
            // AdLoader has no explicit cancel hook; once a load is in flight
            // the callback will fire. The cont.isActive guards above prevent
            // double-resume.
        }
    }

    override fun shutdown() {
        startedContext = null
    }

    private fun stableCreativeId(nativeAd: NativeAd): String {
        val responseId = nativeAd.responseInfo?.responseId
        return if (!responseId.isNullOrBlank()) "admob:$responseId" else "admob:${System.identityHashCode(nativeAd)}"
    }
}
