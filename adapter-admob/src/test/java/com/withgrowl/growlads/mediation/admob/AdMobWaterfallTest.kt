package com.withgrowl.growlads.mediation.admob

import com.withgrowl.growlandroidsdk.GrowlAd
import com.withgrowl.growlandroidsdk.mediation.tracking.AdRenderer
import com.withgrowl.growlandroidsdk.mediation.tracking.AdTracker
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdMobWaterfallTest {

    private fun fakeAd(id: String) = GrowlAd(
        id = id,
        title = "t",
        description = null,
        imageUrl = null,
        clickUrl = "",
        tracker = mockk<AdTracker>(relaxed = true),
        renderer = mockk<AdRenderer>(relaxed = true),
    )

    @Test
    fun `returns first fill from highest-priority tier`() = runTest {
        val tiers = listOf(
            AdMobPriceTier("u-high", 5.0),
            AdMobPriceTier("u-mid", 2.0),
            AdMobPriceTier("u-low", 0.5),
        )
        val bid = AdMobWaterfall.firstFill(
            tiers = tiers,
            timeoutMs = 1_000,
            loadAd = { unitId -> if (unitId == "u-high") fakeAd("ad-high") else null },
        )
        assertEquals(5.0, bid?.eCpm ?: 0.0, 0.0001)
        assertEquals("ad-high", bid?.ad?.id)
        assertEquals("admob", bid?.networkId)
    }

    @Test
    fun `falls through to lower tier when higher returns null`() = runTest {
        val tiers = listOf(
            AdMobPriceTier("u-high", 5.0),
            AdMobPriceTier("u-mid", 2.0),
        )
        val bid = AdMobWaterfall.firstFill(
            tiers = tiers,
            timeoutMs = 1_000,
            loadAd = { unitId -> if (unitId == "u-mid") fakeAd("ad-mid") else null },
        )
        assertEquals(2.0, bid?.eCpm ?: 0.0, 0.0001)
        assertEquals("ad-mid", bid?.ad?.id)
    }

    @Test
    fun `returns null when no tier fills`() = runTest {
        val tiers = listOf(AdMobPriceTier("u-high", 5.0))
        val bid = AdMobWaterfall.firstFill(
            tiers = tiers,
            timeoutMs = 1_000,
            loadAd = { _ -> null },
        )
        assertNull(bid)
    }
}
