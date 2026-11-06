package com.grinch.rivo4.controller.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {
    private const val TAG = "ConsentManager"

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: Exception?)
    }

    private fun isDebug(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Returns true if user consent has been obtained or if consent is not required
     * (e.g. outside EEA/UK/Switzerland).
     */
    fun canRequestAds(context: Context): Boolean {
        return try {
            UserMessagingPlatform.getConsentInformation(context).canRequestAds()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking canRequestAds: ${e.message}")
            false
        }
    }

    /**
     * Checks if the privacy options requirement status is REQUIRED, indicating that
     * a user-facing entry (e.g. in Settings) must be shown to let users update their consent.
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return try {
            UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Requests consent information update and presents the consent form if required by IAB TCF.
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (isDebug(activity)) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .addTestDeviceHashedId("372B822B004FEC61757C53C2F881C02C")
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val params = paramsBuilder.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form dismiss/error: [${formError.errorCode}] ${formError.message}")
                    }
                    onConsentGatheringCompleteListener.consentGatheringComplete(
                        if (formError != null) Exception(formError.message) else null
                    )
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent info update error: [${requestConsentError.errorCode}] ${requestConsentError.message}")
                onConsentGatheringCompleteListener.consentGatheringComplete(
                    Exception(requestConsentError.message)
                )
            }
        )
    }

    /**
     * Displays the Privacy Options form so the user can modify their previously granted consent choices.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismissListener: (Exception?) -> Unit = {}
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: [${formError.errorCode}] ${formError.message}")
            }
            onDismissListener(if (formError != null) Exception(formError.message) else null)
        }
    }
}
