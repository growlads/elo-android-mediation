# Adapter Author Guide

How to add a new ad network to the Elo Android SDK by writing an `AdNetworkAdapter`. The contract here is **v1** and the SDK is pre-1.0 — expect breaking changes; we'll call them out in release notes.

For the bigger-picture mediation design, see the spec at `docs/superpowers/specs/2026-04-29-android-sdk-parity-with-ios-design.md`. For consumer-facing usage, see `README.md`.

---

## What an adapter is

An `AdNetworkAdapter` is a single ad demand source registered on `GrowlConfiguration.adapters`. The SDK runs a parallel first-price auction on every `Growl.loadAd` / `Growl.preloadAd` call — every registered adapter receives the same `AdBidRequest` concurrently, and the mediator picks the highest-eCPM `AdBid` returned.

Adapters live in their own Gradle module (typically a separate published artifact, e.g. `com.withgrowl:growl-android-mediation-admob`). They depend on the SDK as a `compileOnly` API consumer; the consumer app pulls in both the SDK and the adapter modules.

Elo's own demand is wired in by the SDK as a first-party adapter (`GrowlNetworkAdapter`); consumers don't construct or register it.

---

## The contract

```kotlin
public interface AdNetworkAdapter {
    public val networkId: String

    public suspend fun start(context: Context, consent: AdConsent) {}

    public suspend fun bid(request: AdBidRequest): AdBid?

    public fun shutdown() {}
}
```

Defined in `com.withgrowl.growlandroidsdk.mediation.AdNetworkAdapter`.

### `networkId`

Stable identifier for your network — `"admob"`, `"meta"`, `"applovin"`. Used for logging and for attributing winning bids in `mediationDebugSnapshot()`. Must be **unique** across all registered adapters; the `ConfigurationValidator` reports duplicates as a warning, but the mediator does **not** auto-dedupe — both adapters with the same ID will be invoked.

### `start(context, consent)`

Called once before the adapter's first auction. Use this to:

- Initialize the underlying network SDK (e.g. `MobileAds.initialize(context)` for AdMob)
- Apply the privacy snapshot in `consent` to the underlying SDK's request configuration

Default implementation is a no-op — only override if your underlying SDK actually requires startup. The `consent` parameter is the same `AdConsent` passed on every `bid` request; some networks need it at init time (AdMob), others read it fresh per request.

If `start` throws, the mediator records the failure in `MediationDebugRecorder` and the adapter is **excluded from auctions** until the SDK is reconfigured. Bid-time `start` retries are intentionally not part of v1.

### `bid(request: AdBidRequest): AdBid?`

The hot path. Called once per auction. **Return value semantics:**

| Return                       | Meaning                                   | Mediator behavior                                  |
|------------------------------|-------------------------------------------|----------------------------------------------------|
| `AdBid(...)`                 | Adapter has demand at the given eCPM      | Enters the auction; highest eCPM wins             |
| `null`                       | Soft no-fill (normal — this network just doesn't have an ad right now) | Adapter sits out this auction; not an error       |
| `throw AdAdapterError(...)`  | Hard error (network failure, invalid creative, SDK misconfigured) | Recorded in debug snapshot; adapter sits out this auction |
| `throw CancellationException` | The mediator cancelled you (timeout)     | Don't catch this. Re-throw if you must catch broadly |

**Cancellation contract:** the mediator wraps every `bid` call in `withTimeoutOrNull(request.timeoutMs)`. Adapters should be cancellable — if your underlying SDK takes longer than `timeoutMs`, your in-flight work must be cancellable so the auction can finish on time. Use `withContext(Dispatchers.IO)` for blocking calls, propagate cancellation through suspending APIs, and never catch `CancellationException` without re-throwing.

**Concurrency:** the mediator invokes `bid` from `coroutineScope { async { adapter.bid(req) } }` per auction. Adapter implementations must be safe to call concurrently across threads. If your underlying SDK isn't thread-safe, gate it behind a `Mutex`.

### `shutdown()`

Called from `Growl.shutdown()`. Tear down anything `start` brought up. Default is a no-op; override if your network's SDK has a real shutdown hook.

---

## `AdBidRequest`

```kotlin
public data class AdBidRequest(
    val messages: List<ChatMessage>,
    val contextObjects: List<ContextObject>,
    val adUnitId: String,
    val consent: AdConsent,
    val timeoutMs: Long,
    val character: String? = null,
    val conversationId: String? = null,
    val variantId: String? = null,
    val displayPosition: AdDisplayPosition? = null,
    val otherParams: Map<String, String> = emptyMap(),
)
```

Most third-party networks ignore `messages`, `contextObjects`, `character`, `conversationId`, `variantId` — those exist to feed Elo's contextual targeting. **Don't forward them to other networks** unless you have explicit consent and a privacy review; chat messages are sensitive by default.

`adUnitId` is the publisher's Growl ad unit ID. Map it to your network's placement ID via your own configuration (passed into your adapter's constructor by the consumer app).

`consent` is read once per auction by `GrowlState` and snapshotted on every adapter so they all bid against the same picture. See `AdConsent` for fields.

`timeoutMs` is the per-auction budget — already a `Long` in milliseconds. Don't accept smaller-than-`Long` types; cast at the boundary if your underlying SDK uses seconds.

`displayPosition` (`Top`, `Bottom`, `null`) lets some networks pick differently-shaped creatives. Optional; ignore if irrelevant.

`otherParams` is an opaque `Map<String, String>` for per-publisher knobs that don't deserve a top-level field. Convention: namespace your keys with your `networkId` (`"admob.test_device_id"`).

---

## `AdBid`

```kotlin
public data class AdBid(
    val networkId: String,
    val eCpm: Double,
    val ad: GrowlAd,
)
```

`networkId` should match the adapter's `networkId` — set it explicitly so a single adapter that fronts multiple networks (rare but legal) can attribute correctly.

`eCpm` is the price you're bidding, in USD-equivalent CPM (cost per thousand impressions). The mediator picks the highest. **Don't lie** — bidding above your real eCPM gets you delisted. Networks that don't surface a price at request-time (Elo's first-party adapter is one) report a publisher-configured `assumedECpm` instead.

`ad` is your adapter's `GrowlAd` — **you map your network's creative payload to the SDK's `GrowlAd` shape inside your adapter, not in the SDK**. Critically, `GrowlAd.tracker` (an `AdTracker` instance) is how the SDK fires render/impression/click telemetry without knowing your network. See "Tracking" below.

---

## `AdAdapterError`

```kotlin
public sealed class AdAdapterError(...) : Exception(...) {
    public object NotStarted : ...
    public class InvalidConfiguration(detail: String) : ...
    public class InvalidRequest(detail: String) : ...
    public class Network(detail: String) : ...
    public class InvalidCreative(detail: String) : ...
    public object Timeout : ...
    public class Underlying(detail: String) : ...
}
```

Pick the most specific case. The `label` field surfaces in `MediationDebugSnapshot`'s per-adapter outcome and is what publishers see when triaging fill issues.

- `NotStarted` — `bid` was called before `start` finished, or `start` failed and was never retried. Throw this from `bid` if your adapter detects the missing init state.
- `InvalidConfiguration` — your adapter's constructor params are wrong (missing app ID, malformed credentials). Detect at `start` time, throw at `bid` time.
- `InvalidRequest` — the `AdBidRequest` is invalid for this network (e.g. `adUnitId` doesn't map to anything you know).
- `Network` — HTTP failure, timeout calling your network's SDK, etc. Pass the underlying exception's message in `detail`.
- `InvalidCreative` — the network responded with a creative your adapter can't map to `GrowlAd` (missing required fields).
- `Timeout` — your underlying SDK reported a timeout. (Auction-level timeouts are handled by the mediator's `withTimeoutOrNull`; you don't need to throw `Timeout` for those — `null` works.)
- `Underlying` — last resort for exceptions that don't fit the above. Wrap and rethrow.

**Don't throw raw exceptions** — wrap them in an `AdAdapterError`. The mediator catches `AdAdapterError` and records it; raw `Throwable`s break the auction for everyone.

---

## Tracking

Every `GrowlAd` you return on a winning bid must have an `AdTracker` attached:

```kotlin
public interface AdTracker {
    public suspend fun trackRender()
    public suspend fun trackImpression()
    public suspend fun trackClick()
}
```

The SDK calls these three methods at the right moments — render fires on first composition of the ad view, impression fires after the ≥50%-visible-for-1-second viewability gate, click fires when the user taps. Your `AdTracker` is where network-specific telemetry happens.

Implement `AdTracker` inside your adapter module — third-party adapters should ship their own implementation tailored to how their network reports impressions and clicks. The SDK's first-party `UrlPingTracker` is `internal` and wired to Elo's internal HTTP client; it is not part of the public API surface.

- **URL-pinged networks**: write a tracker that uses your own HTTP client (OkHttp, Ktor, etc.) to fire pings off the UI thread. Wrap each call in `withContext(Dispatchers.IO)` and use `runCatching` so a failed ping never throws into the SDK.
- **SDK-callback networks** (e.g. AdMob): implement an `AdTracker` that calls `nativeAd.recordImpression()` inside `trackImpression()` and `nativeAd.performClick(asset)` inside `trackClick()`.

Methods are `suspend` and may be called from any coroutine context. Keep them fast — they fire on the UI render path.

**Telemetry stays inside the adapter.** The SDK never reads network-specific fields off `GrowlAd`. If you need to log, instrument, or report something specific to your network, do it inside the `AdTracker` implementation, not by patching the SDK.

---

## Custom rendering (`AdRenderer`)

Some ad networks (notably AdMob and Meta Audience Network) require the creative to be displayed inside a network-owned native `View` for impressions and clicks to count. The SDK exposes `AdRenderer` (in `mediation/tracking/`) for this case:

```kotlin
@MainThread
public interface AdRenderer {
    public val minimumDisplayHeightDp: Int get() = 0
    public fun makeView(context: Context): View
    public fun update(view: View) {}
    public fun release(view: View) {}
}
```

When your adapter's `bid()` returns an `AdBid`, attach an `AdRenderer` to the `GrowlAd` via the optional `renderer` parameter. The SDK's `GrowlAdView` detects the renderer and embeds the returned `View` via `AndroidView { ... }`, which lets the network's SDK run its own click and impression attribution on the registered subview tree.

```kotlin
GrowlAd(
    id = …,
    title = nativeAd.headline.orEmpty(),
    description = nativeAd.body,
    imageUrl = nativeAd.images.firstOrNull()?.uri?.toString(),
    // Renderer-rendered ads dispatch clicks through the network's own
    // CTA registration (e.g. AdMob's NativeAdView callToActionView), so
    // clickUrl is unused on this path. URL-ping adapters must pass a real
    // destination here.
    clickUrl = "",
    tracker = MyAdTracker(nativeAd),
    renderer = MyAdRenderer(nativeAd),  // optional; null for url-ping ads
)
```

Set `renderer = null` (the default) for URL-ping-tracked ads — Growl's own `GrowlNetworkAdapter` works this way. Adapter consumers can branch on `ad.requiresCustomRendering` to decide which surface to show:

```kotlin
if (ad.requiresCustomRendering) {
    GrowlAdView(ad)             // delegates to the renderer-aware surface
} else {
    GrowlBadgeAdView(ad)        // fine for Elo-direct fills
}
```

`GrowlBadgeAdView` and `GrowlChatAdView` are Elo-styled and **ignore** the renderer. Adapter authors must document which ad surfaces are safe for their network's creatives. For AdMob, only `GrowlAdView` is safe.

**Lifecycle:**

- `makeView` is called once per Compose mount (every host gets its own `View`). The SDK keys the embedding `AndroidView` on `GrowlAd.id`, so swapping ads in place produces a fresh `View` rather than reusing the old one.
- `update` runs on every recomposition with new ad data, starting with the first one immediately after `makeView`. Compose does **not** guarantee the view has been measured or laid out by then — for assets that need real bounds (e.g. AdMob's `MediaView`), defer binding inside `update` (`view.doOnLayout { … }`) or rely on the underlying SDK's own deferred binding.
- `release` runs when the embedding composable leaves composition or before the view is discarded on ad swap. This is where SDK-owned native ad cleanup belongs — for AdMob, call `nativeAd.destroy()` here. The default is a no-op for renderers without native cleanup needs.

---

## Packaging

Ship your adapter as a separate Gradle library so the SDK's release cadence isn't coupled to your network's SDK:

```kotlin
// my-network-adapter/build.gradle.kts
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.growl.adapter.mynetwork"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    compileOnly("ad.elo:elo-android-sdk:X.Y.Z")
    implementation("com.mynetwork:mynetwork-sdk:1.2.3")
}
```

Use `compileOnly` for the SDK so consumers control which version they're on; the adapter inherits whatever they pick. Bundle your network's SDK as a regular `implementation` dependency.

Publish to Maven Central or a private repository — your choice. Document the consumer-side wiring in your adapter's `README.md`:

```kotlin
Growl.configure(
    context,
    GrowlConfiguration(
        growl = GrowlNetworkConfiguration(publisherId = ..., adUnitId = ...),
        adapters = listOf(MyNetworkAdapter(appId = "...", placementId = "...")),
    ),
)
```

---

## Testing

Use the `ParallelAuctionMediator` test fixtures (under `GrowlAndroidSDK/src/test/.../mediation/`) as a reference for stubbing adapters in unit tests. The pattern is straightforward — implement `AdNetworkAdapter` as an anonymous object that returns canned `AdBid`s or throws specific `AdAdapterError`s, register it in a `GrowlConfiguration` for a test, and assert on what `Growl.loadAd` returns.

The first-party `GrowlNetworkAdapter` (`mediation/adapters/GrowlNetworkAdapter.kt`) is the canonical reference for a "real" adapter — it shows the start-then-bid pattern, the consent translation, the URL-ping `AdTracker` wiring, and the throw-vs-null discipline for soft and hard failures.

---

## Reference

- `mediation/AdNetworkAdapter.kt` — the contract
- `mediation/AdBidRequest.kt`, `AdBid.kt`, `AdAdapterError.kt`, `AdConsent.kt` — request/response/error types
- `mediation/tracking/AdTracker.kt` — telemetry contract third-party adapters implement
- `mediation/adapters/GrowlNetworkAdapter.kt` — first-party adapter as a reference implementation
- `mediation/ParallelAuctionMediator.kt` — auction orchestration; useful to understand timing, cancellation, and debug-event recording

If something here is wrong or unclear, file an issue or PR — adapter authors are the audience and we want this to read straight.
