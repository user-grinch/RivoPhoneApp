package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.SharedPreferences
import com.grinch.rivo4.modal.data.SettingsBackupEnvelope
import com.grinch.rivo4.modal.data.SettingsData
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

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != KEY_APP_USAGE_SECONDS && key != KEY_RATE_APP_SNOOZED_TIME && key != KEY_LAST_SELECTED_TAB && key != KEY_SETTINGS_HISTORY) {
            _settingsChanged.value += 1
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun notifySettingsChanged() {
        _settingsChanged.value += 1
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

    fun getLong(key: String, defaultValue: Long): Long =
        try {
            prefs.getLong(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }

    fun setLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
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

    fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            prefs.getFloat(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun setFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
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
        prefs.edit()
            .putString("favorite_sim_$contactId", simHandle)
            .putString("contact_default_sim_$contactId", simHandle)
            .apply()
    }

    fun getFavoriteSim(contactId: String): String? {
        return prefs.getString("contact_default_sim_$contactId", null)
            ?: prefs.getString("favorite_sim_$contactId", null)
    }

    fun setDefaultSimForContact(contactId: String, simHandle: String?) {
        setFavoriteSim(contactId, simHandle)
    }

    fun getDefaultSimForContact(contactId: String): String? {
        return getFavoriteSim(contactId)
    }

    fun getCustomRecordingFolderUri(): String? {
        return getString(KEY_CALL_RECORDING_FOLDER_URI, null)
    }

    fun setCustomRecordingFolderUri(uri: String?) {
        setString(KEY_CALL_RECORDING_FOLDER_URI, uri)
    }

    fun getCustomRecordingFolderName(): String? {
        return getString(KEY_CALL_RECORDING_FOLDER_NAME, null)
    }

    fun setCustomRecordingFolderName(name: String?) {
        setString(KEY_CALL_RECORDING_FOLDER_NAME, name)
    }

    fun saveCustomRecordingFolder(uri: String, name: String? = null) {
        setCustomRecordingFolderUri(uri)
        if (name != null) setCustomRecordingFolderName(name)
    }

    fun resetCustomRecordingFolder() {
        prefs.edit()
            .remove(KEY_CALL_RECORDING_FOLDER_URI)
            .remove(KEY_CALL_RECORDING_FOLDER_NAME)
            .apply()
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

    fun getQuickResponses(): List<String> {
        val stored = getString(KEY_QUICK_RESPONSES, null)
        if (stored.isNullOrBlank()) return DEFAULT_QUICK_RESPONSES
        val list = stored.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
        return if (list.isNotEmpty()) list else DEFAULT_QUICK_RESPONSES
    }

    fun setQuickResponses(responses: List<String>) {
        setString(KEY_QUICK_RESPONSES, responses.filter { it.isNotBlank() }.joinToString("|||"))
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
        val stored = getString(KEY_BOTTOM_NAV_HIDDEN, null) ?: return setOf(TAB_RECORDINGS)
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

    fun getLastSelectedTab(): Int? {
        return if (prefs.contains(KEY_LAST_SELECTED_TAB)) prefs.getInt(KEY_LAST_SELECTED_TAB, -1)
            .takeIf { it != -1 } else null
    }

    fun setLastSelectedTab(tab: Int) {
        prefs.edit().putInt(KEY_LAST_SELECTED_TAB, tab).apply()
    }

    fun getPriorityContacts(): Set<String> {
        return try {
            prefs.getStringSet(KEY_PRIORITY_CONTACTS, emptySet()) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun isPriorityContact(contactId: String?, number: String?): Boolean {
        val prioritySet = getPriorityContacts()
        if (prioritySet.isEmpty()) return false
        if (!contactId.isNullOrBlank() && prioritySet.contains(contactId)) return true
        if (!number.isNullOrBlank()) {
            val normalized = normalizePhoneNumber(number)
            if (prioritySet.contains(normalized)) return true
            if (prioritySet.contains(number.trim())) return true
            for (p in prioritySet) {
                if (areNumbersEqual(p, number)) return true
            }
        }
        return false
    }

    fun setPriorityContact(contactId: String, numbers: List<String>?, isPriority: Boolean) {
        val current = getPriorityContacts().toMutableSet()
        if (isPriority) {
            if (contactId.isNotBlank()) current.add(contactId)
            numbers?.forEach { num ->
                if (num.isNotBlank()) {
                    current.add(normalizePhoneNumber(num))
                    current.add(num.trim())
                }
            }
        } else {
            if (contactId.isNotBlank()) current.remove(contactId)
            numbers?.forEach { num ->
                if (num.isNotBlank()) {
                    current.remove(normalizePhoneNumber(num))
                    current.remove(num.trim())
                }
            }
        }
        prefs.edit().putStringSet(KEY_PRIORITY_CONTACTS, current).apply()
        _settingsChanged.value += 1
    }

    fun setPriorityContact(contactId: String, number: String?, isPriority: Boolean) =
        setPriorityContact(contactId, if (number != null) listOf(number) else null, isPriority)

    companion object {
        const val KEY_LAST_SELECTED_TAB = "last_selected_bottom_tab"
        const val KEY_PRIORITY_CONTACTS = "priority_contacts_set"
        const val CONTACT_BACKGROUND_PREFIX = "contact_background_"
        const val CONTACT_BACKGROUND_NUMBER_PREFIX = "contact_background_num_"

        const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        const val KEY_AMOLED_MODE = "amoled_mode"
        const val KEY_THEME_MODE = "theme_mode"
        const val THEME_MODE_SYSTEM = 0
        const val THEME_MODE_LIGHT = 1
        const val THEME_MODE_DARK = 2
        const val KEY_DYNAMIC_VARIANT = "dynamic_variant"
        const val KEY_FONT_FAMILY = "font_family"
        const val FONT_FAMILY_SANS_SERIF = "sans_serif"
        const val FONT_FAMILY_SERIF = "serif"
        const val FONT_FAMILY_MONOSPACE = "monospace"
        const val FONT_FAMILY_CURSIVE = "cursive"
        const val FONT_FAMILY_CASUAL = "casual"
        const val FONT_FAMILY_CONDENSED = "sans_serif_condensed"
        const val FONT_FAMILY_LIGHT = "sans_serif_light"
        const val FONT_FAMILY_MEDIUM = "sans_serif_medium"
        const val FONT_FAMILY_BLACK = "sans_serif_black"

        /** Supported font family ids in picker order. */
        val ALL_FONT_FAMILIES = listOf(
            FONT_FAMILY_SANS_SERIF,
            FONT_FAMILY_SERIF,
            FONT_FAMILY_MONOSPACE,
            FONT_FAMILY_CURSIVE,
            FONT_FAMILY_CASUAL,
            FONT_FAMILY_CONDENSED,
            FONT_FAMILY_LIGHT,
            FONT_FAMILY_MEDIUM,
            FONT_FAMILY_BLACK
        )
        const val KEY_FONT_SIZE = "font_size"
        const val FONT_SIZE_SMALL = 0
        const val FONT_SIZE_MEDIUM = 1
        const val FONT_SIZE_LARGE = 2
        const val KEY_USE_SYSTEM_FONT = "use_system_font"
        const val KEY_CALL_BUTTON_ORDER = "call_button_order"
        const val DEFAULT_CALL_BUTTON_ORDER = "mute,keypad,audio,record,hold,add"
        const val KEY_HIDDEN_CALL_BUTTONS = "hidden_call_buttons"
        const val KEY_SHOW_FIRST_LETTER = "show_first_letter"
        const val KEY_COLORFUL_AVATARS = "colorful_avatars"
        const val KEY_GRADIENT_AVATARS = "gradient_avatars"
        const val KEY_SHOW_PICTURE = "show_picture"
        const val KEY_ICON_ONLY_NAV = "icon_only_nav"
        const val KEY_FLIP_BOTTOM_NAV = "flip_bottom_nav"
        const val KEY_DEFAULT_BOTTOM_NAV = "default_bottom_nav"
        const val KEY_DTMF_TONE = "dtmf_tone"
        const val KEY_DIALPAD_VIBRATION = "dialpad_vibration"
        const val KEY_HOLD_ZERO_FOR_PLUS = "hold_zero_for_plus"
        const val KEY_SPEED_DIAL = "speed_dial"
        const val KEY_DEFAULT_VIDEO_APP = "default_video_call_app"
        const val KEY_T9_DIALING = "t9_dialing"
        const val KEY_PROXIMITY_SENSOR = "proximity_sensor"
        const val KEY_AUTO_SPEAKER_PROXIMITY = "auto_speaker_proximity"
        const val KEY_CONTACTS_TRASH_ENABLED = "contacts_trash_enabled"
        const val KEY_INCOMING_CALL_POPUP = "incoming_call_popup"
        const val KEY_ALWAYS_FULL_SCREEN_CALLS = "always_full_screen_calls"
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
        const val KEY_TRANSITION_TIME_SCALE = "transition_time_scale"
        const val KEY_DIALPAD_STYLE = "dialpad_style"
        const val DIALPAD_STYLE_MODERN = 0
        const val DIALPAD_STYLE_CIRCLE = 1
        const val DIALPAD_STYLE_ORGANIC = 2
        const val DIALPAD_STYLE_OUTLINED = 3
        const val DIALPAD_STYLE_UNIFIED = 4
        const val DIALPAD_STYLE_MINIMAL = 5

        const val KEY_DIALPAD_SIZE = "dialpad_size"
        const val DIALPAD_SIZE_COMPACT = 0
        const val DIALPAD_SIZE_MEDIUM = 1
        const val DIALPAD_SIZE_LARGE = 2
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
        const val KEY_GROUP_CALL_LOGS = "group_call_logs"
        const val KEY_DIALPAD_LAYOUT = "dialpad_layout_style"
        const val KEY_AVATAR_SHAPE = "avatar_shape"
        const val KEY_SWIPE_TO_CALL = "swipe_to_call"
        const val KEY_DIALPAD_VIBRATION_STRENGTH = "dialpad_vibration_strength"
        const val KEY_DTMF_TONE_VOLUME = "dtmf_tone_volume"
        const val KEY_HAPTIC_LIST_SCROLL = "haptic_list_scroll"
        const val KEY_MISSED_CALL_NOTIFICATIONS = "missed_call_notifications"
        const val KEY_SHOW_SIM_ICON_HISTORY = "show_sim_icon_history"
        const val KEY_SEARCH_MATCH_MODE = "search_match_mode"
        const val KEY_QUICK_RESPONSE_ENABLED = "quick_response_enabled"
        const val KEY_INCOMING_CALL_UI_MODE = "incoming_call_ui_mode"
        const val KEY_PREDICTIVE_BACK_ENABLED = "predictive_back_gestures_enabled"
        const val KEY_OPEN_CALL_SCREEN_ON_ANSWER = "open_call_screen_on_answer"
        const val KEY_SHOW_CARDS = "show_cards"
        const val KEY_SHOW_CALL_SCREEN_AVATAR = "show_call_screen_avatar"
        const val KEY_HIDE_AVATAR_WITH_BACKGROUND = "hide_avatar_with_background"
        const val KEY_CARD_ROUNDNESS = "card_roundness"
        const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        const val KEY_PERMISSION_POPUP_SHOWN = "permission_popup_shown"
        const val KEY_LAST_USED_ACCOUNT_NAME = "last_used_account_name"
        const val KEY_LAST_USED_ACCOUNT_TYPE = "last_used_account_type"
        const val KEY_FAVORITES_ORDER = "favorites_order"
        const val KEY_VISIBLE_ACCOUNTS = "visible_accounts"
        const val KEY_CONTACT_SORT_ORDER = "contact_sort_order"
        const val KEY_CONTACT_DISPLAY_ORDER = "contact_display_order"
        const val KEY_SHOW_CONTACT_SECTION_HEADERS = "show_contact_section_headers"
        const val KEY_PATREON_PROMPT_SHOWN = "patreon_prompt_shown"
        const val KEY_CALL_RECORDING = "call_recording"
        const val KEY_CALL_RECORDING_AUTO = "call_recording_auto"
        const val KEY_CALL_RECORDING_SHIZUKU = "call_recording_shizuku"
        const val KEY_CALL_RECORDING_FILTER = "call_recording_filter"
        const val KEY_CALL_RECORDING_FOLDER_URI = "call_recording_folder_uri"
        const val KEY_CALL_RECORDING_FOLDER_NAME = "call_recording_folder_name"
        const val RECORD_FILTER_ALL = 0
        const val RECORD_FILTER_INCOMING_ONLY = 1
        const val RECORD_FILTER_OUTGOING_ONLY = 2
        const val RECORD_FILTER_UNKNOWN_ONLY = 3
        const val RECORD_FILTER_CONTACTS_ONLY = 4

        const val KEY_POCKET_MODE = "pocket_mode"
        const val KEY_VOLUME_SQUEEZE_DND = "volume_squeeze_dnd"
        const val KEY_DND_DURING_CALLS = "dnd_during_calls"
        const val KEY_QUICK_RESPONSES = "custom_quick_responses"
        val DEFAULT_QUICK_RESPONSES = listOf(
            "Can't talk now. What's up?",
            "I'll call you right back.",
            "I'll call you later.",
            "Can't talk now. Call me later?",
            "I'm in a meeting. Will message you soon."
        )
        const val KEY_BOTTOM_NAV_ORDER = "bottom_nav_order"
        const val KEY_BOTTOM_NAV_HIDDEN = "bottom_nav_hidden"
        const val KEY_MERGE_FAVORITES_RECENTS = "merge_favorites_recents"
        const val KEY_RECENTS_FAVORITES_COLLAPSED = "recents_favorites_collapsed"
        const val KEY_ENABLE_ADS = "enable_ads"
        const val KEY_IS_SUPPORTER = "is_supporter"
        const val KEY_SUPPORT_PURCHASE_TOKEN = "support_purchase_token"
        const val KEY_SUPPORT_PURCHASE_PRODUCT = "support_purchase_product"
        const val KEY_PRO_AD_EXPIRY = "pro_ad_expiry"
        const val KEY_POST_CALL_SCREEN = "post_call_screen"
        const val KEY_NAV_BAR_STYLE = "nav_bar_style"
        const val NAV_BAR_STYLE_STANDARD = 0
        const val NAV_BAR_STYLE_TOOLBAR = 1

        const val KEY_START_LOCATION = "start_location"
        const val START_LOCATION_NORMAL = 0
        const val START_LOCATION_DIALPAD_RECENTS = 1
        const val START_LOCATION_DIALPAD_CONTACTS = 2

        const val KEY_SECRET_DIALPAD_CODE = "secret_dialpad_code"
        const val DEFAULT_SECRET_DIALPAD_CODE = "*#0000#"
        const val KEY_HIDE_PRIVATE_SETTINGS_ENTRY = "hide_private_settings_entry"
        const val KEY_HIDDEN_CONTACTS_VISIBLE = "hidden_contacts_visible"

        const val KEY_LOG_FAKE_CALLS = "log_fake_calls"
        const val KEY_SHOW_RECENTS_STATS = "show_recents_stats"

        const val KEY_APP_USAGE_SECONDS = "app_usage_seconds"
        const val KEY_RATE_APP_SHOWN = "rate_app_shown"
        const val KEY_RATE_APP_SNOOZED_TIME = "rate_app_snoozed_time"

        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_BIOMETRIC = "app_lock_biometric"
        const val KEY_APP_LOCK_PIN = "app_lock_pin"
        const val KEY_APP_LOCK_TIMEOUT = "app_lock_timeout"
        const val APP_LOCK_TIMEOUT_IMMEDIATELY = 0
        const val APP_LOCK_TIMEOUT_1_MIN = 1
        const val APP_LOCK_TIMEOUT_5_MIN = 5
        const val APP_LOCK_TIMEOUT_15_MIN = 15
        const val APP_LOCK_TIMEOUT_30_MIN = 30

        const val KEY_FLOATING_CALL_BUBBLE = "floating_call_bubble"

        const val KEY_FLOATING_BAR_ROUNDNESS = "floating_bar_roundness"
        const val DEFAULT_FLOATING_BAR_ROUNDNESS = 32
        const val KEY_FLOATING_BAR_BLUR = "floating_bar_blur"
        const val KEY_UI_BLUR = "ui_blur"

        const val KEY_SWIPE_ACTIONS_ENABLED = "swipe_actions_enabled"
        const val KEY_SWIPE_RIGHT_ACTION = "swipe_right_action"
        const val KEY_SWIPE_LEFT_ACTION = "swipe_left_action"

        const val SWIPE_ACTION_NONE = 0
        const val SWIPE_ACTION_CALL = 1
        const val SWIPE_ACTION_MESSAGE = 2
        const val SWIPE_ACTION_VIDEO_CALL = 3
        const val SWIPE_ACTION_WHATSAPP = 4
        const val SWIPE_ACTION_COPY_NUMBER = 5
        const val SWIPE_ACTION_DELETE = 6
        const val SWIPE_ACTION_VIEW_CONTACT = 7
        const val SWIPE_ACTION_SHARE = 8
        const val SWIPE_ACTION_BLOCK = 9

        const val KEY_CALL_LOG_ACTIONS = "call_log_actions"
        const val KEY_CALL_LOG_ACTIONS_SAVED = "call_log_actions_saved"
        const val KEY_CALL_LOG_ACTIONS_UNSAVED = "call_log_actions_unsaved"
        const val KEY_CALL_LOG_TRAILING_BUTTON_ACTION = "call_log_trailing_button_action"

        val DEFAULT_CALL_LOG_ACTIONS_SAVED = listOf("view_contact", "block", "message")
        val DEFAULT_CALL_LOG_ACTIONS_UNSAVED = listOf("save", "block", "message")
        const val DEFAULT_CALL_LOG_TRAILING_BUTTON_ACTION = "call"
        val DEFAULT_CALL_LOG_ACTIONS = DEFAULT_CALL_LOG_ACTIONS_SAVED

        const val TAB_RECENTS = 0
        const val TAB_FAVORITES = 1
        const val TAB_CONTACTS = 2
        const val TAB_RECORDINGS = 3

        val DEFAULT_BOTTOM_NAV_ORDER = listOf(TAB_RECENTS, TAB_CONTACTS, TAB_FAVORITES, TAB_RECORDINGS)

        const val KEY_MISSED_CALL_CARD = "missed_call_card"
        const val KEY_AUTO_DECLINE_UNKNOWN = "auto_decline_unknown"
        const val KEY_AUTO_DECLINE_NON_CONTACTS = "auto_decline_non_contacts"
        const val KEY_DUAL_SIM_DIALPAD_BUTTONS = "dual_sim_dialpad_buttons"
        const val KEY_CALL_LOG_LIMIT = "call_log_limit"
        const val CALL_LOG_LIMIT_DEFAULT = 500
        const val KEY_AUTO_PASTE_CLIPBOARD = "auto_paste_clipboard"
        const val KEY_SEPARATE_CONTACTS_ICON = "separate_contacts_icon"
        const val KEY_CALL_ANALYTICS_TRACKING = "call_analytics_tracking"
        const val KEY_SHOW_CONTACT_MANAGEMENT_CARD = "show_contact_management_card"
        const val KEY_SHOW_COUNTRY_CODE = "show_country_code"
        const val KEY_SETTINGS_HISTORY = "settings_history"
        const val KEY_PRO_STATUS = "pro_status"
        const val KEY_VIDEO_AD_REMINDER_MODE = "video_ad_reminder_mode"
        const val KEY_LAST_VIDEO_AD_REMINDER = "last_video_ad_reminder"
        const val VIDEO_AD_REMINDER_OFF = 0
        const val VIDEO_AD_REMINDER_SIX_HOURS = 1
        const val VIDEO_AD_REMINDER_ALWAYS = 2
        const val VIDEO_AD_REMINDER_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L
        const val KEY_PRO_BRANDING_DISABLED = "pro_branding_disabled"
        const val KEY_SELECTED_APP_ICON = "selected_app_icon"

        fun shouldShowVideoAdReminder(mode: Int, lastShown: Long, now: Long): Boolean =
            mode == VIDEO_AD_REMINDER_SIX_HOURS &&
                now - lastShown >= VIDEO_AD_REMINDER_INTERVAL_MILLIS

        val EXCLUDED_KEYS = setOf(
            KEY_APP_USAGE_SECONDS,
            KEY_RATE_APP_SHOWN,
            KEY_RATE_APP_SNOOZED_TIME,
            KEY_ONBOARDING_SHOWN,
            KEY_PERMISSION_POPUP_SHOWN,
            KEY_PATREON_PROMPT_SHOWN,
            KEY_IS_SUPPORTER,
            KEY_SETTINGS_HISTORY,
            KEY_LAST_SELECTED_TAB,
            KEY_APP_LOCK_PIN,
            KEY_APP_LOCK_ENABLED,
            KEY_CALL_RECORDING_FOLDER_URI,
            KEY_CALL_RECORDING_FOLDER_NAME,
            "oem_opt_dismissed"
        )

        fun isExcludedKey(key: String): Boolean {
            if (EXCLUDED_KEYS.contains(key)) return true
            if (key.startsWith("cache_") || key.startsWith("temp_")) return true
            return false
        }

        fun getKnownDefaultBooleans(): Map<String, Boolean> = mapOf(
            KEY_DYNAMIC_COLORS to true,
            KEY_AMOLED_MODE to false,
            KEY_SHOW_FIRST_LETTER to true,
            KEY_COLORFUL_AVATARS to false,
            KEY_GRADIENT_AVATARS to false,
            KEY_SHOW_PICTURE to true,
            KEY_ROUND_AVATARS to false,
            KEY_SHOW_DIVIDERS to true,
            KEY_ICON_ONLY_NAV to false,
            KEY_FLIP_BOTTOM_NAV to false,
            KEY_DTMF_TONE to true,
            KEY_DIALPAD_VIBRATION to true,
            KEY_HOLD_ZERO_FOR_PLUS to true,
            KEY_SPEED_DIAL to true,
            KEY_T9_DIALING to true,
            KEY_PROXIMITY_SENSOR to true,
            KEY_AUTO_SPEAKER_PROXIMITY to false,
            KEY_CONTACTS_TRASH_ENABLED to true,
            KEY_INCOMING_CALL_POPUP to false,
            KEY_ALWAYS_FULL_SCREEN_CALLS to false,
            KEY_AUTO_REDIAL_BUSY to false,
            KEY_BLOCK_NOTIFICATION to true,
            KEY_VIBRATE_ON_ANSWER to true,
            KEY_VIBRATE_ON_HANGUP to false,
            KEY_VOICEMAIL_VIBRATION to true,
            KEY_KEEP_SCREEN_ON to true,
            KEY_SHOW_CALL_SUMMARY_TOAST to false,
            KEY_VIBRATE_OUTGOING_RINGING to true,
            KEY_FLIP_TO_SILENCE to false,
            KEY_SILENCE_UNKNOWN to false,
            KEY_DISPLAY_CARRIER_INFO to true,
            KEY_SWIPE_TO_CALL to true,
            KEY_HAPTIC_LIST_SCROLL to false,
            KEY_MISSED_CALL_NOTIFICATIONS to true,
            KEY_SHOW_SIM_ICON_HISTORY to true,
            KEY_QUICK_RESPONSE_ENABLED to true,
            KEY_PREDICTIVE_BACK_ENABLED to true,
            KEY_OPEN_CALL_SCREEN_ON_ANSWER to false,
            KEY_SHOW_CARDS to true,
            KEY_SHOW_CALL_SCREEN_AVATAR to true,
            KEY_HIDE_AVATAR_WITH_BACKGROUND to false,
            KEY_SHOW_CONTACT_SECTION_HEADERS to true,
            KEY_CALL_RECORDING to true,
            KEY_CALL_RECORDING_AUTO to false,
            KEY_CALL_RECORDING_SHIZUKU to true,
            KEY_POCKET_MODE to false,
            KEY_VOLUME_SQUEEZE_DND to false,
            KEY_DND_DURING_CALLS to false,
            KEY_MERGE_FAVORITES_RECENTS to false,
            KEY_RECENTS_FAVORITES_COLLAPSED to false,
            KEY_POST_CALL_SCREEN to true,
            KEY_HIDE_PRIVATE_SETTINGS_ENTRY to false,
            KEY_HIDDEN_CONTACTS_VISIBLE to false,
            KEY_LOG_FAKE_CALLS to true,
            KEY_SHOW_RECENTS_STATS to true,
            KEY_FLOATING_CALL_BUBBLE to true,
            KEY_UI_BLUR to false,
            KEY_FLOATING_BAR_BLUR to false,
            KEY_SWIPE_ACTIONS_ENABLED to false,
            KEY_MISSED_CALL_CARD to true,
            KEY_AUTO_DECLINE_UNKNOWN to false,
            KEY_AUTO_DECLINE_NON_CONTACTS to false,
            KEY_DUAL_SIM_DIALPAD_BUTTONS to false,
            KEY_AUTO_PASTE_CLIPBOARD to false,
            KEY_SEPARATE_CONTACTS_ICON to false,
            KEY_CALL_ANALYTICS_TRACKING to true,
            KEY_SHOW_CONTACT_MANAGEMENT_CARD to true,
            KEY_SHOW_COUNTRY_CODE to true,
            KEY_GROUP_CALL_LOGS to true
        )

        fun getKnownDefaultFloats(): Map<String, Float> = mapOf(
            KEY_TRANSITION_TIME_SCALE to 2.5f
        )

        fun getKnownDefaultIntegers(): Map<String, Int> = mapOf(
            KEY_REDIAL_ATTEMPTS to 3,
            KEY_REDIAL_DELAY to 5,
            KEY_BLOCK_METHOD to 0,
            KEY_BLOCK_LOG_VISIBILITY to 0,
            KEY_TRANSITION_STYLE to 0,
            KEY_DIALPAD_STYLE to DIALPAD_STYLE_MODERN,
            KEY_DIALPAD_SIZE to DIALPAD_SIZE_MEDIUM,
            KEY_AUTO_ANSWER_DELAY to 0,
            KEY_CALL_DURATION_DISPLAY to 0,
            KEY_CALL_LOG_GROUPING to 0,
            KEY_DIALPAD_LAYOUT to 0,
            KEY_AVATAR_SHAPE to 0,
            KEY_CARD_ROUNDNESS to 24,
            KEY_DIALPAD_VIBRATION_STRENGTH to 2,
            KEY_DTMF_TONE_VOLUME to 80,
            KEY_SEARCH_MATCH_MODE to 0,
            KEY_INCOMING_CALL_UI_MODE to 0,
            KEY_CONTACT_SORT_ORDER to 0,
            KEY_CONTACT_DISPLAY_ORDER to 0,
            KEY_CALL_RECORDING_FILTER to RECORD_FILTER_ALL,
            KEY_NAV_BAR_STYLE to NAV_BAR_STYLE_STANDARD,
            KEY_START_LOCATION to START_LOCATION_NORMAL,
            KEY_FLOATING_BAR_ROUNDNESS to DEFAULT_FLOATING_BAR_ROUNDNESS,
            KEY_SWIPE_RIGHT_ACTION to SWIPE_ACTION_CALL,
            KEY_SWIPE_LEFT_ACTION to SWIPE_ACTION_MESSAGE,
            KEY_CALL_LOG_LIMIT to CALL_LOG_LIMIT_DEFAULT,
            KEY_DEFAULT_BOTTOM_NAV to 0
        )

        fun getKnownDefaultStrings(): Map<String, String> = mapOf(
            KEY_QUICK_RESPONSES to DEFAULT_QUICK_RESPONSES.joinToString("|||"),
            KEY_BOTTOM_NAV_ORDER to DEFAULT_BOTTOM_NAV_ORDER.joinToString(","),
            KEY_BOTTOM_NAV_HIDDEN to listOf(TAB_RECORDINGS).joinToString(","),
            KEY_SECRET_DIALPAD_CODE to DEFAULT_SECRET_DIALPAD_CODE,
            KEY_DEFAULT_VIDEO_APP to "",
            KEY_HIDDEN_CALL_BUTTONS to ""
        )

        fun getKnownDefaultStringSets(): Map<String, Set<String>> = mapOf(
            KEY_PRIORITY_CONTACTS to emptySet()
        )
    }

    fun getCallLogLimit(): Int = getInt(KEY_CALL_LOG_LIMIT, CALL_LOG_LIMIT_DEFAULT)
    fun setCallLogLimit(limit: Int) = setInt(KEY_CALL_LOG_LIMIT, limit)

    fun isAutoPasteClipboardEnabled(): Boolean = getBoolean(KEY_AUTO_PASTE_CLIPBOARD, false)
    fun setAutoPasteClipboardEnabled(enabled: Boolean) = setBoolean(KEY_AUTO_PASTE_CLIPBOARD, enabled)

    fun isSeparateContactsIconEnabled(): Boolean = getBoolean(KEY_SEPARATE_CONTACTS_ICON, false)
    fun setSeparateContactsIconEnabled(enabled: Boolean) = setBoolean(KEY_SEPARATE_CONTACTS_ICON, enabled)

    fun isCallAnalyticsTrackingEnabled(): Boolean = getBoolean(KEY_CALL_ANALYTICS_TRACKING, true)
    fun setCallAnalyticsTrackingEnabled(enabled: Boolean) = setBoolean(KEY_CALL_ANALYTICS_TRACKING, enabled)

    fun isContactManagementCardEnabled(): Boolean = getBoolean(KEY_SHOW_CONTACT_MANAGEMENT_CARD, true)
    fun setContactManagementCardEnabled(enabled: Boolean) = setBoolean(KEY_SHOW_CONTACT_MANAGEMENT_CARD, enabled)

    fun isAppLockEnabled(): Boolean = getBoolean(KEY_APP_LOCK_ENABLED, false)
    fun setAppLockEnabled(enabled: Boolean) = setBoolean(KEY_APP_LOCK_ENABLED, enabled)
    fun isBiometricLockEnabled(): Boolean = getBoolean(KEY_APP_LOCK_BIOMETRIC, true)
    fun setBiometricLockEnabled(enabled: Boolean) = setBoolean(KEY_APP_LOCK_BIOMETRIC, enabled)
    fun getAppLockPin(): String = getString(KEY_APP_LOCK_PIN, "") ?: ""
    fun setAppLockPin(pin: String) = setString(KEY_APP_LOCK_PIN, pin)
    fun getAppLockTimeout(): Int = getInt(KEY_APP_LOCK_TIMEOUT, APP_LOCK_TIMEOUT_IMMEDIATELY)
    fun setAppLockTimeout(timeout: Int) = setInt(KEY_APP_LOCK_TIMEOUT, timeout)
    fun isFloatingCallBubbleEnabled(): Boolean = getBoolean(KEY_FLOATING_CALL_BUBBLE, true)
    fun setFloatingCallBubbleEnabled(enabled: Boolean) = setBoolean(KEY_FLOATING_CALL_BUBBLE, enabled)

    fun isPredictiveBackEnabled(): Boolean = getBoolean(KEY_PREDICTIVE_BACK_ENABLED, true)
    fun setPredictiveBackEnabled(enabled: Boolean) = setBoolean(KEY_PREDICTIVE_BACK_ENABLED, enabled)

    fun getTransitionTimeScale(): Float = getFloat(KEY_TRANSITION_TIME_SCALE, 2.5f).coerceIn(0.2f, 5.0f)
    fun setTransitionTimeScale(scale: Float) = setFloat(KEY_TRANSITION_TIME_SCALE, scale.coerceIn(0.2f, 5.0f))

    fun isOpenCallScreenOnAnswerEnabled(): Boolean = getBoolean(KEY_OPEN_CALL_SCREEN_ON_ANSWER, false)
    fun setOpenCallScreenOnAnswerEnabled(enabled: Boolean) = setBoolean(KEY_OPEN_CALL_SCREEN_ON_ANSWER, enabled)
    fun isHiddenContactsVisible(): Boolean = getBoolean(KEY_HIDDEN_CONTACTS_VISIBLE, false)
    fun setHiddenContactsVisible(visible: Boolean) = setBoolean(KEY_HIDDEN_CONTACTS_VISIBLE, visible)

    fun getFloatingBarRoundness(): Int = getInt(KEY_FLOATING_BAR_ROUNDNESS, DEFAULT_FLOATING_BAR_ROUNDNESS)
    fun setFloatingBarRoundness(roundness: Int) = setInt(KEY_FLOATING_BAR_ROUNDNESS, roundness)

    fun isFloatingBarBlurEnabled(): Boolean = isUiBlurEnabled()
    fun setFloatingBarBlurEnabled(enabled: Boolean) = setUiBlurEnabled(enabled)
    fun isUiBlurEnabled(): Boolean = getBoolean(KEY_UI_BLUR, getBoolean(KEY_FLOATING_BAR_BLUR, false))
    fun setUiBlurEnabled(enabled: Boolean) {
        setBoolean(KEY_UI_BLUR, enabled)
        setBoolean(KEY_FLOATING_BAR_BLUR, enabled)
    }

    fun isSwipeActionsEnabled(): Boolean = getBoolean(KEY_SWIPE_ACTIONS_ENABLED, false)
    fun setSwipeActionsEnabled(enabled: Boolean) = setBoolean(KEY_SWIPE_ACTIONS_ENABLED, enabled)

    fun getSwipeRightAction(): Int = getInt(KEY_SWIPE_RIGHT_ACTION, SWIPE_ACTION_CALL)
    fun setSwipeRightAction(action: Int) = setInt(KEY_SWIPE_RIGHT_ACTION, action)

    fun getSwipeLeftAction(): Int = getInt(KEY_SWIPE_LEFT_ACTION, SWIPE_ACTION_MESSAGE)
    fun setSwipeLeftAction(action: Int) = setInt(KEY_SWIPE_LEFT_ACTION, action)

    fun getCallLogSavedActions(): List<String> {
        val raw = getString(KEY_CALL_LOG_ACTIONS_SAVED, getString(KEY_CALL_LOG_ACTIONS, null))
        if (raw == null) return DEFAULT_CALL_LOG_ACTIONS_SAVED
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setCallLogSavedActions(actions: List<String>) {
        setString(KEY_CALL_LOG_ACTIONS_SAVED, actions.joinToString(","))
        setString(KEY_CALL_LOG_ACTIONS, actions.joinToString(","))
    }

    fun getCallLogExpandedActions(): List<String> = getCallLogSavedActions()
    fun setCallLogExpandedActions(actions: List<String>) = setCallLogSavedActions(actions)

    fun getCallLogUnsavedActions(): List<String> {
        val raw = getString(KEY_CALL_LOG_ACTIONS_UNSAVED, null)
        if (raw == null) return DEFAULT_CALL_LOG_ACTIONS_UNSAVED
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setCallLogUnsavedActions(actions: List<String>) {
        setString(KEY_CALL_LOG_ACTIONS_UNSAVED, actions.joinToString(","))
    }

    fun getCallLogTrailingAction(): String =
        getString(KEY_CALL_LOG_TRAILING_BUTTON_ACTION, DEFAULT_CALL_LOG_TRAILING_BUTTON_ACTION)
            ?: DEFAULT_CALL_LOG_TRAILING_BUTTON_ACTION

    fun setCallLogTrailingAction(action: String) = setString(KEY_CALL_LOG_TRAILING_BUTTON_ACTION, action)

    fun resetCallLogCustomization() {
        setCallLogSavedActions(DEFAULT_CALL_LOG_ACTIONS_SAVED)
        setCallLogUnsavedActions(DEFAULT_CALL_LOG_ACTIONS_UNSAVED)
        setCallLogTrailingAction(DEFAULT_CALL_LOG_TRAILING_BUTTON_ACTION)
    }

    fun isSupporter(): Boolean = getBoolean(KEY_IS_SUPPORTER, false)
    fun setSupporter(isSupporter: Boolean) = setBoolean(KEY_IS_SUPPORTER, isSupporter)
    fun getSupportPurchaseToken(): String = getString(KEY_SUPPORT_PURCHASE_TOKEN, "") ?: ""
    fun setSupportPurchase(token: String, productId: String) {
        setString(KEY_SUPPORT_PURCHASE_TOKEN, token)
        setString(KEY_SUPPORT_PURCHASE_PRODUCT, productId)
        setSupporter(true)
    }
    fun hasAdSupportedPro(): Boolean = System.currentTimeMillis() < getLong(KEY_PRO_AD_EXPIRY, 0L)
    fun grantAdSupportedPro(durationMillis: Long = 24 * 60 * 60 * 1000L) {
        setLong(KEY_PRO_AD_EXPIRY, System.currentTimeMillis() + durationMillis)
    }

    fun getDefaultVideoApp(): String = getString(KEY_DEFAULT_VIDEO_APP, "") ?: ""
    fun setDefaultVideoApp(appId: String?) = setString(KEY_DEFAULT_VIDEO_APP, appId ?: "")

    fun isPostCallScreenEnabled(): Boolean = getBoolean(KEY_POST_CALL_SCREEN, true)
    fun setPostCallScreenEnabled(enabled: Boolean) = setBoolean(KEY_POST_CALL_SCREEN, enabled)

    fun isMissedCallCardEnabled(): Boolean = getBoolean(KEY_MISSED_CALL_CARD, true)
    fun setMissedCallCardEnabled(enabled: Boolean) = setBoolean(KEY_MISSED_CALL_CARD, enabled)

    fun isAutoDeclineUnknownEnabled(): Boolean = getBoolean(KEY_AUTO_DECLINE_UNKNOWN, false)
    fun setAutoDeclineUnknownEnabled(enabled: Boolean) = setBoolean(KEY_AUTO_DECLINE_UNKNOWN, enabled)

    fun isAutoDeclineNonContactsEnabled(): Boolean = getBoolean(KEY_AUTO_DECLINE_NON_CONTACTS, false)
    fun setAutoDeclineNonContactsEnabled(enabled: Boolean) = setBoolean(KEY_AUTO_DECLINE_NON_CONTACTS, enabled)

    fun getDialpadStyle(): Int = getInt(KEY_DIALPAD_STYLE, DIALPAD_STYLE_MODERN)
    fun setDialpadStyle(style: Int) = setInt(KEY_DIALPAD_STYLE, style)

    fun getDialpadSize(): Int = getInt(KEY_DIALPAD_SIZE, DIALPAD_SIZE_MEDIUM)
    fun setDialpadSize(size: Int) = setInt(KEY_DIALPAD_SIZE, size)

    fun isDualSimDialpadButtonsEnabled(): Boolean = getBoolean(KEY_DUAL_SIM_DIALPAD_BUTTONS, false)
    fun setDualSimDialpadButtonsEnabled(enabled: Boolean) = setBoolean(KEY_DUAL_SIM_DIALPAD_BUTTONS, enabled)

    fun isAutoSpeakerProximityEnabled(): Boolean = getBoolean(KEY_AUTO_SPEAKER_PROXIMITY, false)
    fun setAutoSpeakerProximityEnabled(enabled: Boolean) = setBoolean(KEY_AUTO_SPEAKER_PROXIMITY, enabled)

    fun isContactsTrashEnabled(): Boolean = getBoolean(KEY_CONTACTS_TRASH_ENABLED, true)
    fun setContactsTrashEnabled(enabled: Boolean) = setBoolean(KEY_CONTACTS_TRASH_ENABLED, enabled)

    fun resetSwipeActions() {
        setBoolean(KEY_SWIPE_ACTIONS_ENABLED, false)
        setInt(KEY_SWIPE_RIGHT_ACTION, SWIPE_ACTION_CALL)
        setInt(KEY_SWIPE_LEFT_ACTION, SWIPE_ACTION_MESSAGE)
    }

    fun isShowCarrierInRecents(): Boolean = getBoolean(KEY_SHOW_SIM_ICON_HISTORY, true)
    fun setShowCarrierInRecents(enabled: Boolean) {
        setBoolean(KEY_SHOW_SIM_ICON_HISTORY, enabled)
    }

    fun isShowCountryCodeEnabled(): Boolean = getBoolean(KEY_SHOW_COUNTRY_CODE, true)
    fun setShowCountryCodeEnabled(enabled: Boolean) {
        setBoolean(KEY_SHOW_COUNTRY_CODE, enabled)
        clearFormattedNumberCache()
    }


    fun isProStatus(): Boolean = getBoolean(KEY_PRO_STATUS, false)
    fun setProStatus(isPro: Boolean) = setBoolean(KEY_PRO_STATUS, isPro)

    fun getVideoAdReminderMode(): Int =
        getInt(KEY_VIDEO_AD_REMINDER_MODE, VIDEO_AD_REMINDER_SIX_HOURS)
            .coerceIn(VIDEO_AD_REMINDER_OFF, VIDEO_AD_REMINDER_ALWAYS)
    fun setVideoAdReminderMode(mode: Int) = setInt(KEY_VIDEO_AD_REMINDER_MODE, mode)
    fun shouldShowVideoAdReminder(now: Long = System.currentTimeMillis()): Boolean {
        return when (getVideoAdReminderMode()) {
            VIDEO_AD_REMINDER_ALWAYS -> true
            VIDEO_AD_REMINDER_SIX_HOURS -> shouldShowVideoAdReminder(
                VIDEO_AD_REMINDER_SIX_HOURS,
                getLong(KEY_LAST_VIDEO_AD_REMINDER, 0L),
                now
            )
            else -> false
        }
    }
    fun markVideoAdReminderShown(now: Long = System.currentTimeMillis()) =
        setLong(KEY_LAST_VIDEO_AD_REMINDER, now)

    fun isProBrandingDisabled(): Boolean = getBoolean(KEY_PRO_BRANDING_DISABLED, false)
    fun setProBrandingDisabled(disabled: Boolean) = setBoolean(KEY_PRO_BRANDING_DISABLED, disabled)

    fun getSelectedAppIcon(): String = getString(KEY_SELECTED_APP_ICON, "default") ?: "default"
    fun setSelectedAppIcon(iconName: String) = setString(KEY_SELECTED_APP_ICON, iconName)

    fun isShowContactSectionHeaders(): Boolean = getBoolean(KEY_SHOW_CONTACT_SECTION_HEADERS, true)
    fun setShowContactSectionHeaders(enabled: Boolean) {
        setBoolean(KEY_SHOW_CONTACT_SECTION_HEADERS, enabled)
    }

    fun getSettingsHistory(): List<String> {
        val raw = getString(KEY_SETTINGS_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|::|").filter { it.isNotBlank() }
    }

    fun addSettingsHistory(title: String) {
        if (title.isBlank()) return
        val current = getSettingsHistory().toMutableList()
        current.remove(title)
        current.add(0, title)
        if (current.size > 8) {
            current.subList(8, current.size).clear()
        }
        setString(KEY_SETTINGS_HISTORY, current.joinToString("|::|"))
    }

    fun clearSettingsHistory() {
        setString(KEY_SETTINGS_HISTORY, "")
    }

    fun getAllExportableSettings(appVersionName: String, appVersionCode: Long): SettingsBackupEnvelope {
        val allPrefs = try {
            prefs.all
        } catch (e: Exception) {
            emptyMap()
        }

        val booleans = getKnownDefaultBooleans().toMutableMap()
        val integers = getKnownDefaultIntegers().toMutableMap()
        val longs = mutableMapOf<String, Long>()
        val floats = getKnownDefaultFloats().toMutableMap()
        val strings = getKnownDefaultStrings().toMutableMap()
        val stringSets = getKnownDefaultStringSets().toMutableMap()

        for ((key, value) in allPrefs) {
            if (isExcludedKey(key) || value == null) continue
            when (value) {
                is Boolean -> booleans[key] = value
                is Int -> integers[key] = value
                is Long -> longs[key] = value
                is Float -> floats[key] = value
                is String -> strings[key] = value
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val strSet = (value as? Set<String>) ?: value.mapNotNull { it?.toString() }.toSet()
                    stringSets[key] = strSet
                }
            }
        }

        return SettingsBackupEnvelope(
            app = SettingsBackupEnvelope.DEFAULT_APP_IDENTIFIER,
            backupType = SettingsBackupEnvelope.BACKUP_TYPE_SETTINGS,
            schemaVersion = SettingsBackupEnvelope.CURRENT_SCHEMA_VERSION,
            appVersion = appVersionName,
            appVersionCode = appVersionCode,
            createdAt = System.currentTimeMillis(),
            settings = SettingsData(
                booleans = booleans,
                integers = integers,
                longs = longs,
                floats = floats,
                strings = strings,
                stringSets = stringSets
            )
        )
    }

    private fun migrateSettingsData(data: SettingsData, schemaVersion: Int): SettingsData {
        var current = data
        if (schemaVersion < 1) {
            // Placeholder for pre-v1 migrations if ever needed
        }
        return current
    }

    fun restoreSettings(envelope: SettingsBackupEnvelope): Int {
        if (!envelope.app.equals(SettingsBackupEnvelope.DEFAULT_APP_IDENTIFIER, ignoreCase = true) &&
            !envelope.app.equals("Rivo", ignoreCase = true)
        ) {
            throw IllegalArgumentException("Invalid backup file: not a Rivo backup")
        }
        if (envelope.backupType != SettingsBackupEnvelope.BACKUP_TYPE_SETTINGS && envelope.backupType.isNotEmpty()) {
            throw IllegalArgumentException("Invalid backup file: not a settings backup")
        }

        val data = envelope.resolvedSettings()
        val editor = prefs.edit()
        var restoredCount = 0

        val migratedData = migrateSettingsData(data, envelope.schemaVersion)
        val knownBoolKeys = getKnownDefaultBooleans().keys
        val knownIntKeys = getKnownDefaultIntegers().keys

        for ((key, value) in migratedData.booleans) {
            if (!isExcludedKey(key)) {
                editor.putBoolean(key, value)
                restoredCount++
            }
        }
        for ((key, value) in migratedData.integers) {
            if (!isExcludedKey(key)) {
                editor.putInt(key, value)
                restoredCount++
            }
        }
        for ((key, value) in migratedData.longs) {
            if (!isExcludedKey(key)) {
                if (knownIntKeys.contains(key)) {
                    editor.putInt(key, value.toInt())
                } else {
                    editor.putLong(key, value)
                }
                restoredCount++
            }
        }
        for ((key, value) in migratedData.floats) {
            if (!isExcludedKey(key)) {
                editor.putFloat(key, value)
                restoredCount++
            }
        }
        for ((key, value) in migratedData.strings) {
            if (!isExcludedKey(key)) {
                if (knownBoolKeys.contains(key) && (value.equals("true", true) || value.equals("false", true))) {
                    editor.putBoolean(key, value.toBoolean())
                } else if (knownIntKeys.contains(key) && value.toIntOrNull() != null) {
                    editor.putInt(key, value.toInt())
                } else {
                    editor.putString(key, value)
                }
                restoredCount++
            }
        }
        for ((key, value) in migratedData.stringSets) {
            if (!isExcludedKey(key)) {
                editor.putStringSet(key, value)
                restoredCount++
            }
        }

        editor.apply()
        _settingsChanged.value += 1
        clearFormattedNumberCache()
        return restoredCount
    }
}
