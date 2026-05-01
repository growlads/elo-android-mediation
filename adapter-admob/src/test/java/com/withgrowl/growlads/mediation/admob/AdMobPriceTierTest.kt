package com.withgrowl.growlads.mediation.admob

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdMobPriceTierTest {

    @Test
    fun `creates tier with positive eCpm`() {
        val tier = AdMobPriceTier(adUnitId = "ca-app-pub-x/floor", eCpm = 0.50)
        assertEquals("ca-app-pub-x/floor", tier.adUnitId)
        assertEquals(0.50, tier.eCpm, 0.0001)
    }

    @Test
    fun `rejects blank adUnitId`() {
        assertThrows(IllegalArgumentException::class.java) {
            AdMobPriceTier(adUnitId = "  ", eCpm = 1.0)
        }
    }

    @Test
    fun `rejects negative eCpm`() {
        assertThrows(IllegalArgumentException::class.java) {
            AdMobPriceTier(adUnitId = "x", eCpm = -1.0)
        }
    }
}
