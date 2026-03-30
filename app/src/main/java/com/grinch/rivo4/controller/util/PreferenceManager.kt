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
        const val KEY_DTMF_TONE = "dtmf_tone"
        const val KEY_DIALPAD_VIBRATION = "dialpad_vibration"
        const val KEY_SPEED_DIAL = "speed_dial"
        const val KEY_T9_DIALING = "t9_dialing"
    }
}
