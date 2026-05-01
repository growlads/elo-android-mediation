# Changelog

## 0.0.2 — 2026-05-01

- Redesign `AdMobNativeAdRenderer` card chrome: rounded surface, padded
  layout, Sponsored attribution badge, larger icon (56dp), prominent
  rounded CTA button, Material You-aligned defaults.
- Honor AdMob's `callToAction` casing instead of inheriting host theme's
  textAllCaps (overrides `transformationMethod`).
- Hide the CTA view when AdMob's `callToAction` is null/blank instead of
  substituting a fallback label, to keep creative integrity intact and
  align with AdMob native-ad rendering policy.
- New `sponsoredLabel` constructor parameter on `AdMobNativeAdRenderer`
  (default `"Sponsored"`) so consumers can localize the attribution chip
  without subclassing.

## 0.0.1 — 2026-05-01

- Initial AdMob adapter release. Native ad format only. Sequential
  price-tier waterfall. Compatible with `ad.elo:elo-android-sdk:2.3.0+`.
