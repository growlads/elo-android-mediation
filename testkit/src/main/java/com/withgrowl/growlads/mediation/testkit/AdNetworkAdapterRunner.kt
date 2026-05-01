package com.withgrowl.growlads.mediation.testkit

import android.content.Context
import com.withgrowl.growlandroidsdk.mediation.AdBid
import com.withgrowl.growlandroidsdk.mediation.AdBidRequest
import com.withgrowl.growlandroidsdk.mediation.AdConsent
import com.withgrowl.growlandroidsdk.mediation.AdNetworkAdapter
import kotlinx.coroutines.runBlocking

/**
 * Drives an [AdNetworkAdapter] through its full lifecycle for conformance
 * tests. Captures whether [AdNetworkAdapter.start], [AdNetworkAdapter.bid],
 * and [AdNetworkAdapter.shutdown] threw.
 *
 * Use from JUnit tests via [run]; real Android `Context` is not constructed
 * here — pass a mocked Context if the adapter under test needs one (most
 * don't during unit tests).
 */
public class AdNetworkAdapterRunner(
    private val adapter: AdNetworkAdapter,
    private val context: Context,
    private val consent: AdConsent = MockAdConsent.granted,
) {

    public data class Result(
        val bid: AdBid?,
        val startError: Throwable?,
        val bidError: Throwable?,
        val shutdownError: Throwable?,
    )

    public fun run(request: AdBidRequest = MockAdBidRequest.build()): Result = runBlocking {
        val startError = runCatching { adapter.start(context, consent) }.exceptionOrNull()
        val bidResult = runCatching { adapter.bid(request) }
        val bid = bidResult.getOrNull()
        val bidError = bidResult.exceptionOrNull()
        val shutdownError = runCatching { adapter.shutdown() }.exceptionOrNull()

        Result(bid = bid, startError = startError, bidError = bidError, shutdownError = shutdownError)
    }
}
