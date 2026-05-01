package com.withgrowl.growlads.mediation.admob

import com.google.android.gms.ads.nativead.NativeAd
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdMobNativeTrackerTest {

    @Test
    fun `trackImpression delegates to NativeAd recordImpression`() = runTest {
        val nativeAd = mockk<NativeAd>(relaxed = true)
        val tracker = AdMobNativeTracker(nativeAd)
        tracker.trackImpression()
        verify(exactly = 1) { nativeAd.recordImpression(any()) }
    }

    @Test
    fun `trackRender is a no-op (AdMob fires render via NativeAdView attachment)`() = runTest {
        val nativeAd = mockk<NativeAd>(relaxed = true)
        val tracker = AdMobNativeTracker(nativeAd)
        tracker.trackRender()
        verify(exactly = 0) { nativeAd.recordImpression(any()) }
    }

    @Test
    fun `trackClick is a no-op (AdMob fires click via NativeAdView CTA registration)`() = runTest {
        val nativeAd = mockk<NativeAd>(relaxed = true)
        val tracker = AdMobNativeTracker(nativeAd)
        tracker.trackClick()
        verify(exactly = 0) { nativeAd.recordImpression(any()) }
    }
}
