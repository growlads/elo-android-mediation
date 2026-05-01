# Elo Android Mediation Adapters

First-party mediation adapters for the [Elo Android SDK](https://github.com/growlads/elo-android-sdk).

Each adapter wraps a third-party ad network SDK and conforms to the
`AdNetworkAdapter` contract from `ad.elo:elo-android-sdk`. Adapters
participate in Growl's parallel first-price auction.

## Available adapters

| Adapter | Module | Maven coord | Network | README |
| --- | --- | --- | --- | --- |
| AdMob | `:adapter-admob` | `ad.elo:elo-android-mediation-admob` | Google Mobile Ads 23.x | [adapter-admob/README.md](adapter-admob/README.md) |

## Integration

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts
dependencies {
    implementation("ad.elo:elo-android-sdk:2.3.0")
    implementation("ad.elo:elo-android-mediation-admob:0.0.1")
}
```

Then register the adapter at app startup. See the per-adapter README for
construction parameters.

## Compatibility matrix

| `elo-android-mediation` | `elo-android-mediation-admob` runtime | `play-services-ads` | `elo-android-sdk` |
| --- | --- | --- | --- |
| 0.0.1 | 0.0.1 | 23.6.0+ | 2.3.0+ |

The runtime adapter version is exposed at runtime as
`AdMobAdapter.VERSION`. Bumped per-adapter on each behavioral change,
independent of the repo tag.

## Authoring a new adapter

See [ADAPTER_AUTHOR_GUIDE.md](ADAPTER_AUTHOR_GUIDE.md).

## Local development

Building this repo against an unreleased SDK change:

```sh
# In elo-android-sdk-source:
./gradlew :GrowlAndroidSDK:publishToMavenLocal

# Then build this repo (settings.gradle.kts already includes mavenLocal()):
./gradlew :adapter-admob:assembleDebug
```

## License

MIT. See [LICENSE](LICENSE).
