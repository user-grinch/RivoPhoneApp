package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = run {
        val deviceContext = context.createDeviceProtectedStorageContext()
        deviceContext.moveSharedPreferencesFrom(context, "rivo_prefs")
        deviceContext.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
    }

    private val _settingsChanged = MutableStateFlow(0)
    val settingsChanged: StateFlow<Int> = _settingsChanged.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settingsChanged.value += 1
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getString(key: String, defaultValue: String?): String? {
        return try {
            prefs.getString(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun setString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return try {
            prefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun setLastUsedNumber(contactId: String, number: String) {
        prefs.edit().putString("last_used_number_$contactId", number).apply()
    }

    fun getLastUsedNumber(contactId: String): String? {
        return prefs.getString("last_used_number_$contactId", null)
    }

    fun setFavoriteNumber(contactId: String, number: String?) {
        prefs.edit().putString("favorite_number_$contactId", number).apply()
    }

    fun getFavoriteNumber(contactId: String): String? {
        return prefs.getString("favorite_number_$contactId", null)
    }

    fun setFavoriteSim(contactId: String, simHandle: String?) {
        prefs.edit().putString("favorite_sim_$contactId", simHandle).apply()
    }

    fun getFavoriteSim(contactId: String): String? {
        return prefs.getString("favorite_sim_$contactId", null)
    }

    fun setFavoriteEmail(contactId: String, email: String?) {
        prefs.edit().putString("favorite_email_$contactId", email).apply()
    }

    fun getFavoriteEmail(contactId: String): String? {
        return prefs.getString("favorite_email_$contactId", null)
    }

    fun getFavoritesOrder(): List<String> {
        val orderStr = getString(KEY_FAVORITES_ORDER, null) ?: return emptyList()
        return orderStr.split(",").filter { it.isNotEmpty() }
    }

    fun setFavoritesOrder(order: List<String>) {
        setString(KEY_FAVORITES_ORDER, order.joinToString(","))
    }

    fun getBottomNavOrder(): List<Int> {
        val stored = getString(KEY_BOTTOM_NAV_ORDER, null)
        val parsed = stored
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { DEFAULT_BOTTOM_NAV_ORDER.contains(it) }
            ?.distinct()
            ?: emptyList()

        val result = parsed.toMutableList()
        if (result.isEmpty() && getBoolean(KEY_FLIP_BOTTOM_NAV, false)) {
            result.addAll(DEFAULT_BOTTOM_NAV_ORDER.reversed())
        }
        DEFAULT_BOTTOM_NAV_ORDER.forEach { tab ->
            if (!result.contains(tab)) result.add(tab)
        }
        return result
    }

    fun setBottomNavOrder(order: List<Int>) {
        setString(KEY_BOTTOM_NAV_ORDER, order.joinToString(","))
    }

    fun getHiddenBottomNavTabs(): Set<Int> {
        val stored = getString(KEY_BOTTOM_NAV_HIDDEN, null) ?: return emptySet()
        return stored.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun setHiddenBottomNavTabs(hidden: Set<Int>) {
        setString(KEY_BOTTOM_NAV_HIDDEN, hidden.joinToString(","))
    }

    fun getVisibleBottomNavTabs(): List<Int> {
        val hidden = getHiddenBottomNavTabs()
        val visible = getBottomNavOrder().filter { !hidden.contains(it) }
        return visible.ifEmpty { listOf(TAB_RECENTS) }
    }

    fun resetBottomNavLayout() {
        setBottomNavOrder(DEFAULT_BOTTOM_NAV_ORDER)
        setHiddenBottomNavTabs(emptySet())
        setBoolean(KEY_FLIP_BOTTOM_NAV, false)
    }

    fun contactBackgroundIdKey(contactId: String): String {
        return CONTACT_BACKGROUND_PREFIX + contactId
    }

    fun contactBackgroundNumberKey(numberKey: String): String {
        return CONTACT_BACKGROUND_NUMBER_PREFIX + numberKey
    }

    fun setContactBackground(contactId: String, uri: String?) {
        if (uri == null) {
            prefs.edit().remove(contactBackgroundIdKey(contactId)).apply()
        } else {
            prefs.edit().putString(contactBackgroundIdKey(contactId), uri).apply()
        }
    }

    fun getContactBackground(contactId: String): String? {
        return try {
            prefs.getString(contactBackgroundIdKey(contactId), null)
        } catch (e: Exception) {
            null
        }
    }

    fun setContactBackgroundForNumber(numberKey: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(contactBackgroundNumberKey(numberKey)).apply()
        } else {
            prefs.edit().putString(contactBackgroundNumberKey(numberKey), value).apply()
        }
    }

    fun getContactBackgroundForNumber(numberKey: String): String? {
        return try {
            prefs.getString(contactBackgroundNumberKey(numberKey), null)
        } catch (e: Exception) {
            null
        }
    }

    fun getContactBackgroundEntries(): Map<String, String> {
        return try {
            prefs.all
                .filterKeys { it.startsWith(CONTACT_BACKGROUND_PREFIX) }
                .mapNotNull { entry ->
                    val value = entry.value as? String
                    if (value.isNullOrBlank()) null else entry.key to value
                }
                .toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getContactBackgroundNumberEntries(): Map<String, String> {
        return getContactBackgroundEntries()
            .filterKeys { it.startsWith(CONTACT_BACKGROUND_NUMBER_PREFIX) }
            .mapKeys { it.key.removePrefix(CONTACT_BACKGROUND_NUMBER_PREFIX) }
    }

    fun updateContactBackgroundEntries(updates: Map<String, String?>) {
        if (updates.isEmpty()) return
        val editor = prefs.edit()
        var touched = false
        updates.forEach { (key, value) ->
            if (key.startsWith(CONTACT_BACKGROUND_PREFIX)) {
                touched = true
                if (value == null) editor.remove(key) else editor.putString(key, value)
            }
        }
        if (touched) editor.apply()
    }

    fun getVisibleAccounts(): Set<String>? {
        val str = getString(KEY_VISIBLE_ACCOUNTS, null) ?: return null
        return str.split(",").filter { it.isNotEmpty() }.toSet()
    }

    fun setVisibleAccounts(accounts: Set<String>) {
        setString(KEY_VISIBLE_ACCOUNTS, accounts.joinToString(","))
    }

    companion object {
        const val CONTACT_BACKGROUND_PREFIX = "contact_background_"
        const val CONTACT_BACKGROUND_NUMBER_PREFIX = "contact_background_num_"

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
        const val KEY_BLOCK_METHOD = "block_method"
        const val KEY_BLOCK_LOG_VISIBILITY = "block_log_visibility"
        const val KEY_BLOCK_NOTIFICATION = "block_notification"
        const val KEY_VIBRATE_ON_ANSWER = "vibrate_on_answer"
        const val KEY_VIBRATE_ON_HANGUP = "vibrate_on_hangup"
        const val KEY_ROUND_AVATARS = "round_avatars"
        const val KEY_SHOW_DIVIDERS = "show_dividers"
        const val KEY_TRANSITION_STYLE = "transition_animation_style"
        const val KEY_DIALPAD_STYLE = "dialpad_style"
        const val KEY_VOICEMAIL_NUMBER = "voicemail_number"
        const val KEY_VOICEMAIL_VIBRATION = "voicemail_vibration"
        const val KEY_VOICEMAIL_RINGTONE = "voicemail_ringtone"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_AUTO_ANSWER_DELAY = "auto_answer_delay"
        const val KEY_SHOW_CALL_SUMMARY_TOAST = "show_call_summary_toast"
        const val KEY_VIBRATE_OUTGOING_RINGING = "vibrate_outgoing_ringing"
        const val KEY_FLIP_TO_SILENCE = "flip_to_silence"
        const val KEY_SILENCE_UNKNOWN = "silence_unknown_calls"
        const val KEY_DISPLAY_CARRIER_INFO = "display_carrier_info"
        const val KEY_CALL_DURATION_DISPLAY = "call_duration_display_mode"
        const val KEY_CALL_LOG_GROUPING = "call_log_grouping"
        const val KEY_DIALPAD_LAYOUT = "dialpad_layout_style"
        const val KEY_AVATAR_SHAPE = "avatar_shape"
        const val KEY_SWIPE_TO_CALL = "swipe_to_call"
        const val KEY_DIALPAD_VIBRATION_STRENGTH = "dialpad_vibration_strength"
        const val KEY_DTMF_TONE_VOLUME = "dtmf_tone_volume"
        const val KEY_HAPTIC_LIST_SCROLL = "haptic_list_scroll"
        const val KEY_SHOW_SIM_ICON_HISTORY = "show_sim_icon_history"
        const val KEY_SEARCH_MATCH_MODE = "search_match_mode"
        const val KEY_QUICK_RESPONSE_ENABLED = "quick_response_enabled"
        const val KEY_INCOMING_CALL_UI_MODE = "incoming_call_ui_mode"
        const val KEY_SHOW_CARDS = "show_cards"
        const val KEY_SHOW_CALL_SCREEN_AVATAR = "show_call_screen_avatar"
        const val KEY_CARD_ROUNDNESS = "card_roundness"
        const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        const val KEY_LAST_USED_ACCOUNT_NAME = "last_used_account_name"
        const val KEY_LAST_USED_ACCOUNT_TYPE = "last_used_account_type"
        const val KEY_FAVORITES_ORDER = "favorites_order"
        const val KEY_VISIBLE_ACCOUNTS = "visible_accounts"
        const val KEY_CONTACT_SORT_ORDER = "contact_sort_order"
        const val KEY_CONTACT_DISPLAY_ORDER = "contact_display_order"
        const val KEY_PATREON_PROMPT_SHOWN = "patreon_prompt_shown"
        const val KEY_CALL_RECORDING = "call_recording"
        const val KEY_CALL_RECORDING_AUTO = "call_recording_auto"
        const val KEY_BOTTOM_NAV_ORDER = "bottom_nav_order"
        const val KEY_BOTTOM_NAV_HIDDEN = "bottom_nav_hidden"
        const val KEY_MERGE_FAVORITES_RECENTS = "merge_favorites_recents"

        const val TAB_RECENTS = 0
        const val TAB_FAVORITES = 1
        const val TAB_CONTACTS = 2

        val DEFAULT_BOTTOM_NAV_ORDER = listOf(TAB_RECENTS, TAB_CONTACTS, TAB_FAVORITES)
    }
}
