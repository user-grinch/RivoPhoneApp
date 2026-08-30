package com.grinch.rivo4.view.components.ad

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject

private var isMobileAdsInitialized = false

private fun initializeMobileAds(context: Context) {
    if (!isMobileAdsInitialized) {
        MobileAds.initialize(context) {}
        isMobileAdsInitialized = true
    }
}

val IS_ADS_SUPPORTED = true

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-7333874264565957/3922960331"
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val adsEnabled = remember(settingsState) {
        prefs.getBoolean(PreferenceManager.KEY_ENABLE_ADS, true)
    }

    if (!adsEnabled) return

    val context = LocalContext.current
    val isDebug = remember {
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val effectiveAdUnitId = if (isDebug) "ca-app-pub-3940256099942544/6300978111" else adUnitId

    var isAdLoaded by remember { mutableStateOf(false) }

    val adView = remember(effectiveAdUnitId) {
        initializeMobileAds(context.applicationContext)
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = effectiveAdUnitId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                    Log.d("BannerAd", "Ad loaded successfully ($effectiveAdUnitId)")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoaded = false
                    Log.w(
                        "BannerAd",
                        "Ad failed to load: code=${loadAdError.code}, message=${loadAdError.message}"
                    )
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            (adView.parent as? ViewGroup)?.removeView(adView)
            adView.destroy()
        }
    }

    if (isAdLoaded) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    factory = {
                        (adView.parent as? ViewGroup)?.removeView(adView)
                        adView
                    }
                )
            }
        }
    }
}

