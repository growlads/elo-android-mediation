package com.withgrowl.growlads.mediation.admob

import android.os.Bundle
import com.google.android.gms.ads.RequestConfiguration
import com.withgrowl.growlandroidsdk.mediation.AdConsent

/**
 * Translates Growl's [AdConsent] snapshot into AdMob-compatible signals.
 *
 * AdMob exposes COPPA / TFUA via [RequestConfiguration]; non-personalized-ad
 * opt-out via the per-request `npa=1` extra (Google Mobile Ads SDK
 * convention).
 *
 * Mirrors iOS `AdMobConsent`.
 */
public object AdMobConsent {

    public fun toRequestConfiguration(consent: AdConsent): RequestConfiguration {
        val coppa = if (consent.coppa) {
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        } else {
            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        }
        val tfua = if (consent.tfua) {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        } else {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
        }
        return RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(coppa)
            .setTagForUnderAgeOfConsent(tfua)
            .build()
    }

    /**
     * Returns a Bundle suitable for AdMob's NPA extras when GDPR applies and
     * consent for personalized ads is denied. Returns `null` when
     * personalized ads are allowed (no extras needed).
     */
    public fun nonPersonalizedAdParameters(consent: AdConsent): Bundle? {
        val gdprApplies = consent.gdprApplies ?: return null
        if (!gdprApplies) return null
        // Heuristic: a TCF string starting with all-zero consent vector means
        // personalized ads are denied. Absence of TCF when GDPR applies is
        // treated the same way (denied) — safer default for compliance.
        val tcf = consent.tcfString
        val personalizedAllowed = tcf != null && !tcf.startsWith("CO00000")
        return if (personalizedAllowed) null else Bundle().apply { putString("npa", "1") }
    }
}
