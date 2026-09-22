package com.grinch.rivo4.view.components.ad

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdPreloader {
    private val _bannerAdState = MutableStateFlow<Any?>(null)
    val bannerAdState: StateFlow<Any?> = _bannerAdState.asStateFlow()

    private val _postCallAdState = MutableStateFlow<Any?>(null)
    val postCallAdState: StateFlow<Any?> = _postCallAdState.asStateFlow()

    fun init(context: Context) {}
    fun preloadPostCallAd(context: Context) {}
    fun preloadBannerAd(context: Context) {}
    fun requestBannerAd(context: Context, forceRefresh: Boolean = false) {}
    fun requestPostCallAd(context: Context, forceRefresh: Boolean = false) {}
    fun consumeBannerAd(context: Context): Any? = null
    fun consumePostCallAd(context: Context): Any? = null
}
