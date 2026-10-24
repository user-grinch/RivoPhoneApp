package com.grinch.rivo4.view.screen.settings
import com.grinch.rivo4.view.components.MenuTopAppBar

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.automirrored.outlined.PhoneMissed

import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.grinch.rivo4.PATREON_URL
import com.grinch.rivo4.PLAY_STORE_URL
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SettingsHighlightManager
import com.grinch.rivo4.controller.util.ProBrandingManager
import kotlinx.coroutines.delay
import com.grinch.rivo4.controller.util.getAppVersion
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDialogAction
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.ad.IS_ADS_SUPPORTED
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val appInfo = remember { getAppVersion(context) }
    val logoMorph = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { 0.2f }

    var enableAds by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ENABLE_ADS, true)) }
    var showDisableAdsDialog by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
            searchListState.scrollToItem(0)
        }
    }

    LaunchedEffect(searchQuery) {
        searchListState.scrollToItem(0)
    }

    LaunchedEffect(SettingsHighlightManager.highlightEpoch) {
        if (SettingsHighlightManager.highlightEpoch == 0L || isSearchActive) return@LaunchedEffect
        val target = SettingsHighlightManager.highlightedSettingTitle ?: return@LaunchedEffect
        if (target.contains("Ad", ignoreCase = true) || target.contains("Banner", ignoreCase = true)) {
            delay(150L)
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
        }
    }

val searchItems = remember(isSearchActive) {
        if (!isSearchActive) emptyList() else listOf(
            // ==================== PERSONALIZATION & DISPLAY ====================
            SettingSearchItem(
                title = context.getString(R.string.loc_theme_appearance),
                supporting = context.getString(R.string.loc_theme_mode_interface_appearance_and_visual_style),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Palette,
                keywords = listOf("theme", "appearance", "interface", "ui theme", "dark theme", "light theme"),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_material_you_dynamic_colors),
                supporting = context.getString(R.string.loc_follow_system_wallpaper_color_palette_and_dynamic_themes),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Palette,
                keywords = listOf("material you", "dynamic colors", "monet", "wallpaper color", "system theme", "palette"),
                targetHeadline = context.getString(R.string.settings_interface_material_you),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_amoled_dark_mode),
                supporting = context.getString(R.string.loc_pure_pitch_black_background_for_oled_amoled_battery_saving),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.DarkMode,
                keywords = listOf("amoled", "oled", "pure black", "pitch black", "true black", "dark theme", "battery saver"),
                targetHeadline = context.getString(R.string.settings_interface_amoled),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_card_roundness),
                supporting = context.getString(R.string.loc_adjust_corner_curvature_from_subtle_to_ultra_rounded),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.RoundedCorner,
                keywords = listOf("roundness", "corner radius", "curvature", "corners", "rounded cards", "shape"),
                targetHeadline = context.getString(R.string.settings_interface_card_roundness),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_transition_animations),
                supporting = context.getString(R.string.loc_screen_transition_animations_and_motion_effects),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Animation,
                keywords = listOf("animations", "motion", "transitions", "slide", "fade", "smooth"),
                targetHeadline = context.getString(R.string.settings_interface_transition_animation),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_transition_animation_speed),
                supporting = context.getString(R.string.loc_adjust_transition_animation_time_scale_across_pages_tabs_and_menus),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Speed,
                keywords = listOf("transition speed", "animation speed", "time scale", "duration", "fast animations", "slow animations"),
                targetHeadline = "Transition Animation Speed",
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_predictive_back_gestures),
                supporting = context.getString(R.string.loc_interactive_swipe_to_preview_animations_when_navigating_back),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                keywords = listOf("predictive back", "predictive back gestures", "disable predictive back", "back gesture", "back animation", "gesture navigation", "swipe back"),
                targetHeadline = context.getString(R.string.settings_predictive_back_title),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_carrier_names_in_recents),
                supporting = context.getString(R.string.loc_show_or_hide_carrier_and_sim_card_names_in_call_history_and_recents),
                category = context.getString(R.string.loc_calling_sim_accounts),
                icon = Icons.Outlined.SimCard,
                keywords = listOf("carrier", "carrier name", "recents carrier", "sim", "hide carrier", "show carrier", "call log carrier", "recents sim"),
                targetHeadline = context.getString(R.string.settings_show_carrier_in_recents),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_group_call_logs),
                supporting = context.getString(R.string.loc_merge_repeated_calls_from_the_same_contact_into_a_single_entry),
                category = context.getString(R.string.loc_calling_sim_accounts),
                icon = Icons.AutoMirrored.Outlined.CallMerge,
                keywords = listOf("group call logs", "call log grouping", "merge calls", "call history", "recents grouping"),
                targetHeadline = context.getString(R.string.settings_group_call_logs),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_dialpad_dual_sim_buttons),
                supporting = context.getString(R.string.loc_show_separate_sim_1_and_sim_2_call_buttons_directly_on_dialpad),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.SimCard,
                keywords = listOf("dual sim", "sim 1", "sim 2", "dialpad sim", "two call buttons", "carrier buttons"),
                targetHeadline = context.getString(R.string.settings_interface_dual_sim_buttons_title),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_navigation_bar),
                supporting = context.getString(R.string.loc_floating_bar_style_navigation_dock_and_tab_layout),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Dock,
                keywords = listOf("navigation", "navbar", "bottom nav", "dock"),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_navigation_bar_style),
                supporting = context.getString(R.string.loc_choose_between_standard_floating_dock_or_minimal_pill),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Dock,
                keywords = listOf("nav bar style", "floating dock", "pill bar", "bottom bar style", "dock style"),
                targetHeadline = context.getString(R.string.settings_interface_nav_bar_style),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_floating_bar_roundness),
                supporting = context.getString(R.string.loc_adjust_corner_radius_of_the_floating_navigation_bar),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.RoundedCorner,
                keywords = listOf("floating bar roundness", "nav bar corners", "pill roundness"),
                targetHeadline = "Floating Bar Roundness",
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_navigation_buttons_tabs),
                supporting = context.getString(R.string.loc_customize_and_reorder_favorites_recents_contacts_dialpad_tabs),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Tune,
                keywords = listOf("nav tabs", "reorder tabs", "bottom tabs", "favorites tab", "recents tab", "contacts tab", "dialpad tab"),
                targetHeadline = "Navigation Buttons & Tabs",
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_icon_only_navigation_bar),
                supporting = context.getString(R.string.loc_hide_text_labels_on_bottom_navigation_bar_for_a_cleaner_look),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.VisibilityOff,
                keywords = listOf("icon only", "hide labels", "compact bar", "clean bar"),
                targetHeadline = context.getString(R.string.settings_interface_icon_only_bar),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_default_start_screen),
                supporting = context.getString(R.string.loc_choose_which_tab_opens_when_launching_rivo_phone),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Home,
                keywords = listOf("start screen", "default tab", "launch tab", "startup screen", "home tab"),
                targetHeadline = "Default Start Screen",
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_merge_favorites_with_contacts),
                supporting = context.getString(R.string.loc_display_starred_contacts_at_top_of_contacts_tab_instead_of_separate_ta),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Star,
                keywords = listOf("merge favorites", "starred contacts", "favorites tab", "combine contacts"),
                targetHeadline = context.getString(R.string.settings_interface_merge_favorites),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_avatars_contact_cards),
                supporting = context.getString(R.string.loc_contact_photos_initials_and_contact_card_styling),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.AccountCircle,
                keywords = listOf("avatar", "avatars", "contact cards", "profile picture"),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_avatar_shapes),
                supporting = context.getString(R.string.loc_choose_from_circle_squircle_hexagon_flower_cookie_more),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Category,
                keywords = listOf("avatar shapes", "squircle", "circle avatar", "hexagon", "cookie shape", "flower shape", "morph"),
                targetHeadline = context.getString(R.string.settings_interface_avatar_shape),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_show_contact_pictures),
                supporting = context.getString(R.string.loc_display_contact_photos_in_avatar_tiles_when_available),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Image,
                keywords = listOf("contact photo", "contact picture", "avatar image", "profile pic"),
                targetHeadline = context.getString(R.string.settings_interface_show_picture),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_colorful_avatars),
                supporting = context.getString(R.string.loc_tint_letter_avatars_with_unique_accent_colors_per_contact),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.ColorLens,
                keywords = listOf("colorful avatars", "colored initials", "avatar colors", "tinted avatars"),
                targetHeadline = context.getString(R.string.settings_interface_colorful_avatars),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_gradient_avatars),
                supporting = context.getString(R.string.loc_apply_dynamic_gradient_backdrops_to_contact_initial_avatars),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Gradient,
                keywords = listOf("gradient avatars", "gradient", "smooth colors", "avatar style"),
                targetHeadline = context.getString(R.string.settings_interface_gradient_avatars),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_typography),
                supporting = context.getString(R.string.loc_font_text_size_and_system_font),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.TextFields,
                keywords = listOf("font", "typeface", "text size", "typography", "system font", "serif", "monospace", "text scale"),
                onClick = { navigator.navigate(TypographyScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_call_screen_buttons),
                supporting = context.getString(R.string.loc_choose_the_position_of_each_in_call_button),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("call buttons", "rearrange", "button position", "call screen", "mute", "keypad", "hold"),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_hide_avatar_with_wallpaper),
                supporting = context.getString(R.string.loc_omit_avatar_circle_when_a_full_contact_background_wallpaper_is_set),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Wallpaper,
                keywords = listOf("hide avatar", "wallpaper avatar", "full wallpaper", "contact wallpaper"),
                targetHeadline = context.getString(R.string.settings_interface_hide_avatar_with_bg),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_sound_vibration_headline),
                supporting = context.getString(R.string.settings_sound_vibration_supporting),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                keywords = listOf("sound", "vibration", "volume"),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_dialpad_dtmf_tones),
                supporting = context.getString(R.string.loc_play_audible_audio_tones_when_tapping_keys_on_the_dialpad),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("dtmf", "dialpad sound", "keypad tones", "audio beep", "dial tones"),
                targetHeadline = context.getString(R.string.settings_sound_dtmf_tone),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_dialpad_vibration),
                supporting = context.getString(R.string.loc_haptic_vibration_feedback_on_every_dialpad_key_press),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Vibration,
                keywords = listOf("dialpad vibration", "keypad haptics", "haptic feedback", "vibrate dialpad"),
                targetHeadline = context.getString(R.string.settings_sound_dialpad_vibration),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_hold_0_for),
                supporting = context.getString(R.string.loc_long_press_key_0_on_dialpad_to_insert_international_prefix),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Add,
                keywords = listOf("hold 0", "plus prefix", "international code", "long press zero"),
                targetHeadline = context.getString(R.string.settings_sound_hold_zero_for_plus),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_vibrate_on_call_answer),
                supporting = context.getString(R.string.loc_short_haptic_pulse_when_outgoing_call_is_answered_by_recipient),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.PhoneInTalk,
                keywords = listOf("vibrate on answer", "vibrate connect", "call connect haptic", "answered call vibration"),
                targetHeadline = context.getString(R.string.settings_sound_vibrate_on_answer),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_vibrate_on_call_hangup),
                supporting = context.getString(R.string.loc_haptic_feedback_when_call_ends_or_remote_party_hangs_up),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.CallEnd,
                keywords = listOf("vibrate on hangup", "vibrate call end", "disconnect vibration", "end call haptic"),
                targetHeadline = context.getString(R.string.settings_sound_vibrate_on_hangup),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_haptic_feedback_on_scrolling),
                supporting = context.getString(R.string.loc_subtle_tactile_tick_when_scrolling_past_contact_section_headers),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("haptic scroll", "scroll haptics", "tactile tick", "section scroll vibration"),
                targetHeadline = context.getString(R.string.settings_sound_haptic_scroll),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_flip_to_silence),
                supporting = context.getString(R.string.loc_turn_phone_face_down_to_immediately_silence_incoming_ringtone),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.ScreenRotation,
                keywords = listOf("flip to silence", "flip phone", "face down mute", "flip gesture"),
                targetHeadline = context.getString(R.string.settings_sound_flip_to_silence),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_do_not_disturb_during_calls),
                supporting = context.getString(R.string.loc_automatically_suppress_notification_chimes_while_actively_in_a_call),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.DoNotDisturb,
                keywords = listOf("dnd during call", "silence notifications in call", "mute chimes in call"),
                targetHeadline = context.getString(R.string.settings_sound_dnd_during_calls),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_volume_squeeze_for_dnd),
                supporting = context.getString(R.string.loc_press_both_volume_keys_to_activate_do_not_disturb_mode),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.AutoMirrored.Outlined.VolumeDown,
                keywords = listOf("volume squeeze", "volume dnd", "squeeze gesture", "quick dnd"),
                targetHeadline = "Volume Squeeze for DND",
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_missed_call_notifications),
                supporting = context.getString(R.string.loc_show_status_bar_alerts_and_notification_cards_for_missed_calls),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.Notifications,
                keywords = listOf("missed call alerts", "missed call notification", "lock screen missed call"),
                targetHeadline = context.getString(R.string.settings_sound_missed_call_notifications),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_phone_ringtone_sound_settings),
                supporting = context.getString(R.string.loc_open_system_sound_settings_to_choose_phone_call_ringtone),
                category = context.getString(R.string.loc_personalization_display),
                icon = Icons.Outlined.MusicNote,
                keywords = listOf("ringtone", "call ringtone", "change ringtone", "phone sound", "system sounds"),
                targetHeadline = context.getString(R.string.settings_sound_ringtone_settings),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),

            // ==================== CALLING & BEHAVIOR ====================
            SettingSearchItem(
                title = context.getString(R.string.settings_call_settings_headline),
                supporting = context.getString(R.string.settings_call_settings_supporting),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.SimCard,
                keywords = listOf("calling accounts", "call settings", "sim accounts", "carrier"),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_default_sim_for_calling),
                supporting = context.getString(R.string.loc_select_default_sim_card_sim_1_sim_2_or_always_ask_before_call),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.SimCard,
                keywords = listOf("default sim", "sim 1", "sim 2", "always ask sim", "dual sim calling", "preferred sim"),
                targetHeadline = context.getString(R.string.settings_call_default_sim),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_calling_accounts),
                supporting = context.getString(R.string.loc_system_phone_carrier_accounts_and_sip_telecom_providers),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.ManageAccounts,
                keywords = listOf("calling accounts", "telecom", "carrier accounts", "sim accounts", "sip"),
                targetHeadline = context.getString(R.string.settings_call_calling_accounts),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_show_country_code),
                supporting = context.getString(R.string.loc_display_country_code_prefix_in_phone_numbers_throughout_the_app_and_ca),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Public,
                keywords = listOf("country code", "phone format", "international number", "hide country code", "show country code", "prefix", "number formatting"),
                targetHeadline = context.getString(R.string.settings_show_country_code),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_t9_dialing_search),
                supporting = context.getString(R.string.loc_search_contacts_by_spelling_names_with_dialpad_number_keys),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("t9", "t9 search", "dialpad search", "smart dial", "spell contact on dialpad"),
                targetHeadline = context.getString(R.string.settings_call_t9_dialing),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_auto_paste_from_clipboard),
                supporting = context.getString(R.string.loc_automatically_suggest_pasting_phone_numbers_copied_to_clipboard),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.ContentPaste,
                keywords = listOf("clipboard", "auto paste", "copied number", "paste dialpad", "clipboard banner"),
                targetHeadline = context.getString(R.string.settings_auto_paste_clipboard),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_proximity_sensor),
                supporting = context.getString(R.string.loc_turn_off_screen_when_phone_is_held_close_to_your_ear),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Sensors,
                keywords = listOf("proximity sensor", "screen off in call", "ear sensor", "blank screen call"),
                targetHeadline = context.getString(R.string.settings_call_proximity_sensor),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_auto_speakerphone_via_proximity),
                supporting = context.getString(R.string.loc_switch_between_earpiece_and_speaker_based_on_phone_distance),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.SpeakerPhone,
                keywords = listOf("auto speaker", "proximity speaker", "hands free", "auto switch speaker"),
                targetHeadline = context.getString(R.string.settings_call_auto_speaker_proximity),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_incoming_call_ui_style),
                supporting = context.getString(R.string.loc_choose_between_fullscreen_call_screen_or_heads_up_banner_notification),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Call,
                keywords = listOf("incoming call ui", "call banner", "heads up call", "fullscreen call", "floating call"),
                targetHeadline = context.getString(R.string.settings_call_incoming_ui),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_always_fullscreen_call_screen),
                supporting = context.getString(R.string.loc_launch_full_incoming_call_screen_even_when_other_apps_are_active),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Fullscreen,
                keywords = listOf("always fullscreen", "incoming call screen", "force fullscreen call"),
                targetHeadline = context.getString(R.string.settings_call_always_fullscreen),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_open_call_screen_on_answer),
                supporting = context.getString(R.string.loc_open_full_call_screen_when_answering_from_heads_up_notification),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.PhoneInTalk,
                keywords = listOf("open call screen", "heads up notification", "answer notification", "incoming call notification", "open call screen on answer", "answer heads up"),
                targetHeadline = context.getString(R.string.settings_open_call_screen_on_answer_title),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_pocket_mode),
                supporting = context.getString(R.string.loc_prevent_accidental_touches_and_maximize_ringtone_volume_inside_pocket),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.ScreenLockPortrait,
                keywords = listOf("pocket mode", "accidental touch", "pocket dial"),
                targetHeadline = "Pocket Mode",
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_call_log_history_limit),
                supporting = context.getString(R.string.loc_configure_maximum_retained_call_logs_500_1000_or_unlimited),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.History,
                keywords = listOf("call log limit", "call history retention", "max call logs", "delete old logs"),
                targetHeadline = context.getString(R.string.settings_call_log_limit),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_post_call_summary),
                supporting = context.getString(R.string.loc_display_call_duration_note_option_and_quick_actions_after_hanging_up),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.ContentCopy,
                keywords = listOf("post call summary", "call summary", "after call screen", "duration summary"),
                targetHeadline = "Post-Call Summary",
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_missed_call_card_title),
                supporting = context.getString(R.string.settings_missed_call_card_supporting),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.AutoMirrored.Outlined.PhoneMissed,
                keywords = listOf("missed call card", "missed call popup", "quick callback", "popup card", "instant card", "return call", "recents missed card"),
                targetHeadline = context.getString(R.string.settings_missed_call_card_title),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_floating_call_bubble),
                supporting = context.getString(R.string.settings_floating_call_bubble_supporting),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.PictureInPicture,
                keywords = listOf("floating call bubble", "floating bubble", "call bubble", "bubble", "overlay", "mini call", "picture in picture", "floating controls"),
                targetHeadline = context.getString(R.string.settings_floating_call_bubble),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_auto_redial),
                supporting = context.getString(R.string.loc_automatically_redial_busy_declined_or_unreachable_outgoing_calls),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Replay,
                keywords = listOf("auto redial", "redial busy", "call again", "retry call", "redial attempts"),
                targetHeadline = context.getString(R.string.settings_call_auto_redial),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_call_waiting),
                supporting = context.getString(R.string.loc_receive_alerts_for_incoming_calls_while_already_on_another_call),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Call,
                keywords = listOf("call waiting", "second call", "multiple calls", "carrier call waiting"),
                targetHeadline = context.getString(R.string.settings_call_waiting),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_default_call_screen_background),
                supporting = context.getString(R.string.loc_custom_photo_wallpaper_or_gradient_for_active_phone_calls),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Wallpaper,
                keywords = listOf("call background", "call wallpaper", "in call picture", "caller screen background"),
                targetHeadline = context.getString(R.string.settings_call_default_background),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_unknown_caller_background),
                supporting = context.getString(R.string.loc_specialized_wallpaper_for_numbers_not_in_your_address_book),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Image,
                keywords = listOf("unknown caller background", "unknown wallpaper", "stranger background"),
                targetHeadline = context.getString(R.string.settings_call_unknown_background),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_speed_dial),
                supporting = context.getString(R.string.loc_assign_contacts_to_dialpad_keys_1_9_for_quick_dialing),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Speed,
                keywords = listOf("speed dial", "fast dial", "quick call", "dialpad", "shortcut", "1-9"),
                onClick = { navigator.navigate(SpeedDialScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_quick_responses),
                supporting = context.getString(R.string.loc_predefined_sms_messages_to_decline_incoming_calls),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.AutoMirrored.Outlined.Message,
                keywords = listOf("quick response", "reject sms", "decline message", "canned message", "sms reply"),
                onClick = { navigator.navigate(QuickResponsesScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_swipe_actions_title),
                supporting = context.getString(R.string.settings_swipe_actions_supporting),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Swipe,
                keywords = listOf("swipe", "gesture", "swipe to call", "swipe to message", "left", "right"),
                onClick = { navigator.navigate(SwipeActionsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_swipe_right_action),
                supporting = context.getString(R.string.loc_set_action_when_swiping_contact_or_call_log_right_call_sms_etc),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.SwipeRight,
                keywords = listOf("swipe right", "swipe to call", "right gesture", "quick call swipe"),
                onClick = { navigator.navigate(SwipeActionsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_swipe_left_action),
                supporting = context.getString(R.string.loc_set_action_when_swiping_contact_or_call_log_left_message_details),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.SwipeLeft,
                keywords = listOf("swipe left", "swipe to message", "left gesture", "quick sms swipe"),
                onClick = { navigator.navigate(SwipeActionsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.call_recordings_title),
                supporting = context.getString(R.string.loc_auto_recording_shizuku_internal_audio_saved_recordings),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.FiberManualRecord,
                keywords = listOf("record", "recording", "call recording", "auto record", "shizuku", "audio", "folder", "m4a", "mp3"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_auto_record_calls),
                supporting = context.getString(R.string.loc_automatically_start_recording_as_soon_as_a_call_is_connected),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Mic,
                keywords = listOf("auto record", "record all calls", "automatic call recording", "record conversation"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_auto_record_filter),
                supporting = context.getString(R.string.loc_choose_to_auto_record_all_calls_non_contacts_or_specific_numbers),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.FilterList,
                keywords = listOf("record filter", "record strangers", "record non contacts", "recording whitelist"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_audio_recording_quality_bitrate),
                supporting = context.getString(R.string.loc_configure_audio_bitrate_64k_128k_192k_256k_and_format),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Tune,
                keywords = listOf("audio bitrate", "audio quality", "recording format", "m4a", "audio sample rate"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_minimum_duration_filter_for_recordings),
                supporting = context.getString(R.string.loc_automatically_discard_recordings_shorter_than_a_specified_duration),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Timer,
                keywords = listOf("minimum duration", "discard short recordings", "short calls", "skip brief calls"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_recording_storage_folder),
                supporting = context.getString(R.string.loc_select_custom_directory_on_device_storage_to_save_call_audio_files),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Folder,
                keywords = listOf("recording folder", "save directory", "saf tree uri", "recording path", "change folder"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_shizuku_internal_audio_recording),
                supporting = context.getString(R.string.loc_capture_pristine_two_way_system_call_audio_via_shizuku_service),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Terminal,
                keywords = listOf("shizuku", "internal audio", "system audio", "rootless adb", "two way audio", "clean recording"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_call_analytics_title),
                supporting = context.getString(R.string.settings_call_analytics_supporting),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Analytics,
                keywords = listOf("analytics", "statistics", "call stats", "call logs", "duration", "history"),
                onClick = { navigator.navigate(CallAnalyticsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.priority_contacts_title),
                supporting = context.getString(R.string.priority_contacts_supporting),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.NotificationImportant,
                keywords = listOf("priority", "favorite", "vip", "dnd", "star", "important contacts"),
                onClick = { navigator.navigate(PriorityContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_voicemail),
                supporting = context.getString(R.string.loc_carrier_voicemail_service_and_configuration),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Voicemail,
                keywords = listOf("voicemail", "carrier", "mail", "voice message"),
                onClick = { navigator.navigate(VoicemailScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_carrier_voicemail_number),
                supporting = context.getString(R.string.loc_configure_carrier_voicemail_phone_number_or_speed_dial_key_1),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("voicemail number", "dial voicemail", "carrier voicemail"),
                onClick = { navigator.navigate(VoicemailScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_voicemail_ringtone_vibration),
                supporting = context.getString(R.string.loc_customize_ringtone_chime_and_vibration_for_new_voicemails),
                category = context.getString(R.string.loc_calling_behavior),
                icon = Icons.Outlined.Notifications,
                keywords = listOf("voicemail ringtone", "voicemail sound", "voicemail vibration"),
                onClick = { navigator.navigate(VoicemailScreenDestination) }
            ),

            // ==================== CALL PROTECTION & SECURITY ====================
            SettingSearchItem(
                title = context.getString(R.string.loc_app_lock),
                supporting = if (prefs.isAppLockEnabled()) "Enabled (Face, Fingerprint, PIN)" else "Protect app with biometrics or PIN",
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Lock,
                keywords = listOf("app lock", "lock", "biometric", "fingerprint", "face", "pin", "password", "security", "protect"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_biometric_unlock),
                supporting = context.getString(R.string.loc_use_fingerprint_sensor_or_face_unlock_to_open_rivo_phone),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Fingerprint,
                keywords = listOf("biometric", "fingerprint", "face unlock", "biometric prompt", "auth"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_custom_app_pin),
                supporting = context.getString(R.string.loc_set_or_change_fallback_4_digit_security_pin_for_app_lock),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Password,
                keywords = listOf("app pin", "change pin", "4 digit pin", "passcode", "code"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_app_lock_timeout),
                supporting = context.getString(R.string.loc_choose_when_lock_triggers_immediately_30s_1m_5m_after_leaving_app),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Timer,
                keywords = listOf("lock timeout", "lock immediately", "lock delay", "auto lock time"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_private_storage),
                supporting = context.getString(R.string.loc_secret_dialpad_vault_stored_only_in_app_memory),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.FolderShared,
                keywords = listOf("private", "vault", "secret", "dialpad code", "hide contacts", "secure contacts"),
                onClick = { navigator.navigate(PrivateContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_secret_dialpad_vault_code),
                supporting = context.getString(R.string.loc_configure_secret_number_to_type_into_dialpad_to_unlock_vault),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("secret code", "vault code", "dialpad code", "unlock vault", "hidden vault"),
                onClick = { navigator.navigate(PrivateContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_hide_private_storage_from_settings),
                supporting = context.getString(R.string.loc_completely_remove_private_storage_menu_item_from_settings_list),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.VisibilityOff,
                keywords = listOf("hide vault", "stealth mode", "hide private storage", "conceal vault"),
                onClick = { navigator.navigate(PrivateContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_blocked_numbers_headline),
                supporting = context.getString(R.string.settings_blocked_numbers_supporting),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Block,
                keywords = listOf("block", "blocked", "blacklist", "spam", "reject", "filter calls"),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_auto_decline_unknown_numbers),
                supporting = context.getString(R.string.loc_automatically_reject_callers_with_private_or_hidden_caller_id),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.PersonOff,
                keywords = listOf("auto decline unknown", "block private numbers", "block hidden caller id", "reject unknown"),
                targetHeadline = context.getString(R.string.settings_auto_decline_unknown_title),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_auto_decline_non_contacts),
                supporting = context.getString(R.string.loc_block_all_incoming_calls_from_numbers_not_saved_in_your_contacts),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.GroupRemove,
                keywords = listOf("block non contacts", "stranger calls", "only allow contacts", "reject unknown callers"),
                targetHeadline = context.getString(R.string.settings_auto_decline_non_contacts_title),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_call_blocking_behavior),
                supporting = context.getString(R.string.loc_configure_how_blocked_calls_are_handled_hang_up_or_silence),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Tune,
                keywords = listOf("blocking method", "hang up blocked", "silence blocked", "drop call"),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_blocked_call_notifications),
                supporting = context.getString(R.string.loc_toggle_notifications_when_a_call_from_a_blocked_number_is_rejected),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.NotificationsOff,
                keywords = listOf("blocked notifications", "silent block", "hide blocked alerts"),
                targetHeadline = context.getString(R.string.settings_blocked_notifications),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_system_blocked_numbers_manager),
                supporting = context.getString(R.string.loc_open_android_system_dialer_blocked_list_to_sync_blocked_numbers),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Settings,
                keywords = listOf("system blocked numbers", "android blocklist", "carrier blocked numbers"),
                targetHeadline = context.getString(R.string.settings_blocked_system_button),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.fake_call_title),
                supporting = context.getString(R.string.fake_call_subtitle),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.AutoMirrored.Outlined.PhoneCallback,
                keywords = listOf("fake call", "prank", "schedule call", "timer", "simulate call"),
                onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_schedule_fake_call_timer),
                supporting = context.getString(R.string.loc_set_delay_timer_or_exact_time_to_trigger_an_incoming_fake_call),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Timer,
                keywords = listOf("fake call timer", "schedule prank call", "fake incoming call timer"),
                onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_fake_caller_details),
                supporting = context.getString(R.string.loc_customize_caller_name_phone_number_and_photo_for_fake_call),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Person,
                keywords = listOf("fake caller name", "fake caller number", "custom caller id prank"),
                onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_permissions_app_setup),
                supporting = context.getString(R.string.loc_review_granted_permissions_and_system_capabilities),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.VerifiedUser,
                keywords = listOf("permission", "setup", "default phone app", "default dialer", "checklist"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_default_phone_app_dialer_role),
                supporting = context.getString(R.string.loc_set_rivo_phone_as_the_primary_default_dialer_for_call_handling),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Phone,
                keywords = listOf("default phone app", "default dialer", "phone role", "set default dialer"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_shizuku_service_permission),
                supporting = context.getString(R.string.loc_verify_shizuku_service_status_for_internal_audio_call_recording),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Terminal,
                keywords = listOf("shizuku permission", "adb permissions", "shizuku status", "internal recording perm"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_open_app_system_settings),
                supporting = context.getString(R.string.loc_jump_directly_to_android_system_app_info_for_rivo_phone),
                category = context.getString(R.string.loc_call_protection_security),
                icon = Icons.Outlined.Settings,
                keywords = listOf("app info", "system settings", "android permissions", "storage permission"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),

            // ==================== CONTACTS & DATA ====================
            SettingSearchItem(
                title = context.getString(R.string.settings_contact_management_headline),
                supporting = context.getString(R.string.settings_contact_management_supporting),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.ManageAccounts,
                keywords = listOf("contact management", "duplicates", "merge contacts", "clean contacts", "sync"),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_merge_duplicate_contacts),
                supporting = context.getString(R.string.loc_detect_and_combine_contacts_sharing_identical_phone_numbers_or_names),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.ManageAccounts,
                keywords = listOf("merge contacts", "duplicate contacts", "combine contacts", "deduplicate", "clean contacts"),
                targetHeadline = context.getString(R.string.contact_management_duplicates_title),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_move_contacts_across_accounts),
                supporting = context.getString(R.string.loc_transfer_contacts_between_google_accounts_sim_cards_or_local_memory),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.SwapHoriz,
                keywords = listOf("move contacts", "transfer contacts", "copy contacts", "sim to google", "google to local"),
                targetHeadline = context.getString(R.string.contact_management_move_title),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_standardize_phone_numbers_e_164),
                supporting = context.getString(R.string.loc_format_phone_numbers_with_proper_country_code_and_spacing),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.FormatListNumbered,
                keywords = listOf("standardize numbers", "e164", "country code", "format numbers", "normalize phone numbers"),
                targetHeadline = context.getString(R.string.contact_management_standardize_numbers),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_contact_management_card_on_contacts_tab),
                supporting = context.getString(R.string.loc_show_quick_storage_duplicate_cleaner_card_at_top_of_contacts_list),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.ViewAgenda,
                keywords = listOf("contact management card", "contacts banner", "duplicate banner", "show cleaner card"),
                targetHeadline = context.getString(R.string.settings_contact_management_card),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_contacts_launcher_icon),
                supporting = context.getString(R.string.loc_add_standalone_contacts_shortcut_icon_to_your_home_screen_launcher),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.AppShortcut,
                keywords = listOf("contacts shortcut", "launcher icon", "standalone contacts app", "home screen icon"),
                targetHeadline = context.getString(R.string.settings_contacts_launcher_icon),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_manage_visibility),
                supporting = context.getString(R.string.settings_manage_visibility_supporting),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.Visibility,
                keywords = listOf("visibility", "accounts", "google contacts", "sim contacts", "filter contacts", "hide accounts"),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_sort_contacts_by_name),
                supporting = context.getString(R.string.loc_sort_address_book_list_by_first_name_or_last_name),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.AutoMirrored.Outlined.Sort,
                keywords = listOf("sort contacts", "first name", "last name", "alphabetical sort", "surname first"),
                targetHeadline = context.getString(R.string.settings_manage_sort_by),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_contact_name_format),
                supporting = context.getString(R.string.loc_display_contact_names_as_first_last_or_last_first),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.Badge,
                keywords = listOf("name format", "display name", "surname comma first", "first name first"),
                targetHeadline = context.getString(R.string.settings_manage_name_format),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_contact_section_headers),
                supporting = context.getString(R.string.loc_show_or_hide_alphabetical_a_z_letter_headers_in_contact_list),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.AutoMirrored.Outlined.Sort,
                keywords = listOf("section headers", "alphabet headers", "abc headers", "contact headers", "letter dividers"),
                targetHeadline = context.getString(R.string.settings_contact_section_headers),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_filter_accounts_contact_sources),
                supporting = context.getString(R.string.loc_show_or_hide_contacts_from_specific_google_accounts_sim_or_device_memo),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.FilterAlt,
                keywords = listOf("filter accounts", "hide google contacts", "hide sim contacts", "local memory contacts"),
                targetHeadline = "Filter Accounts & Contact Sources",
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_trash_contacts),
                supporting = context.getString(R.string.loc_view_and_restore_recently_deleted_contacts_within_30_days),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.Delete,
                keywords = listOf("trash", "deleted", "recycle bin", "restore contacts", "remove contacts"),
                onClick = { navigator.navigate(TrashContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_empty_contacts_trash),
                supporting = context.getString(R.string.loc_permanently_remove_all_deleted_contacts_currently_in_trash),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.DeleteForever,
                keywords = listOf("empty trash", "permanent delete", "clear trash", "purge contacts"),
                onClick = { navigator.navigate(TrashContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_backup_restore_headline),
                supporting = context.getString(R.string.settings_backup_restore_supporting),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.Backup,
                keywords = listOf("backup", "restore", "export vcf", "import vcard", "vcf", "contacts backup"),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_export_contacts_to_vcf_vcard),
                supporting = context.getString(R.string.loc_export_entire_address_book_to_standard_vcf_backup_file),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.FileDownload,
                keywords = listOf("export contacts", "vcf", "vcard", "backup file", "save contacts file"),
                targetHeadline = context.getString(R.string.settings_backup_export_contacts),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_import_contacts_from_vcf),
                supporting = context.getString(R.string.loc_restore_contacts_from_a_saved_vcf_vcard_file_into_your_account),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.FileUpload,
                keywords = listOf("import contacts", "import vcf", "restore vcf", "load vcard"),
                targetHeadline = context.getString(R.string.settings_backup_import_contacts),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_export_call_logs),
                supporting = context.getString(R.string.loc_back_up_entire_call_history_and_call_records_to_storage),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.HistoryEdu,
                keywords = listOf("export call logs", "backup call logs", "save call history", "export call records"),
                targetHeadline = context.getString(R.string.settings_backup_export_logs),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_import_call_logs),
                supporting = context.getString(R.string.loc_restore_call_history_from_a_previously_saved_backup_file),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.RestorePage,
                keywords = listOf("import call logs", "restore call logs", "load call history"),
                targetHeadline = context.getString(R.string.settings_backup_import_logs),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_export_app_settings),
                supporting = context.getString(R.string.loc_back_up_entire_app_preferences_themes_dialpad_style_and_configurations),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.FileUpload,
                keywords = listOf("backup settings", "export settings", "save settings", "app settings backup", "export preferences"),
                targetHeadline = context.getString(R.string.settings_backup_export_settings),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_restore_app_settings),
                supporting = context.getString(R.string.loc_restore_all_app_preferences_and_configurations_from_a_saved_backup_jso),
                category = context.getString(R.string.loc_contacts_data),
                icon = Icons.Outlined.Restore,
                keywords = listOf("restore settings", "import settings", "load settings", "restore preferences"),
                targetHeadline = context.getString(R.string.settings_backup_import_settings),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),

            // ==================== SUPPORT & ABOUT ====================
            SettingSearchItem(
                title = context.getString(R.string.settings_support_development),
                supporting = context.getString(R.string.settings_support_development_supporting),
                category = context.getString(R.string.loc_support_about),
                icon = Icons.Outlined.Favorite,
                keywords = listOf("support", "development", "donate", "patreon", "tip jar", "video ad", "pro"),
                onClick = { navigator.navigate(SupportDevelopmentScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_rate_google_play),
                supporting = context.getString(R.string.settings_rate_google_play_supporting),
                category = context.getString(R.string.loc_support_about),
                icon = Icons.Default.Star,
                keywords = listOf("rate", "review", "google play", "store", "feedback"),
                onClick = { openLink(context, PLAY_STORE_URL) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_about_rivo),
                supporting = context.getString(R.string.settings_about_rivo_supporting),
                category = context.getString(R.string.loc_support_about),
                icon = Icons.Outlined.Info,
                keywords = listOf("about", "version", "app info", "developer", "licenses", "github", "source code"),
                onClick = { navigator.navigate(AboutScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_check_for_updates),
                supporting = context.getString(R.string.loc_check_latest_github_releases_for_updates_to_rivo_phone),
                category = context.getString(R.string.loc_support_about),
                icon = Icons.Outlined.Update,
                keywords = listOf("update", "check updates", "latest version", "new release", "github release"),
                onClick = { openLink(context, "https://github.com/thegrinch47/RivoPhoneApp/releases") }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_source_code_github),
                supporting = context.getString(R.string.loc_view_source_code_report_issues_and_star_rivo_on_github),
                category = context.getString(R.string.loc_support_about),
                icon = Icons.Outlined.Code,
                keywords = listOf("source code", "github", "git", "open source", "repository"),
                onClick = { openLink(context, "https://github.com/thegrinch47/RivoPhoneApp") }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_discord_community),
                supporting = context.getString(R.string.loc_join_rivo_phone_discussions_and_feature_requests_on_discord),
                category = context.getString(R.string.loc_support_about),
                icon = Icons.Outlined.Forum,
                keywords = listOf("discord", "community", "chat", "feedback", "support group"),
                onClick = { openLink(context, "https://discord.gg/h9n8a2rFwF") }
            ),
            SettingSearchItem(
                title = context.getString(R.string.loc_contributors),
                supporting = context.getString(R.string.loc_people_who_helped_make_rivo_possible),
                category = context.getString(R.string.loc_support_about),
                icon = Icons.Outlined.People,
                keywords = listOf("contributors", "team", "credits", "translators", "developers"),
                onClick = { navigator.navigate(ContributorsScreenDestination) }
            )
        )
    }

    val filteredSearchItems = remember(searchQuery, searchItems) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            emptyList()
        } else {
            searchItems
                .distinctBy { it.title }
                .filter { itemMatchesExplicitQuery(it, q) }
                .sortedWith(
                    compareByDescending<SettingSearchItem> { scoreSearchItem(it, q) }
                        .thenBy { it.title }
                )
        }
    }

    val settingsHistory = remember(settingsState) { prefs.getSettingsHistory() }
    val recentItems = remember(settingsHistory, searchItems) {
        settingsHistory.mapNotNull { title -> searchItems.firstOrNull { it.title == title } }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = context.getString(R.string.settings_search_placeholder),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Surface(
                                        onClick = { searchQuery = "" },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = context.getString(R.string.loc_clear_search),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        Surface(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = context.getString(R.string.action_back),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                MenuTopAppBar(
                    text = context.getString(R.string.settings_title),
                    navigator = navigator,
                    actions = {
                        Surface(
                            onClick = { isSearchActive = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = context.getString(R.string.settings_search_placeholder),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        if (isSearchActive) {
            LazyColumn(
                state = searchListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (searchQuery.isBlank()) {
                    // MD3 Expressive Search Welcome Card
                    item {
                        RivoExpressiveCard(
                            modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(54.dp),
                                    shape = logoMorph,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shadowElevation = 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(R.string.loc_search_settings),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = context.getString(R.string.loc_jump_directly_to_any_customization_tool_or_preference),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Settings Search History / Recently Visited (Issue #264)
                    if (recentItems.isNotEmpty()) {
                        item {
                            RivoExpressiveGroup(
                                title = context.getString(R.string.loc_recently_visited),
                                icon = Icons.Outlined.History
                            ) {
                                recentItems.forEach { item ->
                                    item(key = "recent_${item.title}") {
                                        RivoListItem(
                                            headline = item.title,
                                            supporting = item.supporting.ifBlank { null },
                                            leadingIcon = item.icon,
                                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            onClick = {
                                                isSearchActive = false
                                                searchQuery = ""
                                                prefs.addSettingsHistory(item.title)
                                                SettingsHighlightManager.setHighlight(item.title, item.targetHeadline)
                                                item.onClick()
                                            }
                                        )
                                    }
                                }
                                item(key = "clear_history") {
                                    Surface(
                                        onClick = { prefs.clearSettingsHistory() },
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = context.getString(R.string.loc_clear_history),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Category Quick-Filter Chips
                    item {
                        Column(modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) {
                            Text(
                                text = context.getString(R.string.loc_categories),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                            val categories = listOf(
                                Triple("Personalization", Icons.Outlined.Palette, "Personalization & Display"),
                                Triple("Calling", Icons.Outlined.Phone, "Calling & Behavior"),
                                Triple("Security", Icons.Outlined.Security, "Call Protection & Security"),
                                Triple("Contacts", Icons.Outlined.ManageAccounts, "Contacts & Data"),
                                Triple("About", Icons.Outlined.Info, "Support & About")
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                items(categories) { (name, icon, fullCategory) ->
                                    Surface(
                                        onClick = { searchQuery = name },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Popular Settings Group
                    item {
                        RivoExpressiveGroup(
                            title = context.getString(R.string.loc_popular_settings),
                            icon = Icons.Default.Star
                        ) {
                            item {
                                RivoListItem(
                                    headline = context.getString(R.string.loc_theme_appearance),
                                    supporting = context.getString(R.string.loc_theme_mode_interface_appearance_and_visual_style),
                                    leadingIcon = Icons.Outlined.Palette,
                                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                        prefs.addSettingsHistory("Theme & Appearance")
                                        navigator.navigate(InterfaceScreenDestination)
                                    }
                                )
                            }
                            item {
                                RivoListItem(
                                    headline = context.getString(R.string.call_recordings_title),
                                    supporting = context.getString(R.string.loc_auto_recording_shizuku_internal_audio_saved_recordings),
                                    leadingIcon = Icons.Outlined.FiberManualRecord,
                                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                        prefs.addSettingsHistory("Call Recordings")
                                        navigator.navigate(CallRecordingsScreenDestination())
                                    }
                                )
                            }
                            item {
                                RivoListItem(
                                    headline = context.getString(R.string.loc_app_lock),
                                    supporting = context.getString(R.string.loc_protect_app_with_biometrics_or_pin),
                                    leadingIcon = Icons.Outlined.Lock,
                                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                        prefs.addSettingsHistory("App Lock")
                                        navigator.navigate(AppLockScreenDestination)
                                    }
                                )
                            }
                            item {
                                RivoListItem(
                                    headline = context.getString(R.string.loc_speed_dial),
                                    supporting = context.getString(R.string.loc_assign_contacts_to_dialpad_keys_1_9),
                                    leadingIcon = Icons.Outlined.Speed,
                                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                        prefs.addSettingsHistory("Speed Dial")
                                        navigator.navigate(SpeedDialScreenDestination)
                                    }
                                )
                            }
                        }
                    }
                } else if (filteredSearchItems.isEmpty()) {
                    // MD3 Expressive Empty State
                    item {
                        RivoExpressiveCard(
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = rememberRivoMorphShape(RivoMaterialShapes.Cookie9Sided, RivoMaterialShapes.Circle) { 0.2f },
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = context.getString(R.string.search_no_results_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.settings_search_no_results, searchQuery.trim()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Direct results without category headings or container boxes
                    items(
                        items = filteredSearchItems,
                        key = { it.title }
                    ) { item ->
                        RivoListItem(
                            headline = item.title,
                            supporting = item.supporting.ifBlank { null },
                            leadingIcon = item.icon,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                                prefs.addSettingsHistory(item.title)
                                SettingsHighlightManager.setHighlight(item.title, item.targetHeadline)
                                item.onClick()
                            }
                        )
                    }
                }
            }
        } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // App Info Banner
            item {
                RivoExpressiveCard(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { navigator.navigate(AboutScreenDestination) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = if (ProBrandingManager.isActive(prefs)) {
                                painterResource(R.drawable.pro_app_icon)
                            } else {
                                painterResource(R.drawable.logo)
                            },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .graphicsLayer {
                                    scaleX = 1.15f
                                    scaleY = 1.15f
                                }
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (ProBrandingManager.isActive(prefs)) {
                                        "Rivo Pro"
                                    } else {
                                        context.getString(R.string.about_app_display_name)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = context.getString(R.string.loc_v_appinfo_first),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = context.getString(R.string.settings_top_card_subtext),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 1. Personalization & Display
            item {
                RivoExpressiveGroup(
                    title = context.getString(R.string.loc_personalization_display),
                    icon = Icons.Outlined.Palette
                ) {
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.loc_theme_appearance),
                            supporting = context.getString(R.string.loc_theme_mode_interface_appearance_and_visual_style),
                            leadingIcon = Icons.Outlined.Palette,
                            onClick = {
                                prefs.addSettingsHistory("Theme & Appearance")
                                navigator.navigate(InterfaceScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.loc_navigation_bar),
                            supporting = context.getString(R.string.loc_floating_bar_style_navigation_dock_and_tab_layout),
                            leadingIcon = Icons.Outlined.Dock,
                            onClick = {
                                prefs.addSettingsHistory("Navigation Bar")
                                navigator.navigate(BottomNavScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.loc_avatars_contact_cards),
                            supporting = context.getString(R.string.loc_11_avatar_shapes_contact_photos_initials_cards),
                            leadingIcon = Icons.Outlined.AccountCircle,
                            onClick = {
                                prefs.addSettingsHistory("Avatars & Contact Cards")
                                navigator.navigate(AvatarSettingsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_sound_vibration_headline),
                            supporting = context.getString(R.string.settings_sound_vibration_supporting),
                            leadingIcon = Icons.AutoMirrored.Outlined.VolumeUp,
                            onClick = {
                                prefs.addSettingsHistory("Sound & Vibration")
                                navigator.navigate(SoundVibrationScreenDestination)
                            }
                        )
                    }
                }
            }

            // 2. Calling & Behavior
            item {
                RivoExpressiveGroup(
                    title = context.getString(R.string.loc_calling_behavior),
                    icon = Icons.Outlined.Phone
                ) {
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_call_settings_headline),
                            supporting = context.getString(R.string.settings_call_settings_supporting),
                            leadingIcon = Icons.Outlined.SimCard,
                            onClick = {
                                prefs.addSettingsHistory("Calling & SIM Accounts")
                                navigator.navigate(CallAccountsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_swipe_actions_title),
                            supporting = context.getString(R.string.settings_swipe_actions_supporting),
                            leadingIcon = Icons.Outlined.Swipe,
                            onClick = {
                                prefs.addSettingsHistory("Swipe Actions")
                                navigator.navigate(SwipeActionsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.call_recordings_title),
                            supporting = context.getString(R.string.loc_auto_recording_shizuku_internal_audio_saved_recordings),
                            leadingIcon = Icons.Outlined.FiberManualRecord,
                            onClick = {
                                prefs.addSettingsHistory("Call Recordings")
                                navigator.navigate(CallRecordingsScreenDestination())
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_call_analytics_title),
                            supporting = context.getString(R.string.settings_call_analytics_supporting),
                            leadingIcon = Icons.Outlined.Analytics,
                            onClick = {
                                prefs.addSettingsHistory("Call Analytics")
                                navigator.navigate(CallAnalyticsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.priority_contacts_title),
                            supporting = context.getString(R.string.priority_contacts_supporting),
                            leadingIcon = Icons.Outlined.NotificationImportant,
                            onClick = {
                                prefs.addSettingsHistory("Priority Contacts")
                                navigator.navigate(PriorityContactsScreenDestination)
                            }
                        )
                    }
                }
            }

            // 3. Call Protection & Security
            item {
                val appLockEnabled = remember(settingsState) { prefs.isAppLockEnabled() }
                RivoExpressiveGroup(
                    title = context.getString(R.string.loc_call_protection_security),
                    icon = Icons.Outlined.Security
                ) {
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.loc_app_lock),
                            supporting = if (appLockEnabled) "Enabled (Face, Fingerprint, PIN)" else "Protect app with biometrics or PIN",
                            leadingIcon = Icons.Outlined.Lock,
                            onClick = {
                                prefs.addSettingsHistory("App Lock")
                                navigator.navigate(AppLockScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.loc_private_storage),
                            supporting = context.getString(R.string.loc_secret_dialpad_vault_stored_only_in_app_memory),
                            leadingIcon = Icons.Outlined.FolderShared,
                            onClick = {
                                prefs.addSettingsHistory("Private Storage")
                                navigator.navigate(PrivateContactsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_blocked_numbers_headline),
                            supporting = context.getString(R.string.settings_blocked_numbers_supporting),
                            leadingIcon = Icons.Outlined.Block,
                            onClick = {
                                prefs.addSettingsHistory("Blocked Numbers")
                                navigator.navigate(BlockedNumbersScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.fake_call_title),
                            supporting = context.getString(R.string.fake_call_subtitle),
                            leadingIcon = Icons.AutoMirrored.Outlined.PhoneCallback,
                            onClick = {
                                prefs.addSettingsHistory("Fake Call Scheduler")
                                navigator.navigate(FakeCallSchedulerScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.loc_permissions_app_setup),
                            supporting = context.getString(R.string.loc_review_granted_permissions_and_system_capabilities),
                            leadingIcon = Icons.Outlined.VerifiedUser,
                            onClick = {
                                prefs.addSettingsHistory("Permissions & App Setup")
                                navigator.navigate(PermissionsChecklistScreenDestination)
                            }
                        )
                    }
                }
            }

            // 4. Contacts & Data
            item {
                val isContactManagementCardEnabled = remember(settingsState) { prefs.isContactManagementCardEnabled() }
                RivoExpressiveGroup(
                    title = context.getString(R.string.settings_contacts_management_title),
                    icon = Icons.Outlined.ManageAccounts
                ) {
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_contact_management_headline),
                            supporting = context.getString(R.string.settings_contact_management_supporting),
                            leadingIcon = Icons.Outlined.ManageAccounts,
                            onClick = { navigator.navigate(ContactManagementScreenDestination) }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = context.getString(R.string.settings_contact_management_card),
                            supporting = context.getString(R.string.settings_contact_management_card_supporting),
                            leadingIcon = Icons.Outlined.Info,
                            checked = isContactManagementCardEnabled,
                            onCheckedChange = { prefs.setContactManagementCardEnabled(it) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_manage_visibility),
                            supporting = context.getString(R.string.settings_manage_visibility_supporting),
                            leadingIcon = Icons.Outlined.Visibility,
                            onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_backup_restore_headline),
                            supporting = context.getString(R.string.settings_backup_restore_supporting),
                            leadingIcon = Icons.Outlined.Backup,
                            onClick = { navigator.navigate(BackupRestoreScreenDestination) }
                        )
                    }
                }
            }

            // 5. Support & About
            item {
                RivoExpressiveGroup(
                    title = context.getString(R.string.settings_group_support_about),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline
                ) {
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_support_development),
                            supporting = context.getString(R.string.settings_support_development_supporting),
                            leadingIcon = Icons.Outlined.Favorite,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = { navigator.navigate(SupportDevelopmentScreenDestination) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_rate_google_play),
                            supporting = context.getString(R.string.settings_rate_google_play_supporting),
                            leadingIcon = Icons.Default.Star,
                            onClick = { openLink(context, PLAY_STORE_URL) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = context.getString(R.string.settings_about_rivo),
                            supporting = context.getString(R.string.settings_about_rivo_supporting),
                            leadingIcon = Icons.Outlined.Info,
                            onClick = { navigator.navigate(AboutScreenDestination) }
                        )
                    }
                }
            }

            item {
                com.grinch.rivo4.view.components.ad.BannerAd()
            }

            item {
                Text(
                    text = context.getString(R.string.about_copyright),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
        }

        if (showDisableAdsDialog) {
            RivoDialog(
                onDismissRequest = { showDisableAdsDialog = false },
                title = context.getString(R.string.ads_disable_dialog_title),
                icon = Icons.Outlined.Favorite,
                confirmAction = RivoDialogAction(
                    label = context.getString(R.string.ads_disable_dialog_confirm),
                    onClick = {
                        enableAds = false
                        prefs.setBoolean(PreferenceManager.KEY_ENABLE_ADS, false)
                        showDisableAdsDialog = false
                    }
                ),
                dismissAction = RivoDialogAction(
                    label = context.getString(R.string.ads_disable_dialog_keep),
                    onClick = { showDisableAdsDialog = false }
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = context.getString(R.string.ads_disable_dialog_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            openLink(context, PATREON_URL)
                            showDisableAdsDialog = false
                        }
                    ) {
                        Icon(Icons.Outlined.VolunteerActivism, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(context.getString(R.string.patreon_prompt_confirm))
                    }
                }
            }
        }

    }
}

private data class SettingSearchItem(
    val title: String,
    val supporting: String,
    val category: String,
    val icon: ImageVector,
    val keywords: List<String> = emptyList(),
    val targetHeadline: String? = null,
    val onClick: () -> Unit
)

private fun extractWords(text: String): List<String> {
    return text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
}

private fun wordMatchesToken(targetWord: String, token: String, allowPrefix: Boolean): Boolean {
    if (targetWord == token) return true
    // Common plural / singular variations
    if (targetWord == "${token}s" || targetWord == "${token}es") return true
    if ("${targetWord}s" == token || "${targetWord}es" == token) return true
    // Prefix match for progressive typing, requiring at least 2 characters
    if (allowPrefix && token.length >= 2 && targetWord.startsWith(token)) return true
    return false
}

private fun itemMatchesExplicitQuery(item: SettingSearchItem, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isBlank()) return false
    val tokens = q.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return false

    val titleLower = item.title.lowercase()
    // 1. Direct phrase match in main title/heading
    if (titleLower.contains(q)) return true

    // 2. All tokens explicitly match words in main title/heading (e.g. "call log" matches "Call Logs")
    val titleWords = extractWords(titleLower)
    return tokens.all { token -> titleWords.any { wordMatchesToken(it, token, allowPrefix = true) } }
}

private fun scoreSearchItem(item: SettingSearchItem, query: String): Int {
    val q = query.trim().lowercase()
    val tokens = q.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
    val titleLower = item.title.lowercase()
    val titleWords = extractWords(titleLower)

    return when {
        titleLower == q -> 1000
        titleLower.startsWith(q) -> 900
        titleLower.contains(q) -> 850
        tokens.isNotEmpty() && tokens.all { token -> titleWords.any { it == token } } -> 800
        tokens.isNotEmpty() && tokens.all { token -> titleWords.any { wordMatchesToken(it, token, allowPrefix = true) } } -> 750
        else -> 100
    }
}
