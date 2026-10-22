package com.grinch.rivo4.controller.ads

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class RewardedAdManager(private val context: Context) {

    companion object {
        const val REWARDED_AD_UNIT_ID = ""
    }

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
        // No-op for FOSS build
    }

    fun loadAd() {
        // No-op for FOSS build
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        // No-op for FOSS build - ads not supported
    }

    fun isReady(): Boolean = false
}
