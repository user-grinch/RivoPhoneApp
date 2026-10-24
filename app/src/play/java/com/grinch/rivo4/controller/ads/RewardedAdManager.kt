package com.grinch.rivo4.controller.ads

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.grinch.rivo4.controller.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class RewardedAdManager(private val context: Context) {
    private val preferences = PreferenceManager(context)

    companion object {
        private const val TAG = "RewardedAdManager"

        // Test ad unit ID for development
        // Replace with your actual ad unit ID for production
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-7333874264565957/6821493941"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rewardedAd: RewardedAd? = null
    private var adsInitializationRequested = false
    private var adsInitialized = false
    private var pendingActivity: WeakReference<Activity>? = null
    private var _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _adLoadError = MutableSharedFlow<String>()
    val adLoadError: SharedFlow<String> = _adLoadError.asSharedFlow()

    private val _adWatched = MutableSharedFlow<Unit>()
    val adWatched: SharedFlow<Unit> = _adWatched.asSharedFlow()

    private val _adFailedToShow = MutableSharedFlow<String>()
    val adFailedToShow: SharedFlow<String> = _adFailedToShow.asSharedFlow()

    private val _adDismissed = MutableSharedFlow<Unit>()
    val adDismissed: SharedFlow<Unit> = _adDismissed.asSharedFlow()

    init {
        initializeAds()
    }

    private fun initializeAds() {
        if (adsInitializationRequested) return
        adsInitializationRequested = true
        MobileAds.initialize(context) {
            adsInitialized = true
            loadAd()
        }
    }

    fun loadAd() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            scope.launch { loadAd() }
            return
        }

        if (!adsInitialized) {
            initializeAds()
            return
        }
        if (_isLoading.value || _isAdReady.value) return

        _isLoading.value = true

        val adUnitId = if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            TEST_REWARDED_AD_UNIT_ID
        } else {
            REWARDED_AD_UNIT_ID
        }
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    _isLoading.value = false
                    _isAdReady.value = true
                    pendingActivity?.get()?.takeUnless { it.isFinishing || it.isDestroyed }?.let { activity ->
                        pendingActivity = null
                        showAd(activity) {}
                    }
                    pendingActivity = null

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Rewarded ad dismissed")
                            _adDismissed.tryEmit(Unit)
                            rewardedAd = null
                            _isAdReady.value = false
                            // Preload next ad
                            loadAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                            _adFailedToShow.tryEmit(adError.message ?: "Unknown error")
                            rewardedAd = null
                            _isAdReady.value = false
                            loadAd()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Rewarded ad showed")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    _isLoading.value = false
                    _isAdReady.value = false
                    _adLoadError.tryEmit(loadAdError.message ?: "Failed to load ad")

                    // Retry after a delay
                    scope.launch {
                        kotlinx.coroutines.delay(30000) // 30 seconds
                        loadAd()
                    }
                }
            }
        )
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            scope.launch { showAd(activity, onRewardEarned) }
            return
        }

        if (!_isAdReady.value || rewardedAd == null) {
            Log.w(TAG, "Rewarded ad not ready")
            if (activity.isFinishing || activity.isDestroyed) return
            pendingActivity = WeakReference(activity)
            loadAd()
            return
        }

        rewardedAd?.show(activity) { rewardItem: RewardItem ->
            Log.d(TAG, "User completed rewarded ad")
            preferences.grantAdSupportedPro()
            preferences.setProStatus(true)
            _adWatched.tryEmit(Unit)
            onRewardEarned()
        }
    }

    fun isReady(): Boolean = _isAdReady.value
}
