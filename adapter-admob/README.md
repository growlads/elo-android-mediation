# Elo Android Mediation — AdMob

AdMob adapter for the Elo Android SDK mediation auction.

Maven coord: `ad.elo:elo-android-mediation-admob`

## Setup

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ad.elo:elo-android-sdk:2.3.0")
    implementation("ad.elo:elo-android-mediation-admob:0.0.1")
}
```

`AndroidManifest.xml`:

```xml
<manifest>
    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>
    </application>
</manifest>
```

## Usage

```kotlin
import com.withgrowl.growlads.mediation.admob.AdMobNetworkAdapter
import com.withgrowl.growlads.mediation.admob.AdMobPriceTier

Growl.configure(
    context = this,
    configuration = GrowlConfiguration(
        growl = GrowlNetworkConfiguration(
            publisherId = "YOUR_GROWL_PUB",
            adUnitId = "YOUR_GROWL_AD_UNIT",
        ),
        adapters = listOf(
            AdMobNetworkAdapter(priceTiers = listOf(
                AdMobPriceTier(adUnitId = "ca-app-pub-.../high",  eCpm = 5.00),
                AdMobPriceTier(adUnitId = "ca-app-pub-.../mid",   eCpm = 2.00),
                AdMobPriceTier(adUnitId = "ca-app-pub-.../floor", eCpm = 0.50),
            )),
        ),
    ),
)
```

## Surface compatibility

AdMob native ads must be displayed via `GrowlAdView` only — that view embeds
the adapter's `NativeAdView` (and its registered asset slots) so AdMob's
billing counts the impression. `GrowlBadgeAdView` and `GrowlChatAdView` are
Growl-styled and reject adapter-rendered ads at runtime; branch on
`ad.requiresCustomRendering` if a screen mixes formats.

```kotlin
if (ad.requiresCustomRendering) {
    GrowlAdView(ad)            // AdMob fills land here
} else {
    GrowlBadgeAdView(ad)       // Growl-direct fills only
}
```

## eCPM model

AdMob does not surface a programmatic bid price for native ads. This
adapter loads `priceTiers` sequentially (highest eCPM first) and returns
the first fill as an `AdBid` with the matching tier's eCPM. Configure your
AdMob ad units at fixed eCPM floors and order them highest-first in the
adapter constructor.
