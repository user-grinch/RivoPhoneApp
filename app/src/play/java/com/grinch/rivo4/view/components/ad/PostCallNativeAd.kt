package com.grinch.rivo4.view.components.ad

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject

private const val TAG = "PostCallNativeAd"
const val NATIVE_AD_UNIT_ID = "ca-app-pub-7333874264565957/9568920091"
const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

@Composable
fun PostCallNativeAd(
    modifier: Modifier = Modifier,
    adUnitId: String = NATIVE_AD_UNIT_ID
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
    val effectiveAdUnitId = if (isDebug) TEST_NATIVE_AD_UNIT_ID else adUnitId

    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(AdPreloader.consumePostCallAd(context)) }
    var isFailedToLoad by remember { mutableStateOf(false) }

    DisposableEffect(effectiveAdUnitId) {
        if (loadedNativeAd == null) {
            AdPreloader.init(context)

            val adLoader = AdLoader.Builder(context, effectiveAdUnitId)
                .forNativeAd { ad: NativeAd ->
                    loadedNativeAd = ad
                    isFailedToLoad = false
                    Log.d(TAG, "Native ad loaded successfully ($effectiveAdUnitId)")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isFailedToLoad = true
                        Log.w(TAG, "Native ad failed to load: ${loadAdError.code} - ${loadAdError.message}")
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setVideoOptions(
                            VideoOptions.Builder()
                                .setStartMuted(true)
                                .build()
                        )
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
                .padding(vertical = 2.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    factory = { ctx ->
                        createExpressiveNativeAdView(
                            context = ctx,
                            primaryColor = primaryColor,
                            onPrimaryColor = onPrimaryColor,
                            onSurfaceColor = onSurfaceColor,
                            onSurfaceVariant = onSurfaceVariant
                        )
                    },
                    update = { view ->
                        populateNativeAdView(view, nativeAd)
                    }
                )
            }
        }
    }
}

private fun createExpressiveNativeAdView(
    context: Context,
    primaryColor: Int,
    onPrimaryColor: Int,
    onSurfaceColor: Int,
    onSurfaceVariant: Int
): NativeAdView {
    val dp = context.resources.displayMetrics.density
    val nativeAdView = NativeAdView(context)

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Header: Sponsor icon / name + "Ad" pill badge
    val topHeader = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (6 * dp).toInt()
        }
    }

    val iconView = ImageView(context).apply {
        id = View.generateViewId()
        val sizePx = (20 * dp).toInt()
        layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
            marginEnd = (6 * dp).toInt()
        }
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    nativeAdView.iconView = iconView
    topHeader.addView(iconView)

    val advertiserView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 11f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceVariant)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
    nativeAdView.advertiserView = advertiserView
    topHeader.addView(advertiserView)

    // "AD" badge pill
    val adBadge = TextView(context).apply {
        text = "AD"
        textSize = 8.5f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceVariant)
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 4 * dp
            setColor((onSurfaceVariant and 0x00FFFFFF) or 0x22000000)
        }
        setPadding((5 * dp).toInt(), (1 * dp).toInt(), (5 * dp).toInt(), (1 * dp).toInt())
    }
    topHeader.addView(adBadge)

    root.addView(topHeader)

    // Content: Horizontal split (Left: Headline, Body, CTA. Right: MediaView)
    val contentRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Left info column
    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val headlineView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceColor)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    nativeAdView.headlineView = headlineView
    textCol.addView(headlineView)

    val bodyView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 11.5f
        setTextColor(onSurfaceVariant)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, (2 * dp).toInt(), 0, 0)
    }
    nativeAdView.bodyView = bodyView
    textCol.addView(bodyView)

    // Call To Action button
    val ctaButton = Button(context).apply {
        id = View.generateViewId()
        val btnHeightPx = (36 * dp).toInt()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            btnHeightPx
        ).apply {
            topMargin = (8 * dp).toInt()
        }
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 18 * dp
            setColor(primaryColor)
        }
        setTextColor(onPrimaryColor)
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        isAllCaps = false
        elevation = 0f
        setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
    }
    nativeAdView.callToActionView = ctaButton
    textCol.addView(ctaButton)

    contentRow.addView(textCol)

    // Right: Media visual (AdMob requires >= 120dp x 120dp for video)
    val mediaView = MediaView(context).apply {
        id = View.generateViewId()
        val mediaWidthPx = (125 * dp).toInt()
        val mediaHeightPx = (122 * dp).toInt()
        layoutParams = LinearLayout.LayoutParams(mediaWidthPx, mediaHeightPx).apply {
            marginStart = (10 * dp).toInt()
            gravity = Gravity.CENTER_VERTICAL
        }
        minimumWidth = (120 * dp).toInt()
        minimumHeight = (120 * dp).toInt()
        outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 12 * dp)
            }
        }
        clipToOutline = true
    }
    nativeAdView.mediaView = mediaView
    contentRow.addView(mediaView)

    root.addView(contentRow)
    nativeAdView.addView(root)
    return nativeAdView
}

private fun populateNativeAdView(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    val headlineView = nativeAdView.headlineView as? TextView
    val bodyView = nativeAdView.bodyView as? TextView
    val iconView = nativeAdView.iconView as? ImageView
    val advertiserView = nativeAdView.advertiserView as? TextView
    val callToActionView = nativeAdView.callToActionView as? Button
    val mediaView = nativeAdView.mediaView as? MediaView

    headlineView?.text = nativeAd.headline
    headlineView?.visibility = if (nativeAd.headline.isNullOrEmpty()) View.GONE else View.VISIBLE

    bodyView?.text = nativeAd.body
    bodyView?.visibility = if (nativeAd.body.isNullOrEmpty()) View.GONE else View.VISIBLE

    val advertiserText = nativeAd.advertiser ?: nativeAd.store
    if (!advertiserText.isNullOrEmpty()) {
        advertiserView?.text = advertiserText
        advertiserView?.visibility = View.VISIBLE
    } else {
        advertiserView?.visibility = View.GONE
    }

    if (nativeAd.icon != null && nativeAd.icon?.drawable != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    if (!nativeAd.callToAction.isNullOrEmpty()) {
        callToActionView?.text = nativeAd.callToAction
        callToActionView?.visibility = View.VISIBLE
    } else {
        callToActionView?.visibility = View.GONE
    }

    if (nativeAd.mediaContent != null && (nativeAd.mediaContent?.hasVideoContent() == true || nativeAd.images.isNotEmpty())) {
        mediaView?.setMediaContent(nativeAd.mediaContent!!)
        mediaView?.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        mediaView?.visibility = View.VISIBLE
    } else {
        mediaView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}
