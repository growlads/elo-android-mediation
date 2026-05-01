# Changelog

## 0.0.2 — 2026-05-01

- Redesign `AdMobNativeAdRenderer` card chrome: rounded surface, padded
  layout, Sponsored attribution badge, larger icon (56dp), prominent
  rounded CTA button, Material You-aligned defaults.
- Honor AdMob's `callToAction` casing instead of inheriting host theme's
  textAllCaps (overrides `transformationMethod`).

## 0.0.1 — 2026-05-01

- Initial AdMob adapter release. Native ad format only. Sequential
  price-tier waterfall. Compatible with `ad.elo:elo-android-sdk:2.3.0+`.
