package com.grinch.rivo4.controller.ads

import android.app.Activity
import android.content.Context

object ConsentManager {
    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: Exception?)
    }

    fun canRequestAds(context: Context): Boolean = false
    fun isPrivacyOptionsRequired(context: Context): Boolean = false

    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        onConsentGatheringCompleteListener.consentGatheringComplete(null)
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismissListener: (Exception?) -> Unit = {}
    ) {
        onDismissListener(null)
    }
}
