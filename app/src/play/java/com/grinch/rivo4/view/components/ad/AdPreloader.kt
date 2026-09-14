package com.grinch.rivo4.view.components.ad

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

object AdPreloader {
    private const val TAG = "AdPreloader"

    private var isInitialized = false

    @Volatile
    var cachedPostCallAd: NativeAd? = null
        private set

    @Volatile
    var cachedBannerAd: NativeAd? = null
        private set

    @Volatile
    private var isLoadingPostCall = false

    @Volatile
    private var isLoadingBanner = false

    fun init(context: Context) {
        if (!isInitialized) {
            isInitialized = true
            val appContext = context.applicationContext
            try {
                MobileAds.initialize(appContext) {
                    preloadPostCallAd(appContext)
                    preloadBannerAd(appContext)
                }
            } catch (e: Exception) {
                Log.w(TAG, "MobileAds initialization error: ${e.message}")
            }
        }
    }

    private fun isDebug(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun preloadPostCallAd(context: Context) {
        if (cachedPostCallAd != null || isLoadingPostCall) return
        isLoadingPostCall = true

        val appContext = context.applicationContext
        val adUnitId = if (isDebug(appContext)) TEST_NATIVE_AD_UNIT_ID else NATIVE_AD_UNIT_ID

        try {
            val adLoader = AdLoader.Builder(appContext, adUnitId)
                .forNativeAd { ad: NativeAd ->
                    val old = cachedPostCallAd
                    cachedPostCallAd = ad
                    isLoadingPostCall = false
                    old?.destroy()
                    Log.d(TAG, "PostCall native ad preloaded successfully")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingPostCall = false
                        Log.w(TAG, "Failed to preload PostCall ad: ${error.code} - ${error.message}")
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
            Log.w(TAG, "Error preloading PostCall ad: ${e.message}")
        }
    }

    fun preloadBannerAd(context: Context) {
        if (cachedBannerAd != null || isLoadingBanner) return
        isLoadingBanner = true

        val appContext = context.applicationContext
        val adUnitId = if (isDebug(appContext)) TEST_NATIVE_BANNER_AD_UNIT_ID else NATIVE_BANNER_AD_UNIT_ID

        try {
            val adLoader = AdLoader.Builder(appContext, adUnitId)
                .forNativeAd { ad: NativeAd ->
                    val old = cachedBannerAd
                    cachedBannerAd = ad
                    isLoadingBanner = false
                    old?.destroy()
                    Log.d(TAG, "Banner native ad preloaded successfully")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingBanner = false
                        Log.w(TAG, "Failed to preload Banner ad: ${error.code} - ${error.message}")
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
            Log.w(TAG, "Error preloading Banner ad: ${e.message}")
        }
    }

    fun consumePostCallAd(context: Context): NativeAd? {
        val ad = cachedPostCallAd
        cachedPostCallAd = null
        preloadPostCallAd(context)
        return ad
    }

    fun consumeBannerAd(context: Context): NativeAd? {
        val ad = cachedBannerAd
        cachedBannerAd = null
        preloadBannerAd(context)
        return ad
    }
}
