package com.grinch.rivo4.controller.lock

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.grinch.rivo4.controller.util.PreferenceManager

object AppLockManager {
    private const val TAG = "AppLockManager"

    var isUnlocked: Boolean = false
        private set

    private var lastBackgroundTime: Long = 0L

    fun isLocked(prefs: PreferenceManager): Boolean {
        if (!prefs.isAppLockEnabled()) return false
        if (!isUnlocked) return true

        if (lastBackgroundTime > 0) {
            val timeoutMinutes = prefs.getAppLockTimeout()
            val elapsedMillis = System.currentTimeMillis() - lastBackgroundTime
            val timeoutMillis = timeoutMinutes * 60 * 1000L
            if (elapsedMillis > timeoutMillis) {
                isUnlocked = false
                return true
            }
        }
        return false
    }

    fun onAppBackgrounded() {
        lastBackgroundTime = System.currentTimeMillis()
    }

    fun onAppForegrounded(prefs: PreferenceManager) {
        if (isLocked(prefs)) {
            isUnlocked = false
        }
    }

    fun unlock() {
        isUnlocked = true
        lastBackgroundTime = 0L
    }

    fun lock() {
        isUnlocked = false
        lastBackgroundTime = 0L
    }

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = getAuthenticators()
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun getAuthenticators(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Rivo Phone",
        subtitle: String = "Use your Face, Fingerprint, PIN, or Password",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                unlock()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d(TAG, "Authentication error ($errorCode): $errString")
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "Authentication failed")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(getAuthenticators())
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "BiometricPrompt authentication failed to launch: ${e.message}", e)
            onError(e.message ?: "Authentication error")
        }
    }
}
