package com.withgrowl.growlads.mediation.testkit

import com.withgrowl.growlandroidsdk.AdDisplayPosition
import com.withgrowl.growlandroidsdk.mediation.AdBidRequest
import com.withgrowl.growlandroidsdk.mediation.AdConsent

/**
 * Builder for [AdBidRequest] with sane defaults; override only the fields a
 * test cares about.
 *
 * ```
 * val req = MockAdBidRequest.build {
 *     adUnitId = "ca-app-pub-test/floor"
 *     timeoutMs = 500
 * }
 * ```
 */
public object MockAdBidRequest {

    public class Builder {
        public var adUnitId: String = "test-ad-unit"
        public var consent: AdConsent = MockAdConsent.granted
        public var timeoutMs: Long = 1_000
        public var character: String? = null
        public var conversationId: String? = null
        public var variantId: String? = null
        public var displayPosition: AdDisplayPosition? = null
        public var otherParams: Map<String, String> = emptyMap()
    }

    public fun build(configure: Builder.() -> Unit = {}): AdBidRequest {
        val b = Builder().apply(configure)
        return AdBidRequest(
            messages = emptyList(),
            contextObjects = emptyList(),
            adUnitId = b.adUnitId,
            consent = b.consent,
            timeoutMs = b.timeoutMs,
            character = b.character,
            conversationId = b.conversationId,
            variantId = b.variantId,
            displayPosition = b.displayPosition,
            otherParams = b.otherParams,
        )
    }
}
