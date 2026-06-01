package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)

    private val _settingsChanged = MutableStateFlow(0)
    val settingsChanged: StateFlow<Int> = _settingsChanged.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settingsChanged.value += 1
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    fun setString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    companion object {
        const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        const val KEY_AMOLED_MODE = "amoled_mode"
        const val KEY_SHOW_FIRST_LETTER = "show_first_letter"
        const val KEY_COLORFUL_AVATARS = "colorful_avatars"
        const val KEY_SHOW_PICTURE = "show_picture"
        const val KEY_ICON_ONLY_NAV = "icon_only_nav"
        const val KEY_FLIP_BOTTOM_NAV = "flip_bottom_nav"
        const val KEY_DEFAULT_BOTTOM_NAV = "default_bottom_nav"
        const val KEY_DTMF_TONE = "dtmf_tone"
        const val KEY_DIALPAD_VIBRATION = "dialpad_vibration"
        const val KEY_SPEED_DIAL = "speed_dial"
        const val KEY_T9_DIALING = "t9_dialing"
        const val KEY_PROXIMITY_SENSOR = "proximity_sensor"
        const val KEY_INCOMING_CALL_POPUP = "incoming_call_popup"
        const val KEY_AUTO_REDIAL_BUSY = "auto_redial_busy"
        const val KEY_REDIAL_ATTEMPTS = "redial_attempts"
        const val KEY_REDIAL_DELAY = "redial_delay"
        const val KEY_BLOCK_METHOD = "block_method" // 0: Decline, 1: Silent
        const val KEY_BLOCK_LOG_VISIBILITY = "block_log_visibility" // 0: Hide, 1: Show
        const val KEY_BLOCK_NOTIFICATION = "block_notification"
        const val KEY_VIBRATE_ON_ANSWER = "vibrate_on_answer"
        const val KEY_VIBRATE_ON_HANGUP = "vibrate_on_hangup"
        const val KEY_ROUND_AVATARS = "round_avatars"
        const val KEY_SHOW_DIVIDERS = "show_dividers"
        const val KEY_TRANSITION_STYLE = "transition_animation_style" // 0: Standard, 1: Slide, 2: Fade, 3: None
        const val KEY_APP_ICON = "app_icon_style" // 0: Default, 1: Dark, 2: Premium
        const val KEY_UI_DENSITY = "ui_density" // 0: Comfort, 1: Compact
        const val KEY_DIALPAD_STYLE = "dialpad_style" // 0: Modern, 1: Classic, 2: Minimal
        const val KEY_CALL_BACKGROUND = "call_background_style" // 0: Expressive, 1: Solid, 2: Contact Photo
        const val KEY_VOICEMAIL_NUMBER = "voicemail_number"
        const val KEY_VOICEMAIL_VIBRATION = "voicemail_vibration"
        const val KEY_VOICEMAIL_RINGTONE = "voicemail_ringtone"
    }
}
