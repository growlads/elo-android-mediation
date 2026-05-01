package com.withgrowl.growlads.mediation.admob

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdMobNetworkAdapterTest {

    @Test
    fun `networkId is admob`() {
        val adapter = AdMobNetworkAdapter(
            priceTiers = listOf(AdMobPriceTier("u", 1.0)),
        )
        assertEquals("admob", adapter.networkId)
    }

    @Test
    fun `requires non-empty price tiers`() {
        assertThrows(IllegalArgumentException::class.java) {
            AdMobNetworkAdapter(priceTiers = emptyList())
        }
    }
}
