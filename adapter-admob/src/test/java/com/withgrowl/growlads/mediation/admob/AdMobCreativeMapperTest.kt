package com.withgrowl.growlads.mediation.admob

import com.withgrowl.growlandroidsdk.mediation.tracking.AdRenderer
import com.withgrowl.growlandroidsdk.mediation.tracking.AdTracker
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AdMobCreativeMapperTest {

    private val tracker = mockk<AdTracker>(relaxed = true)
    private val renderer = mockk<AdRenderer>(relaxed = true)

    @Test
    fun `maps headline to title and body to description`() {
        val ad = AdMobCreativeMapper.makeCreative(
            assets = AdMobNativeAssets(
                identifier = "admob:abc",
                headline = "Buy this thing",
                body = "It's a thing you should buy",
                imageUrl = "https://cdn.example.com/img.jpg",
                clickUrl = null,
            ),
            tracker = tracker,
            renderer = renderer,
        )
        assertNotNull(ad)
        assertEquals("Buy this thing", ad!!.title)
        assertEquals("It's a thing you should buy", ad.description)
        assertEquals("https://cdn.example.com/img.jpg", ad.imageUrl)
        assertEquals("admob:abc", ad.id)
    }

    @Test
    fun `null body becomes null description`() {
        val ad = AdMobCreativeMapper.makeCreative(
            assets = AdMobNativeAssets(
                identifier = "admob:abc",
                headline = "Headline only",
                body = null,
                imageUrl = null,
                clickUrl = null,
            ),
            tracker = tracker,
            renderer = renderer,
        )
        assertNotNull(ad)
        assertNull(ad!!.description)
    }

    @Test
    fun `blank headline returns null (rejects creative)`() {
        val ad = AdMobCreativeMapper.makeCreative(
            assets = AdMobNativeAssets(
                identifier = "admob:abc",
                headline = "",
                body = "body",
                imageUrl = null,
                clickUrl = null,
            ),
            tracker = tracker,
            renderer = renderer,
        )
        assertNull(ad)
    }
}
