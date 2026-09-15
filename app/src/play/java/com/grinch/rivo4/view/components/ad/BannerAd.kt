package com.grinch.rivo4.view.components.ad

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject

private const val TAG = "BannerAd"
const val NATIVE_BANNER_AD_UNIT_ID = "ca-app-pub-7333874264565957/6821493941"
const val TEST_NATIVE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

val IS_ADS_SUPPORTED = true

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NATIVE_BANNER_AD_UNIT_ID
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val adsEnabled = remember(settingsState) {
        prefs.getBoolean(PreferenceManager.KEY_ENABLE_ADS, true) && !prefs.isSupporter()
    }

    if (!adsEnabled) return

    val context = LocalContext.current
    val isDebug = remember {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val effectiveAdUnitId = if (isDebug) TEST_NATIVE_BANNER_AD_UNIT_ID else adUnitId

    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(AdPreloader.consumeBannerAd(context)) }
    var isFailedToLoad by remember { mutableStateOf(false) }

    DisposableEffect(effectiveAdUnitId) {
        if (loadedNativeAd == null) {
            AdPreloader.init(context)

            val adLoader = AdLoader.Builder(context, effectiveAdUnitId)
                .forNativeAd { ad: NativeAd ->
                    loadedNativeAd = ad
                    isFailedToLoad = false
                    Log.d(TAG, "Native banner loaded successfully ($effectiveAdUnitId)")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isFailedToLoad = true
                        Log.w(TAG, "Native banner failed to load: ${loadAdError.code} - ${loadAdError.message}")
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }

        onDispose {
            loadedNativeAd?.destroy()
        }
    }

    val nativeAd = loadedNativeAd
    if (nativeAd != null && !isFailedToLoad) {
        val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                factory = { ctx ->
                    createNativeBannerView(
                        context = ctx,
                        primaryColor = primaryColor,
                        onPrimaryColor = onPrimaryColor,
                        onSurfaceColor = onSurfaceColor,
                        onSurfaceVariant = onSurfaceVariant
                    )
                },
                update = { view ->
                    populateNativeBannerView(view, nativeAd)
                }
            )
        }
    }
}

private fun createNativeBannerView(
    context: Context,
    primaryColor: Int,
    onPrimaryColor: Int,
    onSurfaceColor: Int,
    onSurfaceVariant: Int
): NativeAdView {
    val dp = context.resources.displayMetrics.density
    val nativeAdView = NativeAdView(context)

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Ad Icon
    val iconView = ImageView(context).apply {
        id = View.generateViewId()
        val sizePx = (44 * dp).toInt()
        layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
            marginEnd = (10 * dp).toInt()
        }
        scaleType = ImageView.ScaleType.FIT_CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10 * dp
        }
        clipToOutline = true
    }
    nativeAdView.iconView = iconView
    root.addView(iconView)

    // Center Text column: [AD badge + Advertiser] & Headline & Body
    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = (8 * dp).toInt()
        }
    }

    // Top metadata row: [AD] badge + Advertiser name
    val metaRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (2 * dp).toInt()
        }
    }

    val adBadge = TextView(context).apply {
        text = "AD"
        textSize = 8.5f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceVariant)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4 * dp
            setColor((onSurfaceVariant and 0x00FFFFFF) or 0x22000000)
        }
        setPadding((4 * dp).toInt(), (1 * dp).toInt(), (4 * dp).toInt(), (1 * dp).toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = (5 * dp).toInt()
        }
    }
    metaRow.addView(adBadge)

    val advertiserView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 10.5f
        setTextColor(onSurfaceVariant)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    nativeAdView.advertiserView = advertiserView
    metaRow.addView(advertiserView)

    textCol.addView(metaRow)

    val headlineView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 13.5f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceColor)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    nativeAdView.headlineView = headlineView
    textCol.addView(headlineView)

    val bodyView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 11.5f
        setTextColor(onSurfaceVariant)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    nativeAdView.bodyView = bodyView
    textCol.addView(bodyView)

    root.addView(textCol)

    // Call to Action Pill Button
    val ctaButton = Button(context).apply {
        id = View.generateViewId()
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onPrimaryColor)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * dp
            setColor(primaryColor)
        }
        setPadding((12 * dp).toInt(), (4 * dp).toInt(), (12 * dp).toInt(), (4 * dp).toInt())
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            (36 * dp).toInt()
        )
    }
    nativeAdView.callToActionView = ctaButton
    root.addView(ctaButton)

    nativeAdView.addView(root)
    return nativeAdView
}

private fun populateNativeBannerView(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

    val advertiserView = nativeAdView.advertiserView as? TextView
    if (nativeAd.advertiser != null) {
        advertiserView?.text = nativeAd.advertiser
        advertiserView?.visibility = View.VISIBLE
    } else if (nativeAd.store != null) {
        advertiserView?.text = nativeAd.store
        advertiserView?.visibility = View.VISIBLE
    } else {
        advertiserView?.visibility = View.GONE
    }

    val bodyView = nativeAdView.bodyView as? TextView
    if (nativeAd.body != null) {
        bodyView?.text = nativeAd.body
        bodyView?.visibility = View.VISIBLE
    } else {
        bodyView?.visibility = View.GONE
    }

    val iconView = nativeAdView.iconView as? ImageView
    if (nativeAd.icon != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    val ctaButton = nativeAdView.callToActionView as? Button
    if (nativeAd.callToAction != null) {
        ctaButton?.text = nativeAd.callToAction
        ctaButton?.visibility = View.VISIBLE
    } else {
        ctaButton?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}

