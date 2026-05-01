package com.withgrowl.growlads.mediation.admob

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
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
 * `setCallToActionView()`. This renderer builds that subtree, applies card
 * chrome that visually fits alongside Elo-direct ads, and registers the
 * `NativeAd` against it.
 *
 * Embedded by Elo's rendered-ad surface via `AndroidView { renderer.makeView(it) }`.
 *
 * @param nativeAd the AdMob `NativeAd` whose assets will be rendered.
 * @param sponsoredLabel the attribution label rendered above the creative.
 * Defaults to `"Sponsored"`; pass a localized string to render in another
 * language.
 *
 * `release` calls `NativeAd.destroy()` — JVM has no automatic cleanup hook
 * for the native pointer Google Mobile Ads SDK holds, so the renderer must
 * release it when the embedding composable leaves composition.
 */
public class AdMobNativeAdRenderer(
    private val nativeAd: NativeAd,
    private val sponsoredLabel: String = "Sponsored",
) : AdRenderer {

    override val minimumDisplayHeightDp: Int = 96

    override fun makeView(context: Context): View {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val nativeAdView = NativeAdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(resolveSurfaceColor(context))
                setStroke(dp(1), resolveOutlineColor(context))
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val outer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        // Sponsored attribution chip — small uppercase label that always
        // identifies advertising content. Sits above the creative on its
        // own line so it never gets clipped by the AdChoices badge AdMob
        // places top-right of the NativeAdView.
        val sponsored = TextView(context).apply {
            text = sponsoredLabel
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(resolveSecondaryTextColor(context))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAllCaps = true
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(8) }
        }
        outer.addView(sponsored)

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val iconBgRadius = dp(12).toFloat()
        val icon = ImageView(context).apply {
            id = View.generateViewId()
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = iconBgRadius
                setColor(resolveIconBackgroundColor(context))
            }
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                marginEnd = dp(12)
            }
        }
        row.addView(icon)

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            )
        }
        val headline = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(resolvePrimaryTextColor(context))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val body = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(resolveSecondaryTextColor(context))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(2) }
        }
        textColumn.addView(headline)
        textColumn.addView(body)
        row.addView(textColumn)

        val cta = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(resolveOnPrimaryColor(context))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            // Override host theme's textAllCaps — AdMob's callToAction text
            // is already title-cased ("Install", "Open"), so honor that.
            transformationMethod = null
            isAllCaps = false
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(resolvePrimaryColor(context))
            }
            setPadding(dp(16), dp(8), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { marginStart = dp(8) }
        }
        row.addView(cta)

        outer.addView(row)
        nativeAdView.addView(outer)

        nativeAdView.headlineView = headline
        nativeAdView.bodyView = body
        nativeAdView.iconView = icon
        nativeAdView.callToActionView = cta

        return nativeAdView
    }

    override fun update(view: View) {
        val nativeAdView = view as? NativeAdView ?: return
        (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline
        (nativeAdView.bodyView as? TextView)?.apply {
            text = nativeAd.body
            visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        (nativeAdView.callToActionView as? TextView)?.apply {
            // Don't fabricate a CTA when the creative omits one — substituting
            // a generic label ("Open", etc.) misrepresents the ad and can run
            // afoul of AdMob's native-ad rendering policy.
            val cta = nativeAd.callToAction?.takeIf { it.isNotBlank() }
            text = cta
            visibility = if (cta == null) View.GONE else View.VISIBLE
        }
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

    // Color palette: Material You-aligned light-theme defaults so the
    // card looks reasonable in any host without depending on Material
    // theme attrs at compile time. Theme integration (dark mode, custom
    // colorScheme overrides) is a follow-up — accept a `style` parameter
    // analogous to GrowlAdStyle on the next rev.

    private fun resolvePrimaryColor(ctx: Context): Int = 0xFF6750A4.toInt()
    private fun resolveOnPrimaryColor(ctx: Context): Int = Color.WHITE
    private fun resolveSurfaceColor(ctx: Context): Int = 0xFFFFFBFE.toInt()
    private fun resolveOutlineColor(ctx: Context): Int = 0xFFCAC4D0.toInt()
    private fun resolveIconBackgroundColor(ctx: Context): Int = 0xFFE7E0EC.toInt()
    private fun resolvePrimaryTextColor(ctx: Context): Int = 0xFF1C1B1F.toInt()
    private fun resolveSecondaryTextColor(ctx: Context): Int = 0xFF49454F.toInt()
}
