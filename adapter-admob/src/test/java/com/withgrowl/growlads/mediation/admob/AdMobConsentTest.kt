package com.withgrowl.growlads.mediation.admob

import com.google.android.gms.ads.RequestConfiguration
import com.withgrowl.growlads.mediation.testkit.MockAdConsent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdMobConsentTest {

    @Test
    fun `granted maps to no child-directed and no under-age treatment`() {
        val cfg = AdMobConsent.toRequestConfiguration(MockAdConsent.granted)
        assertEquals(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE, cfg.tagForChildDirectedTreatment)
        assertEquals(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE, cfg.tagForUnderAgeOfConsent)
    }

    @Test
    fun `restricted maps to child-directed true and under-age true`() {
        val cfg = AdMobConsent.toRequestConfiguration(MockAdConsent.restricted)
        assertEquals(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE, cfg.tagForChildDirectedTreatment)
        assertEquals(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE, cfg.tagForUnderAgeOfConsent)
    }

    @Test
    fun `denied gdpr returns NPA extras bundle with npa=1`() {
        val extras = AdMobConsent.nonPersonalizedAdParameters(MockAdConsent.denied)
        assertEquals("1", extras?.getString("npa"))
    }

    @Test
    fun `granted gdpr returns null NPA extras (personalized ads ok)`() {
        val extras = AdMobConsent.nonPersonalizedAdParameters(MockAdConsent.granted)
        assertEquals(null, extras)
    }
}
