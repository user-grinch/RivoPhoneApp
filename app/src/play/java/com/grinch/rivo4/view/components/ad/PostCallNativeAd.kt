package com.grinch.rivo4.view.components.ad

import android.content.Context
import android.graphics.Typeface
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
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
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

    LaunchedEffect(Unit) {
        AdPreloader.requestPostCallAd(context)
    }

    val nativeAd by AdPreloader.postCallAdState.collectAsState()

    nativeAd?.let { currentAd ->
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
                        populateNativeAdView(view, currentAd)
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

    // Top Row: [AD badge + Advertiser]
    val topMetaRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (6 * dp).toInt()
        }
    }

    val adBadge = TextView(context).apply {
        text = "Sponsored"
        textSize = 9.5f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceVariant)
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 5 * dp
            setColor((onSurfaceVariant and 0x00FFFFFF) or 0x20000000)
        }
        setPadding((5 * dp).toInt(), (2 * dp).toInt(), (5 * dp).toInt(), (2 * dp).toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = (6 * dp).toInt()
        }
    }
    topMetaRow.addView(adBadge)

    val advertiserView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 11.5f
        setTextColor(onSurfaceVariant)
        maxLines = 1
    }
    nativeAdView.advertiserView = advertiserView
    topMetaRow.addView(advertiserView)

    root.addView(topMetaRow)

    // Middle Content Row: Icon + (Headline & Body)
    val contentRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = (8 * dp).toInt()
        }
    }

    val iconView = ImageView(context).apply {
        id = View.generateViewId()
        val sizePx = (46 * dp).toInt()
        layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
            marginEnd = (12 * dp).toInt()
        }
        scaleType = ImageView.ScaleType.FIT_CENTER
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 12 * dp
        }
        clipToOutline = true
    }
    nativeAdView.iconView = iconView
    contentRow.addView(iconView)

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val headlineView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceColor)
        maxLines = 1
    }
    nativeAdView.headlineView = headlineView
    textCol.addView(headlineView)

    val bodyView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 12f
        setTextColor(onSurfaceVariant)
        maxLines = 2
    }
    nativeAdView.bodyView = bodyView
    textCol.addView(bodyView)

    contentRow.addView(textCol)
    root.addView(contentRow)

    // Media View (Optional, compact)
    val mediaView = MediaView(context).apply {
        id = View.generateViewId()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (120 * dp).toInt()
        ).apply {
            bottomMargin = (8 * dp).toInt()
        }
        clipToOutline = true
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 12 * dp
        }
    }
    nativeAdView.mediaView = mediaView
    root.addView(mediaView)

    // Call to Action Button (Full width expressive button)
    val ctaButton = Button(context).apply {
        id = View.generateViewId()
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        setTextColor(onPrimaryColor)
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16 * dp
            setColor(primaryColor)
        }
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (40 * dp).toInt()
        )
    }
    nativeAdView.callToActionView = ctaButton
    root.addView(ctaButton)

    nativeAdView.addView(root)
    return nativeAdView
}

private fun populateNativeAdView(nativeAdView: NativeAdView, nativeAd: NativeAd) {
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

    val mediaView = nativeAdView.mediaView as? MediaView
    if (nativeAd.mediaContent != null && nativeAd.mediaContent?.hasVideoContent() == true) {
        mediaView?.mediaContent = nativeAd.mediaContent
        mediaView?.visibility = View.VISIBLE
    } else if (nativeAd.images.isNotEmpty()) {
        mediaView?.mediaContent = nativeAd.mediaContent
        mediaView?.visibility = View.VISIBLE
    } else {
        mediaView?.visibility = View.GONE
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
