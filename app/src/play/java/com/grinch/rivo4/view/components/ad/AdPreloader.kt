package com.grinch.rivo4.view.components.ad

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val TEST_DEVICE_IDS = listOf(
    "372B822B004FEC61757C53C2F881C02C"
)

object AdPreloader {
    private const val TAG = "AdPreloader"

    // Enforce cooldowns to protect match rate and avoid spamming AdMob with rapid requests
    const val BANNER_MIN_REFRESH_INTERVAL_MS = 60_000L // 60 seconds minimum between banner loads
    const val POST_CALL_MIN_REFRESH_INTERVAL_MS = 30_000L // 30 seconds minimum between post-call loads

    private var isInitialized = false

    private val _bannerAdState = MutableStateFlow<NativeAd?>(null)
    val bannerAdState: StateFlow<NativeAd?> = _bannerAdState.asStateFlow()

    private val _postCallAdState = MutableStateFlow<NativeAd?>(null)
    val postCallAdState: StateFlow<NativeAd?> = _postCallAdState.asStateFlow()

    @Volatile
    private var lastBannerLoadTimeMs: Long = 0L

    @Volatile
    private var lastPostCallLoadTimeMs: Long = 0L

    @Volatile
    private var isLoadingBanner = false

    @Volatile
    private var isLoadingPostCall = false

    fun init(context: Context) {
        if (!isInitialized) {
            isInitialized = true
            val appContext = context.applicationContext
            try {
                val requestConfiguration = RequestConfiguration.Builder()
                    .setTestDeviceIds(TEST_DEVICE_IDS)
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)

                MobileAds.initialize(appContext) {
                    preloadBannerAd(appContext)
                    preloadPostCallAd(appContext)
                }
            } catch (e: Exception) {
                Log.w(TAG, "MobileAds initialization error: ${e.message}")
            }
        }
    }

    private fun isDebug(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun requestBannerAd(context: Context, forceRefresh: Boolean = false) {
        val appContext = context.applicationContext
        init(appContext)

        val now = System.currentTimeMillis()
        val hasValidAd = _bannerAdState.value != null
        val timeSinceLast = now - lastBannerLoadTimeMs

        // Reuse cached ad if still within cooldown interval
        if (!forceRefresh && hasValidAd && timeSinceLast < BANNER_MIN_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "Reusing existing cached banner ad (age: ${timeSinceLast / 1000}s)")
            return
        }

        if (isLoadingBanner) {
            Log.d(TAG, "Banner ad load already in flight, skipping duplicate request")
            return
        }

        isLoadingBanner = true
        val adUnitId = if (isDebug(appContext)) TEST_NATIVE_BANNER_AD_UNIT_ID else NATIVE_BANNER_AD_UNIT_ID

        try {
            val adLoader = AdLoader.Builder(appContext, adUnitId)
                .forNativeAd { ad: NativeAd ->
                    val oldAd = _bannerAdState.value
                    _bannerAdState.value = ad
                    lastBannerLoadTimeMs = System.currentTimeMillis()
                    isLoadingBanner = false
                    oldAd?.destroy()
                    Log.d(TAG, "Native banner ad loaded and cached successfully ($adUnitId)")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingBanner = false
                        Log.w(TAG, "Failed to load banner ad: ${error.code} - ${error.message}")
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            isLoadingBanner = false
            Log.w(TAG, "Error loading banner ad: ${e.message}")
        }
    }

    fun preloadBannerAd(context: Context) {
        requestBannerAd(context, forceRefresh = false)
    }

    fun requestPostCallAd(context: Context, forceRefresh: Boolean = false) {
        val appContext = context.applicationContext
        init(appContext)

        val now = System.currentTimeMillis()
        val hasValidAd = _postCallAdState.value != null
        val timeSinceLast = now - lastPostCallLoadTimeMs

        // Reuse cached ad if still within cooldown interval
        if (!forceRefresh && hasValidAd && timeSinceLast < POST_CALL_MIN_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "Reusing existing cached post-call ad (age: ${timeSinceLast / 1000}s)")
            return
        }

        if (isLoadingPostCall) {
            Log.d(TAG, "PostCall ad load already in flight, skipping duplicate request")
            return
        }

        isLoadingPostCall = true
        val adUnitId = if (isDebug(appContext)) TEST_NATIVE_AD_UNIT_ID else NATIVE_AD_UNIT_ID

        try {
            val adLoader = AdLoader.Builder(appContext, adUnitId)
                .forNativeAd { ad: NativeAd ->
                    val oldAd = _postCallAdState.value
                    _postCallAdState.value = ad
                    lastPostCallLoadTimeMs = System.currentTimeMillis()
                    isLoadingPostCall = false
                    oldAd?.destroy()
                    Log.d(TAG, "PostCall native ad loaded successfully ($adUnitId)")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingPostCall = false
                        Log.w(TAG, "Failed to load PostCall ad: ${error.code} - ${error.message}")
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
        } catch (e: Exception) {
            isLoadingPostCall = false
            Log.w(TAG, "Error loading PostCall ad: ${e.message}")
        }
    }

    fun preloadPostCallAd(context: Context) {
        requestPostCallAd(context, forceRefresh = false)
    }

    fun consumeBannerAd(context: Context): NativeAd? {
        requestBannerAd(context)
        return _bannerAdState.value
    }

    fun consumePostCallAd(context: Context): NativeAd? {
        requestPostCallAd(context)
        return _postCallAdState.value
    }
}
