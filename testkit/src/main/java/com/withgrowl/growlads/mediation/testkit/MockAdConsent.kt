package com.withgrowl.growlads.mediation.testkit

import com.withgrowl.growlandroidsdk.mediation.AdConsent

/**
 * Preset [AdConsent] snapshots for adapter unit tests.
 *
 * Use these instead of constructing `AdConsent` by hand in every test —
 * keeps adapter tests focused on adapter behavior, not consent plumbing.
 */
public object MockAdConsent {

    /** All non-restricted defaults; gdprApplies = false. */
    public val granted: AdConsent = AdConsent(
        coppa = false,
        tfua = false,
        gdprApplies = false,
        tcfString = null,
        addtlConsent = null,
        gppString = null,
        gppSid = null,
    )

    /** GDPR applies but consent denied — TCF string of all zeros. */
    public val denied: AdConsent = granted.copy(
        gdprApplies = true,
        tcfString = "CO0000000000000000000000000000000000000",
    )

    /** Restricted: COPPA + TFUA both on. */
    public val restricted: AdConsent = granted.copy(
        coppa = true,
        tfua = true,
    )

    /** Unknown: gdprApplies = null (not yet determined). */
    public val unknown: AdConsent = granted.copy(gdprApplies = null)
}
