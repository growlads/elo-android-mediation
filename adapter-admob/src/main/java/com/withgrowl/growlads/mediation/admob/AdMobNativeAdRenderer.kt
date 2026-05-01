package com.withgrowl.growlads.mediation.admob

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.withgrowl.growlandroidsdk.mediation.tracking.AdRenderer

/**
 * AdMob's billing requires `NativeAd` assets to be displayed inside a
 * [NativeAdView] subtree with each asset View registered via
 * `setHeadlineView()`, `setBodyView()`, `setIconView()`, and
 * `setCallToActionView()`. This renderer builds that subtree and registers
 * the `NativeAd` against it.
 *
 * Embedded by `GrowlRenderedAdView` via `AndroidView { renderer.makeView(it) }`.
 *
 * `release` calls `NativeAd.destroy()` — JVM has no automatic cleanup hook
 * for the native pointer Google Mobile Ads SDK holds, so the renderer must
 * release it when the embedding composable leaves composition.
 */
public class AdMobNativeAdRenderer(
    private val nativeAd: NativeAd,
) : AdRenderer {

    override val minimumDisplayHeightDp: Int = 96

    override fun makeView(context: Context): View {
        val nativeAdView = NativeAdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val iconSizePx = (48 * context.resources.displayMetrics.density).toInt()
        val icon = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx)
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val headline = TextView(context).apply { id = View.generateViewId() }
        val body = TextView(context).apply { id = View.generateViewId() }
        textColumn.addView(headline)
        textColumn.addView(body)

        val cta = TextView(context).apply { id = View.generateViewId() }

        container.addView(icon)
        container.addView(textColumn)
        container.addView(cta)
        nativeAdView.addView(container)

        nativeAdView.headlineView = headline
        nativeAdView.bodyView = body
        nativeAdView.iconView = icon
        nativeAdView.callToActionView = cta

        return nativeAdView
    }

    override fun update(view: View) {
        val nativeAdView = view as? NativeAdView ?: return
        (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline
        (nativeAdView.bodyView as? TextView)?.text = nativeAd.body
        (nativeAdView.callToActionView as? TextView)?.text = nativeAd.callToAction
        nativeAd.icon?.drawable?.let { drawable ->
            (nativeAdView.iconView as? ImageView)?.setImageDrawable(drawable)
        }
        nativeAdView.setNativeAd(nativeAd)
    }

    override fun release(view: View) {
        // Google Mobile Ads SDK holds a native pointer for each NativeAd.
        // Without destroy() those pointers leak across ad swaps and screen
        // recreations.
        nativeAd.destroy()
    }
}
