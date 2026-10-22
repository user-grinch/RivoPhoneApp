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
import kotlinx.coroutines.delay
import com.grinch.rivo4.controller.util.getAppVersion
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.TipJarDialog
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
    var showTipJarDialog by remember { mutableStateOf(false) }
    val isSupporter = remember(settingsState) { prefs.isSupporter() }

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

val searchItems = remember(isSearchActive, isSupporter) {
        if (!isSearchActive) emptyList() else listOf(
            // ==================== PERSONALIZATION & DISPLAY ====================
            SettingSearchItem(
                title = "Theme & Appearance",
                supporting = "Theme mode, interface appearance and visual style",
                category = "Personalization & Display",
                icon = Icons.Outlined.Palette,
                keywords = listOf("theme", "appearance", "interface", "ui theme", "dark theme", "light theme"),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "Material You / Dynamic Colors",
                supporting = "Follow system wallpaper color palette and dynamic themes",
                category = "Personalization & Display",
                icon = Icons.Outlined.Palette,
                keywords = listOf("material you", "dynamic colors", "monet", "wallpaper color", "system theme", "palette"),
                targetHeadline = context.getString(R.string.settings_interface_material_you),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "AMOLED Dark Mode",
                supporting = "Pure pitch black background for OLED/AMOLED battery saving",
                category = "Personalization & Display",
                icon = Icons.Outlined.DarkMode,
                keywords = listOf("amoled", "oled", "pure black", "pitch black", "true black", "dark theme", "battery saver"),
                targetHeadline = context.getString(R.string.settings_interface_amoled),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "Card Roundness",
                supporting = "Adjust corner curvature from subtle to ultra rounded",
                category = "Personalization & Display",
                icon = Icons.Outlined.RoundedCorner,
                keywords = listOf("roundness", "corner radius", "curvature", "corners", "rounded cards", "shape"),
                targetHeadline = context.getString(R.string.settings_interface_card_roundness),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "Transition Animations",
                supporting = "Screen transition animations and motion effects",
                category = "Personalization & Display",
                icon = Icons.Outlined.Animation,
                keywords = listOf("animations", "motion", "transitions", "slide", "fade", "smooth"),
                targetHeadline = context.getString(R.string.settings_interface_transition_animation),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "Transition Animation Speed",
                supporting = "Adjust transition animation time scale across pages, tabs, and menus",
                category = "Personalization & Display",
                icon = Icons.Outlined.Speed,
                keywords = listOf("transition speed", "animation speed", "time scale", "duration", "fast animations", "slow animations"),
                targetHeadline = "Transition Animation Speed",
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "Predictive Back Gestures",
                supporting = "Interactive swipe-to-preview animations when navigating back",
                category = "Personalization & Display",
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                keywords = listOf("predictive back", "predictive back gestures", "disable predictive back", "back gesture", "back animation", "gesture navigation", "swipe back"),
                targetHeadline = context.getString(R.string.settings_predictive_back_title),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "Carrier Names in Recents",
                supporting = "Show or hide carrier and SIM card names in call history and recents",
                category = "Calling & SIM Accounts",
                icon = Icons.Outlined.SimCard,
                keywords = listOf("carrier", "carrier name", "recents carrier", "sim", "hide carrier", "show carrier", "call log carrier", "recents sim"),
                targetHeadline = context.getString(R.string.settings_show_carrier_in_recents),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Group Call Logs",
                supporting = "Merge repeated calls from the same contact into a single entry",
                category = "Calling & SIM Accounts",
                icon = Icons.AutoMirrored.Outlined.CallMerge,
                keywords = listOf("group call logs", "call log grouping", "merge calls", "call history", "recents grouping"),
                targetHeadline = context.getString(R.string.settings_group_call_logs),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Dialpad Dual SIM Buttons",
                supporting = "Show separate SIM 1 and SIM 2 call buttons directly on dialpad",
                category = "Personalization & Display",
                icon = Icons.Outlined.SimCard,
                keywords = listOf("dual sim", "sim 1", "sim 2", "dialpad sim", "two call buttons", "carrier buttons"),
                targetHeadline = context.getString(R.string.settings_interface_dual_sim_buttons_title),
                onClick = { navigator.navigate(InterfaceScreenDestination) }
            ),
            SettingSearchItem(
                title = "Navigation Bar",
                supporting = "Floating bar style, navigation dock and tab layout",
                category = "Personalization & Display",
                icon = Icons.Outlined.Dock,
                keywords = listOf("navigation", "navbar", "bottom nav", "dock"),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = "Navigation Bar Style",
                supporting = "Choose between Standard, Floating Dock, or Minimal Pill",
                category = "Personalization & Display",
                icon = Icons.Outlined.Dock,
                keywords = listOf("nav bar style", "floating dock", "pill bar", "bottom bar style", "dock style"),
                targetHeadline = context.getString(R.string.settings_interface_nav_bar_style),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = "Floating Bar Roundness",
                supporting = "Adjust corner radius of the floating navigation bar",
                category = "Personalization & Display",
                icon = Icons.Outlined.RoundedCorner,
                keywords = listOf("floating bar roundness", "nav bar corners", "pill roundness"),
                targetHeadline = "Floating Bar Roundness",
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = "Navigation Buttons & Tabs",
                supporting = "Customize and reorder Favorites, Recents, Contacts & Dialpad tabs",
                category = "Personalization & Display",
                icon = Icons.Outlined.Tune,
                keywords = listOf("nav tabs", "reorder tabs", "bottom tabs", "favorites tab", "recents tab", "contacts tab", "dialpad tab"),
                targetHeadline = "Navigation Buttons & Tabs",
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = "Icon-Only Navigation Bar",
                supporting = "Hide text labels on bottom navigation bar for a cleaner look",
                category = "Personalization & Display",
                icon = Icons.Outlined.VisibilityOff,
                keywords = listOf("icon only", "hide labels", "compact bar", "clean bar"),
                targetHeadline = context.getString(R.string.settings_interface_icon_only_bar),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = "Default Start Screen",
                supporting = "Choose which tab opens when launching Rivo Phone",
                category = "Personalization & Display",
                icon = Icons.Outlined.Home,
                keywords = listOf("start screen", "default tab", "launch tab", "startup screen", "home tab"),
                targetHeadline = "Default Start Screen",
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = "Merge Favorites with Contacts",
                supporting = "Display starred contacts at top of Contacts tab instead of separate tab",
                category = "Personalization & Display",
                icon = Icons.Outlined.Star,
                keywords = listOf("merge favorites", "starred contacts", "favorites tab", "combine contacts"),
                targetHeadline = context.getString(R.string.settings_interface_merge_favorites),
                onClick = { navigator.navigate(BottomNavScreenDestination) }
            ),
            SettingSearchItem(
                title = "Avatars & Contact Cards",
                supporting = "Contact photos, initials and contact card styling",
                category = "Personalization & Display",
                icon = Icons.Outlined.AccountCircle,
                keywords = listOf("avatar", "avatars", "contact cards", "profile picture"),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Avatar Shapes",
                supporting = "Choose from Circle, Squircle, Hexagon, Flower, Cookie & more",
                category = "Personalization & Display",
                icon = Icons.Outlined.Category,
                keywords = listOf("avatar shapes", "squircle", "circle avatar", "hexagon", "cookie shape", "flower shape", "morph"),
                targetHeadline = context.getString(R.string.settings_interface_avatar_shape),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Show Contact Pictures",
                supporting = "Display contact photos in avatar tiles when available",
                category = "Personalization & Display",
                icon = Icons.Outlined.Image,
                keywords = listOf("contact photo", "contact picture", "avatar image", "profile pic"),
                targetHeadline = context.getString(R.string.settings_interface_show_picture),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Colorful Avatars",
                supporting = "Tint letter avatars with unique accent colors per contact",
                category = "Personalization & Display",
                icon = Icons.Outlined.ColorLens,
                keywords = listOf("colorful avatars", "colored initials", "avatar colors", "tinted avatars"),
                targetHeadline = context.getString(R.string.settings_interface_colorful_avatars),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Gradient Avatars",
                supporting = "Apply dynamic gradient backdrops to contact initial avatars",
                category = "Personalization & Display",
                icon = Icons.Outlined.Gradient,
                keywords = listOf("gradient avatars", "gradient", "smooth colors", "avatar style"),
                targetHeadline = context.getString(R.string.settings_interface_gradient_avatars),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Typography",
                supporting = "Font, text size and system font",
                category = "Personalization & Display",
                icon = Icons.Outlined.TextFields,
                keywords = listOf("font", "typeface", "text size", "typography", "system font", "serif", "monospace", "text scale"),
                onClick = { navigator.navigate(TypographyScreenDestination) }
            ),
            SettingSearchItem(
                title = "Call Screen Buttons",
                supporting = "Choose the position of each in-call button",
                category = "Calling & SIM Accounts",
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("call buttons", "rearrange", "button position", "call screen", "mute", "keypad", "hold"),
                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Hide Avatar with Wallpaper",
                supporting = "Omit avatar circle when a full contact background wallpaper is set",
                category = "Personalization & Display",
                icon = Icons.Outlined.Wallpaper,
                keywords = listOf("hide avatar", "wallpaper avatar", "full wallpaper", "contact wallpaper"),
                targetHeadline = context.getString(R.string.settings_interface_hide_avatar_with_bg),
                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_sound_vibration_headline),
                supporting = context.getString(R.string.settings_sound_vibration_supporting),
                category = "Personalization & Display",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                keywords = listOf("sound", "vibration", "volume"),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Dialpad DTMF Tones",
                supporting = "Play audible audio tones when tapping keys on the dialpad",
                category = "Personalization & Display",
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("dtmf", "dialpad sound", "keypad tones", "audio beep", "dial tones"),
                targetHeadline = context.getString(R.string.settings_sound_dtmf_tone),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Dialpad Vibration",
                supporting = "Haptic vibration feedback on every dialpad key press",
                category = "Personalization & Display",
                icon = Icons.Outlined.Vibration,
                keywords = listOf("dialpad vibration", "keypad haptics", "haptic feedback", "vibrate dialpad"),
                targetHeadline = context.getString(R.string.settings_sound_dialpad_vibration),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Hold 0 for +",
                supporting = "Long press key 0 on dialpad to insert international prefix +",
                category = "Personalization & Display",
                icon = Icons.Outlined.Add,
                keywords = listOf("hold 0", "plus prefix", "international code", "long press zero"),
                targetHeadline = context.getString(R.string.settings_sound_hold_zero_for_plus),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Vibrate on Call Answer",
                supporting = "Short haptic pulse when outgoing call is answered by recipient",
                category = "Personalization & Display",
                icon = Icons.Outlined.PhoneInTalk,
                keywords = listOf("vibrate on answer", "vibrate connect", "call connect haptic", "answered call vibration"),
                targetHeadline = context.getString(R.string.settings_sound_vibrate_on_answer),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Vibrate on Call Hangup",
                supporting = "Haptic feedback when call ends or remote party hangs up",
                category = "Personalization & Display",
                icon = Icons.Outlined.CallEnd,
                keywords = listOf("vibrate on hangup", "vibrate call end", "disconnect vibration", "end call haptic"),
                targetHeadline = context.getString(R.string.settings_sound_vibrate_on_hangup),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Haptic Feedback on Scrolling",
                supporting = "Subtle tactile tick when scrolling past contact section headers",
                category = "Personalization & Display",
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("haptic scroll", "scroll haptics", "tactile tick", "section scroll vibration"),
                targetHeadline = context.getString(R.string.settings_sound_haptic_scroll),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Flip to Silence",
                supporting = "Turn phone face down to immediately silence incoming ringtone",
                category = "Personalization & Display",
                icon = Icons.Outlined.ScreenRotation,
                keywords = listOf("flip to silence", "flip phone", "face down mute", "flip gesture"),
                targetHeadline = context.getString(R.string.settings_sound_flip_to_silence),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Do Not Disturb During Calls",
                supporting = "Automatically suppress notification chimes while actively in a call",
                category = "Personalization & Display",
                icon = Icons.Outlined.DoNotDisturb,
                keywords = listOf("dnd during call", "silence notifications in call", "mute chimes in call"),
                targetHeadline = context.getString(R.string.settings_sound_dnd_during_calls),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Volume Squeeze for DND",
                supporting = "Press both volume keys to activate Do Not Disturb mode",
                category = "Personalization & Display",
                icon = Icons.AutoMirrored.Outlined.VolumeDown,
                keywords = listOf("volume squeeze", "volume dnd", "squeeze gesture", "quick dnd"),
                targetHeadline = "Volume Squeeze for DND",
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Missed Call Notifications",
                supporting = "Show status bar alerts and notification cards for missed calls",
                category = "Personalization & Display",
                icon = Icons.Outlined.Notifications,
                keywords = listOf("missed call alerts", "missed call notification", "lock screen missed call"),
                targetHeadline = context.getString(R.string.settings_sound_missed_call_notifications),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Phone Ringtone & Sound Settings",
                supporting = "Open system sound settings to choose phone call ringtone",
                category = "Personalization & Display",
                icon = Icons.Outlined.MusicNote,
                keywords = listOf("ringtone", "call ringtone", "change ringtone", "phone sound", "system sounds"),
                targetHeadline = context.getString(R.string.settings_sound_ringtone_settings),
                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
            ),

            // ==================== CALLING & BEHAVIOR ====================
            SettingSearchItem(
                title = context.getString(R.string.settings_call_settings_headline),
                supporting = context.getString(R.string.settings_call_settings_supporting),
                category = "Calling & Behavior",
                icon = Icons.Outlined.SimCard,
                keywords = listOf("calling accounts", "call settings", "sim accounts", "carrier"),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Default SIM for Calling",
                supporting = "Select default SIM card (SIM 1, SIM 2, or Always Ask before call)",
                category = "Calling & Behavior",
                icon = Icons.Outlined.SimCard,
                keywords = listOf("default sim", "sim 1", "sim 2", "always ask sim", "dual sim calling", "preferred sim"),
                targetHeadline = context.getString(R.string.settings_call_default_sim),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Calling Accounts",
                supporting = "System phone carrier accounts and SIP telecom providers",
                category = "Calling & Behavior",
                icon = Icons.Outlined.ManageAccounts,
                keywords = listOf("calling accounts", "telecom", "carrier accounts", "sim accounts", "sip"),
                targetHeadline = context.getString(R.string.settings_call_calling_accounts),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Show Country Code",
                supporting = "Display country code prefix in phone numbers throughout the app and call screen",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Public,
                keywords = listOf("country code", "phone format", "international number", "hide country code", "show country code", "prefix", "number formatting"),
                targetHeadline = context.getString(R.string.settings_show_country_code),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "T9 Dialing Search",
                supporting = "Search contacts by spelling names with dialpad number keys",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("t9", "t9 search", "dialpad search", "smart dial", "spell contact on dialpad"),
                targetHeadline = context.getString(R.string.settings_call_t9_dialing),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Auto-Paste from Clipboard",
                supporting = "Automatically suggest pasting phone numbers copied to clipboard",
                category = "Calling & Behavior",
                icon = Icons.Outlined.ContentPaste,
                keywords = listOf("clipboard", "auto paste", "copied number", "paste dialpad", "clipboard banner"),
                targetHeadline = context.getString(R.string.settings_auto_paste_clipboard),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Proximity Sensor",
                supporting = "Turn off screen when phone is held close to your ear",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Sensors,
                keywords = listOf("proximity sensor", "screen off in call", "ear sensor", "blank screen call"),
                targetHeadline = context.getString(R.string.settings_call_proximity_sensor),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Auto Speakerphone via Proximity",
                supporting = "Switch between earpiece and speaker based on phone distance",
                category = "Calling & Behavior",
                icon = Icons.Outlined.SpeakerPhone,
                keywords = listOf("auto speaker", "proximity speaker", "hands free", "auto switch speaker"),
                targetHeadline = context.getString(R.string.settings_call_auto_speaker_proximity),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Incoming Call UI Style",
                supporting = "Choose between Fullscreen call screen or Heads-up Banner notification",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Call,
                keywords = listOf("incoming call ui", "call banner", "heads up call", "fullscreen call", "floating call"),
                targetHeadline = context.getString(R.string.settings_call_incoming_ui),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Always Fullscreen Call Screen",
                supporting = "Launch full incoming call screen even when other apps are active",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Fullscreen,
                keywords = listOf("always fullscreen", "incoming call screen", "force fullscreen call"),
                targetHeadline = context.getString(R.string.settings_call_always_fullscreen),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Open Call Screen on Answer",
                supporting = "Open full call screen when answering from heads-up notification",
                category = "Calling & Behavior",
                icon = Icons.Outlined.PhoneInTalk,
                keywords = listOf("open call screen", "heads up notification", "answer notification", "incoming call notification", "open call screen on answer", "answer heads up"),
                targetHeadline = context.getString(R.string.settings_open_call_screen_on_answer_title),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Pocket Mode",
                supporting = "Prevent accidental touches and maximize ringtone volume inside pocket",
                category = "Calling & Behavior",
                icon = Icons.Outlined.ScreenLockPortrait,
                keywords = listOf("pocket mode", "accidental touch", "pocket dial"),
                targetHeadline = "Pocket Mode",
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Call Log History Limit",
                supporting = "Configure maximum retained call logs (500, 1000, or Unlimited)",
                category = "Calling & Behavior",
                icon = Icons.Outlined.History,
                keywords = listOf("call log limit", "call history retention", "max call logs", "delete old logs"),
                targetHeadline = context.getString(R.string.settings_call_log_limit),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Post-Call Summary",
                supporting = "Display call duration, note option, and quick actions after hanging up",
                category = "Calling & Behavior",
                icon = Icons.Outlined.ContentCopy,
                keywords = listOf("post call summary", "call summary", "after call screen", "duration summary"),
                targetHeadline = "Post-Call Summary",
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_missed_call_card_title),
                supporting = context.getString(R.string.settings_missed_call_card_supporting),
                category = "Calling & Behavior",
                icon = Icons.AutoMirrored.Outlined.PhoneMissed,
                keywords = listOf("missed call card", "missed call popup", "quick callback", "popup card", "instant card", "return call", "recents missed card"),
                targetHeadline = context.getString(R.string.settings_missed_call_card_title),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_floating_call_bubble),
                supporting = context.getString(R.string.settings_floating_call_bubble_supporting),
                category = "Calling & Behavior",
                icon = Icons.Outlined.PictureInPicture,
                keywords = listOf("floating call bubble", "floating bubble", "call bubble", "bubble", "overlay", "mini call", "picture in picture", "floating controls"),
                targetHeadline = context.getString(R.string.settings_floating_call_bubble),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Auto Redial",
                supporting = "Automatically redial busy, declined, or unreachable outgoing calls",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Replay,
                keywords = listOf("auto redial", "redial busy", "call again", "retry call", "redial attempts"),
                targetHeadline = context.getString(R.string.settings_call_auto_redial),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Call Waiting",
                supporting = "Receive alerts for incoming calls while already on another call",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Call,
                keywords = listOf("call waiting", "second call", "multiple calls", "carrier call waiting"),
                targetHeadline = context.getString(R.string.settings_call_waiting),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Default Call Screen Background",
                supporting = "Custom photo, wallpaper, or gradient for active phone calls",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Wallpaper,
                keywords = listOf("call background", "call wallpaper", "in call picture", "caller screen background"),
                targetHeadline = context.getString(R.string.settings_call_default_background),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Unknown Caller Background",
                supporting = "Specialized wallpaper for numbers not in your address book",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Image,
                keywords = listOf("unknown caller background", "unknown wallpaper", "stranger background"),
                targetHeadline = context.getString(R.string.settings_call_unknown_background),
                onClick = { navigator.navigate(CallAccountsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Speed Dial",
                supporting = "Assign contacts to dialpad keys 1-9 for quick dialing",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Speed,
                keywords = listOf("speed dial", "fast dial", "quick call", "dialpad", "shortcut", "1-9"),
                onClick = { navigator.navigate(SpeedDialScreenDestination) }
            ),
            SettingSearchItem(
                title = "Quick Responses",
                supporting = "Predefined SMS messages to decline incoming calls",
                category = "Calling & Behavior",
                icon = Icons.AutoMirrored.Outlined.Message,
                keywords = listOf("quick response", "reject sms", "decline message", "canned message", "sms reply"),
                onClick = { navigator.navigate(QuickResponsesScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_swipe_actions_title),
                supporting = context.getString(R.string.settings_swipe_actions_supporting),
                category = "Calling & Behavior",
                icon = Icons.Outlined.Swipe,
                keywords = listOf("swipe", "gesture", "swipe to call", "swipe to message", "left", "right"),
                onClick = { navigator.navigate(SwipeActionsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_call_log_customization_title),
                supporting = context.getString(R.string.settings_call_log_customization_supporting),
                category = "Calling & Behavior",
                icon = Icons.Outlined.History,
                keywords = listOf("call log", "call log actions", "click actions", "trailing button", "recents options", "call log customize"),
                onClick = { navigator.navigate(CallLogCustomizationScreenDestination) }
            ),
            SettingSearchItem(
                title = "Swipe Right Action",
                supporting = "Set action when swiping contact or call log right (Call, SMS, etc.)",
                category = "Calling & Behavior",
                icon = Icons.Outlined.SwipeRight,
                keywords = listOf("swipe right", "swipe to call", "right gesture", "quick call swipe"),
                onClick = { navigator.navigate(SwipeActionsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Swipe Left Action",
                supporting = "Set action when swiping contact or call log left (Message, Details)",
                category = "Calling & Behavior",
                icon = Icons.Outlined.SwipeLeft,
                keywords = listOf("swipe left", "swipe to message", "left gesture", "quick sms swipe"),
                onClick = { navigator.navigate(SwipeActionsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.call_recordings_title),
                supporting = "Auto-recording, Shizuku internal audio & saved recordings",
                category = "Calling & Behavior",
                icon = Icons.Outlined.FiberManualRecord,
                keywords = listOf("record", "recording", "call recording", "auto record", "shizuku", "audio", "folder", "m4a", "mp3"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = "Auto-Record Calls",
                supporting = "Automatically start recording as soon as a call is connected",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Mic,
                keywords = listOf("auto record", "record all calls", "automatic call recording", "record conversation"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = "Auto-Record Filter",
                supporting = "Choose to auto-record All Calls, Non-Contacts, or Specific Numbers",
                category = "Calling & Behavior",
                icon = Icons.Outlined.FilterList,
                keywords = listOf("record filter", "record strangers", "record non contacts", "recording whitelist"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = "Audio Recording Quality & Bitrate",
                supporting = "Configure audio bitrate (64k, 128k, 192k, 256k) and format",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Tune,
                keywords = listOf("audio bitrate", "audio quality", "recording format", "m4a", "audio sample rate"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = "Minimum Duration Filter for Recordings",
                supporting = "Automatically discard recordings shorter than a specified duration",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Timer,
                keywords = listOf("minimum duration", "discard short recordings", "short calls", "skip brief calls"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = "Recording Storage Folder",
                supporting = "Select custom directory on device storage to save call audio files",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Folder,
                keywords = listOf("recording folder", "save directory", "saf tree uri", "recording path", "change folder"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = "Shizuku Internal Audio Recording",
                supporting = "Capture pristine two-way system call audio via Shizuku service",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Terminal,
                keywords = listOf("shizuku", "internal audio", "system audio", "rootless adb", "two way audio", "clean recording"),
                onClick = { navigator.navigate(CallRecordingsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_call_analytics_title),
                supporting = context.getString(R.string.settings_call_analytics_supporting),
                category = "Calling & Behavior",
                icon = Icons.Outlined.Analytics,
                keywords = listOf("analytics", "statistics", "call stats", "call logs", "duration", "history"),
                onClick = { navigator.navigate(CallAnalyticsScreenDestination()) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.priority_contacts_title),
                supporting = context.getString(R.string.priority_contacts_supporting),
                category = "Calling & Behavior",
                icon = Icons.Outlined.NotificationImportant,
                keywords = listOf("priority", "favorite", "vip", "dnd", "star", "important contacts"),
                onClick = { navigator.navigate(PriorityContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Voicemail",
                supporting = "Carrier voicemail service and configuration",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Voicemail,
                keywords = listOf("voicemail", "carrier", "mail", "voice message"),
                onClick = { navigator.navigate(VoicemailScreenDestination) }
            ),
            SettingSearchItem(
                title = "Carrier Voicemail Number",
                supporting = "Configure carrier voicemail phone number or speed dial key 1",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("voicemail number", "dial voicemail", "carrier voicemail"),
                onClick = { navigator.navigate(VoicemailScreenDestination) }
            ),
            SettingSearchItem(
                title = "Voicemail Ringtone & Vibration",
                supporting = "Customize ringtone chime and vibration for new voicemails",
                category = "Calling & Behavior",
                icon = Icons.Outlined.Notifications,
                keywords = listOf("voicemail ringtone", "voicemail sound", "voicemail vibration"),
                onClick = { navigator.navigate(VoicemailScreenDestination) }
            ),

            // ==================== CALL PROTECTION & SECURITY ====================
            SettingSearchItem(
                title = "App Lock",
                supporting = if (prefs.isAppLockEnabled()) "Enabled (Face, Fingerprint, PIN)" else "Protect app with biometrics or PIN",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Lock,
                keywords = listOf("app lock", "lock", "biometric", "fingerprint", "face", "pin", "password", "security", "protect"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = "Biometric Unlock",
                supporting = "Use fingerprint sensor or face unlock to open Rivo Phone",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Fingerprint,
                keywords = listOf("biometric", "fingerprint", "face unlock", "biometric prompt", "auth"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = "Custom App PIN",
                supporting = "Set or change fallback 4-digit security PIN for App Lock",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Password,
                keywords = listOf("app pin", "change pin", "4 digit pin", "passcode", "code"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = "App Lock Timeout",
                supporting = "Choose when lock triggers (Immediately, 30s, 1m, 5m after leaving app)",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Timer,
                keywords = listOf("lock timeout", "lock immediately", "lock delay", "auto lock time"),
                onClick = { navigator.navigate(AppLockScreenDestination) }
            ),
            SettingSearchItem(
                title = "Private Storage",
                supporting = "Secret dialpad vault • Stored only in app memory",
                category = "Call Protection & Security",
                icon = Icons.Outlined.FolderShared,
                keywords = listOf("private", "vault", "secret", "dialpad code", "hide contacts", "secure contacts"),
                onClick = { navigator.navigate(PrivateContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Secret Dialpad Vault Code",
                supporting = "Configure secret number to type into dialpad to unlock vault",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Dialpad,
                keywords = listOf("secret code", "vault code", "dialpad code", "unlock vault", "hidden vault"),
                onClick = { navigator.navigate(PrivateContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Hide Private Storage from Settings",
                supporting = "Completely remove Private Storage menu item from Settings list",
                category = "Call Protection & Security",
                icon = Icons.Outlined.VisibilityOff,
                keywords = listOf("hide vault", "stealth mode", "hide private storage", "conceal vault"),
                onClick = { navigator.navigate(PrivateContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_blocked_numbers_headline),
                supporting = context.getString(R.string.settings_blocked_numbers_supporting),
                category = "Call Protection & Security",
                icon = Icons.Outlined.Block,
                keywords = listOf("block", "blocked", "blacklist", "spam", "reject", "filter calls"),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = "Auto-Decline Unknown Numbers",
                supporting = "Automatically reject callers with private or hidden caller ID",
                category = "Call Protection & Security",
                icon = Icons.Outlined.PersonOff,
                keywords = listOf("auto decline unknown", "block private numbers", "block hidden caller id", "reject unknown"),
                targetHeadline = context.getString(R.string.settings_auto_decline_unknown_title),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = "Auto-Decline Non-Contacts",
                supporting = "Block all incoming calls from numbers not saved in your contacts",
                category = "Call Protection & Security",
                icon = Icons.Outlined.GroupRemove,
                keywords = listOf("block non contacts", "stranger calls", "only allow contacts", "reject unknown callers"),
                targetHeadline = context.getString(R.string.settings_auto_decline_non_contacts_title),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = "Call Blocking Behavior",
                supporting = "Configure how blocked calls are handled (Hang up or Silence)",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Tune,
                keywords = listOf("blocking method", "hang up blocked", "silence blocked", "drop call"),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = "Blocked Call Notifications",
                supporting = "Toggle notifications when a call from a blocked number is rejected",
                category = "Call Protection & Security",
                icon = Icons.Outlined.NotificationsOff,
                keywords = listOf("blocked notifications", "silent block", "hide blocked alerts"),
                targetHeadline = context.getString(R.string.settings_blocked_notifications),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = "System Blocked Numbers Manager",
                supporting = "Open Android system dialer blocked list to sync blocked numbers",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Settings,
                keywords = listOf("system blocked numbers", "android blocklist", "carrier blocked numbers"),
                targetHeadline = context.getString(R.string.settings_blocked_system_button),
                onClick = { navigator.navigate(BlockedNumbersScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.fake_call_title),
                supporting = context.getString(R.string.fake_call_subtitle),
                category = "Call Protection & Security",
                icon = Icons.AutoMirrored.Outlined.PhoneCallback,
                keywords = listOf("fake call", "prank", "schedule call", "timer", "simulate call"),
                onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
            ),
            SettingSearchItem(
                title = "Schedule Fake Call Timer",
                supporting = "Set delay timer or exact time to trigger an incoming fake call",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Timer,
                keywords = listOf("fake call timer", "schedule prank call", "fake incoming call timer"),
                onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
            ),
            SettingSearchItem(
                title = "Fake Caller Details",
                supporting = "Customize caller name, phone number, and photo for fake call",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Person,
                keywords = listOf("fake caller name", "fake caller number", "custom caller id prank"),
                onClick = { navigator.navigate(FakeCallSchedulerScreenDestination) }
            ),
            SettingSearchItem(
                title = "Permissions & App Setup",
                supporting = "Review granted permissions and system capabilities",
                category = "Call Protection & Security",
                icon = Icons.Outlined.VerifiedUser,
                keywords = listOf("permission", "setup", "default phone app", "default dialer", "checklist"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),
            SettingSearchItem(
                title = "Default Phone App (Dialer Role)",
                supporting = "Set Rivo Phone as the primary default dialer for call handling",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Phone,
                keywords = listOf("default phone app", "default dialer", "phone role", "set default dialer"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),
            SettingSearchItem(
                title = "Shizuku Service Permission",
                supporting = "Verify Shizuku service status for internal audio call recording",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Terminal,
                keywords = listOf("shizuku permission", "adb permissions", "shizuku status", "internal recording perm"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),
            SettingSearchItem(
                title = "Open App System Settings",
                supporting = "Jump directly to Android system App Info for Rivo Phone",
                category = "Call Protection & Security",
                icon = Icons.Outlined.Settings,
                keywords = listOf("app info", "system settings", "android permissions", "storage permission"),
                onClick = { navigator.navigate(PermissionsChecklistScreenDestination) }
            ),

            // ==================== CONTACTS & DATA ====================
            SettingSearchItem(
                title = context.getString(R.string.settings_contact_management_headline),
                supporting = context.getString(R.string.settings_contact_management_supporting),
                category = "Contacts & Data",
                icon = Icons.Outlined.ManageAccounts,
                keywords = listOf("contact management", "duplicates", "merge contacts", "clean contacts", "sync"),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = "Merge Duplicate Contacts",
                supporting = "Detect and combine contacts sharing identical phone numbers or names",
                category = "Contacts & Data",
                icon = Icons.Outlined.ManageAccounts,
                keywords = listOf("merge contacts", "duplicate contacts", "combine contacts", "deduplicate", "clean contacts"),
                targetHeadline = context.getString(R.string.contact_management_duplicates_title),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = "Move Contacts Across Accounts",
                supporting = "Transfer contacts between Google accounts, SIM cards, or local memory",
                category = "Contacts & Data",
                icon = Icons.Outlined.SwapHoriz,
                keywords = listOf("move contacts", "transfer contacts", "copy contacts", "sim to google", "google to local"),
                targetHeadline = context.getString(R.string.contact_management_move_title),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = "Standardize Phone Numbers (E.164)",
                supporting = "Format phone numbers with proper country code and spacing",
                category = "Contacts & Data",
                icon = Icons.Outlined.FormatListNumbered,
                keywords = listOf("standardize numbers", "e164", "country code", "format numbers", "normalize phone numbers"),
                targetHeadline = context.getString(R.string.contact_management_standardize_numbers),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = "Contact Management Card on Contacts Tab",
                supporting = "Show quick storage & duplicate cleaner card at top of Contacts list",
                category = "Contacts & Data",
                icon = Icons.Outlined.ViewAgenda,
                keywords = listOf("contact management card", "contacts banner", "duplicate banner", "show cleaner card"),
                targetHeadline = context.getString(R.string.settings_contact_management_card),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = "Contacts Launcher Icon",
                supporting = "Add standalone 'Contacts' shortcut icon to your home screen launcher",
                category = "Contacts & Data",
                icon = Icons.Outlined.AppShortcut,
                keywords = listOf("contacts shortcut", "launcher icon", "standalone contacts app", "home screen icon"),
                targetHeadline = context.getString(R.string.settings_contacts_launcher_icon),
                onClick = { navigator.navigate(ContactManagementScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_manage_visibility),
                supporting = context.getString(R.string.settings_manage_visibility_supporting),
                category = "Contacts & Data",
                icon = Icons.Outlined.Visibility,
                keywords = listOf("visibility", "accounts", "google contacts", "sim contacts", "filter contacts", "hide accounts"),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = "Sort Contacts by Name",
                supporting = "Sort address book list by First Name or Last Name",
                category = "Contacts & Data",
                icon = Icons.AutoMirrored.Outlined.Sort,
                keywords = listOf("sort contacts", "first name", "last name", "alphabetical sort", "surname first"),
                targetHeadline = context.getString(R.string.settings_manage_sort_by),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = "Contact Name Format",
                supporting = "Display contact names as 'First Last' or 'Last, First'",
                category = "Contacts & Data",
                icon = Icons.Outlined.Badge,
                keywords = listOf("name format", "display name", "surname comma first", "first name first"),
                targetHeadline = context.getString(R.string.settings_manage_name_format),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = "Contact Section Headers",
                supporting = "Show or hide alphabetical A-Z letter headers in contact list",
                category = "Contacts & Data",
                icon = Icons.AutoMirrored.Outlined.Sort,
                keywords = listOf("section headers", "alphabet headers", "abc headers", "contact headers", "letter dividers"),
                targetHeadline = context.getString(R.string.settings_contact_section_headers),
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = "Filter Accounts & Contact Sources",
                supporting = "Show or hide contacts from specific Google accounts, SIM, or device memory",
                category = "Contacts & Data",
                icon = Icons.Outlined.FilterAlt,
                keywords = listOf("filter accounts", "hide google contacts", "hide sim contacts", "local memory contacts"),
                targetHeadline = "Filter Accounts & Contact Sources",
                onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
            ),
            SettingSearchItem(
                title = "Trash Contacts",
                supporting = "View and restore recently deleted contacts within 30 days",
                category = "Contacts & Data",
                icon = Icons.Outlined.Delete,
                keywords = listOf("trash", "deleted", "recycle bin", "restore contacts", "remove contacts"),
                onClick = { navigator.navigate(TrashContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = "Empty Contacts Trash",
                supporting = "Permanently remove all deleted contacts currently in Trash",
                category = "Contacts & Data",
                icon = Icons.Outlined.DeleteForever,
                keywords = listOf("empty trash", "permanent delete", "clear trash", "purge contacts"),
                onClick = { navigator.navigate(TrashContactsScreenDestination) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_backup_restore_headline),
                supporting = context.getString(R.string.settings_backup_restore_supporting),
                category = "Contacts & Data",
                icon = Icons.Outlined.Backup,
                keywords = listOf("backup", "restore", "export vcf", "import vcard", "vcf", "contacts backup"),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = "Export Contacts to VCF (vCard)",
                supporting = "Export entire address book to standard .vcf backup file",
                category = "Contacts & Data",
                icon = Icons.Outlined.FileDownload,
                keywords = listOf("export contacts", "vcf", "vcard", "backup file", "save contacts file"),
                targetHeadline = context.getString(R.string.settings_backup_export_contacts),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = "Import Contacts from VCF",
                supporting = "Restore contacts from a saved .vcf / vCard file into your account",
                category = "Contacts & Data",
                icon = Icons.Outlined.FileUpload,
                keywords = listOf("import contacts", "import vcf", "restore vcf", "load vcard"),
                targetHeadline = context.getString(R.string.settings_backup_import_contacts),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = "Export Call Logs",
                supporting = "Back up entire call history and call records to storage",
                category = "Contacts & Data",
                icon = Icons.Outlined.HistoryEdu,
                keywords = listOf("export call logs", "backup call logs", "save call history", "export call records"),
                targetHeadline = context.getString(R.string.settings_backup_export_logs),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = "Import Call Logs",
                supporting = "Restore call history from a previously saved backup file",
                category = "Contacts & Data",
                icon = Icons.Outlined.RestorePage,
                keywords = listOf("import call logs", "restore call logs", "load call history"),
                targetHeadline = context.getString(R.string.settings_backup_import_logs),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = "Export App Settings",
                supporting = "Back up entire app preferences, themes, dialpad style, and configurations to JSON",
                category = "Contacts & Data",
                icon = Icons.Outlined.FileUpload,
                keywords = listOf("backup settings", "export settings", "save settings", "app settings backup", "export preferences"),
                targetHeadline = context.getString(R.string.settings_backup_export_settings),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),
            SettingSearchItem(
                title = "Restore App Settings",
                supporting = "Restore all app preferences and configurations from a saved backup JSON file",
                category = "Contacts & Data",
                icon = Icons.Outlined.Restore,
                keywords = listOf("restore settings", "import settings", "load settings", "restore preferences"),
                targetHeadline = context.getString(R.string.settings_backup_import_settings),
                onClick = { navigator.navigate(BackupRestoreScreenDestination) }
            ),

            // ==================== SUPPORT & ABOUT ====================
            SettingSearchItem(
                title = if (isSupporter) "Rivo Supporter ⭐" else "Support Us",
                supporting = if (isSupporter) "Thank you for supporting Rivo!" else "Support development via Tip Jar or Patreon",
                category = "Support & About",
                icon = if (isSupporter) Icons.Outlined.Star else Icons.Outlined.Favorite,
                keywords = listOf("tip jar", "patreon", "donate", "supporter", "sponsor"),
                onClick = { showTipJarDialog = true }
            ),
            SettingSearchItem(
                title = "Display Banner Ads",
                supporting = "Toggle support banner ads on settings and screens",
                category = "Support & About",
                icon = Icons.Outlined.AdUnits,
                keywords = listOf("ads", "banner ads", "disable ads", "turn off ads", "advertisements"),
                onClick = {
                    if (enableAds) {
                        showDisableAdsDialog = true
                    } else {
                        enableAds = true
                        prefs.setBoolean(PreferenceManager.KEY_ENABLE_ADS, true)
                    }
                }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_rate_google_play),
                supporting = context.getString(R.string.settings_rate_google_play_supporting),
                category = "Support & About",
                icon = Icons.Default.Star,
                keywords = listOf("rate", "review", "google play", "store", "feedback"),
                onClick = { openLink(context, PLAY_STORE_URL) }
            ),
            SettingSearchItem(
                title = context.getString(R.string.settings_about_rivo),
                supporting = context.getString(R.string.settings_about_rivo_supporting),
                category = "Support & About",
                icon = Icons.Outlined.Info,
                keywords = listOf("about", "version", "app info", "developer", "licenses", "github", "source code"),
                onClick = { navigator.navigate(AboutScreenDestination) }
            ),
            SettingSearchItem(
                title = "Check for Updates",
                supporting = "Check latest GitHub releases for updates to Rivo Phone",
                category = "Support & About",
                icon = Icons.Outlined.Update,
                keywords = listOf("update", "check updates", "latest version", "new release", "github release"),
                onClick = { openLink(context, "https://github.com/thegrinch47/RivoPhoneApp/releases") }
            ),
            SettingSearchItem(
                title = "Source Code & GitHub",
                supporting = "View source code, report issues, and star Rivo on GitHub",
                category = "Support & About",
                icon = Icons.Outlined.Code,
                keywords = listOf("source code", "github", "git", "open source", "repository"),
                onClick = { openLink(context, "https://github.com/thegrinch47/RivoPhoneApp") }
            ),
            SettingSearchItem(
                title = "Discord Community",
                supporting = "Join Rivo Phone discussions and feature requests on Discord",
                category = "Support & About",
                icon = Icons.Outlined.Forum,
                keywords = listOf("discord", "community", "chat", "feedback", "support group"),
                onClick = { openLink(context, "https://discord.gg/h9n8a2rFwF") }
            ),
            SettingSearchItem(
                title = "Contributors",
                supporting = "People who helped make Rivo possible",
                category = "Support & About",
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
                                            text = stringResource(R.string.settings_search_placeholder),
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
                                                contentDescription = "Clear search",
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
                                    contentDescription = stringResource(R.string.action_back),
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
                    text = stringResource(R.string.settings_title),
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
                                    contentDescription = stringResource(R.string.settings_search_placeholder),
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
                                        text = "Search Settings",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Jump directly to any customization, tool, or preference",
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
                                title = "Recently Visited",
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
                                                text = "Clear History",
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
                                text = "CATEGORIES",
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
                            title = "Popular Settings",
                            icon = Icons.Default.Star
                        ) {
                            item {
                                RivoListItem(
                                    headline = "Theme & Appearance",
                                    supporting = "Theme mode, interface appearance and visual style",
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
                                    headline = stringResource(R.string.call_recordings_title),
                                    supporting = "Auto-recording, Shizuku internal audio & saved recordings",
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
                                    headline = "App Lock",
                                    supporting = "Protect app with biometrics or PIN",
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
                                    headline = "Speed Dial",
                                    supporting = "Assign contacts to dialpad keys 1-9",
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
                                    text = stringResource(R.string.search_no_results_title),
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
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = logoMorph,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 3.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                                Image(
                                    painter = painterResource(R.drawable.logo),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.about_app_display_name),
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
                                        text = "v${appInfo.first}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_top_card_subtext),
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
                    title = "Personalization & Display",
                    icon = Icons.Outlined.Palette
                ) {
                    item {
                        RivoListItem(
                            headline = "Theme & Appearance",
                            supporting = "Theme mode, interface appearance and visual style",
                            leadingIcon = Icons.Outlined.Palette,
                            onClick = {
                                prefs.addSettingsHistory("Theme & Appearance")
                                navigator.navigate(InterfaceScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = "Navigation Bar",
                            supporting = "Floating bar style, navigation dock and tab layout",
                            leadingIcon = Icons.Outlined.Dock,
                            onClick = {
                                prefs.addSettingsHistory("Navigation Bar")
                                navigator.navigate(BottomNavScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = "Avatars & Contact Cards",
                            supporting = "11 avatar shapes, contact photos, initials & cards",
                            leadingIcon = Icons.Outlined.AccountCircle,
                            onClick = {
                                prefs.addSettingsHistory("Avatars & Contact Cards")
                                navigator.navigate(AvatarSettingsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_sound_vibration_headline),
                            supporting = stringResource(R.string.settings_sound_vibration_supporting),
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
                    title = "Calling & Behavior",
                    icon = Icons.Outlined.Phone
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_settings_headline),
                            supporting = stringResource(R.string.settings_call_settings_supporting),
                            leadingIcon = Icons.Outlined.SimCard,
                            onClick = {
                                prefs.addSettingsHistory("Calling & SIM Accounts")
                                navigator.navigate(CallAccountsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_swipe_actions_title),
                            supporting = stringResource(R.string.settings_swipe_actions_supporting),
                            leadingIcon = Icons.Outlined.Swipe,
                            onClick = {
                                prefs.addSettingsHistory("Swipe Actions")
                                navigator.navigate(SwipeActionsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_log_customization_title),
                            supporting = stringResource(R.string.settings_call_log_customization_supporting),
                            leadingIcon = Icons.Outlined.History,
                            onClick = {
                                prefs.addSettingsHistory("Call Log Customization")
                                navigator.navigate(CallLogCustomizationScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.call_recordings_title),
                            supporting = "Auto-recording, Shizuku internal audio & saved recordings",
                            leadingIcon = Icons.Outlined.FiberManualRecord,
                            onClick = {
                                prefs.addSettingsHistory("Call Recordings")
                                navigator.navigate(CallRecordingsScreenDestination())
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_analytics_title),
                            supporting = stringResource(R.string.settings_call_analytics_supporting),
                            leadingIcon = Icons.Outlined.Analytics,
                            onClick = {
                                prefs.addSettingsHistory("Call Analytics")
                                navigator.navigate(CallAnalyticsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.priority_contacts_title),
                            supporting = stringResource(R.string.priority_contacts_supporting),
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
                    title = "Call Protection & Security",
                    icon = Icons.Outlined.Security
                ) {
                    item {
                        RivoListItem(
                            headline = "App Lock",
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
                            headline = "Private Storage",
                            supporting = "Secret dialpad vault • Stored only in app memory",
                            leadingIcon = Icons.Outlined.FolderShared,
                            onClick = {
                                prefs.addSettingsHistory("Private Storage")
                                navigator.navigate(PrivateContactsScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_blocked_numbers_headline),
                            supporting = stringResource(R.string.settings_blocked_numbers_supporting),
                            leadingIcon = Icons.Outlined.Block,
                            onClick = {
                                prefs.addSettingsHistory("Blocked Numbers")
                                navigator.navigate(BlockedNumbersScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.fake_call_title),
                            supporting = stringResource(R.string.fake_call_subtitle),
                            leadingIcon = Icons.AutoMirrored.Outlined.PhoneCallback,
                            onClick = {
                                prefs.addSettingsHistory("Fake Call Scheduler")
                                navigator.navigate(FakeCallSchedulerScreenDestination)
                            }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = "Permissions & App Setup",
                            supporting = "Review granted permissions and system capabilities",
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
                    title = stringResource(R.string.settings_contacts_management_title),
                    icon = Icons.Outlined.ManageAccounts
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_contact_management_headline),
                            supporting = stringResource(R.string.settings_contact_management_supporting),
                            leadingIcon = Icons.Outlined.ManageAccounts,
                            onClick = { navigator.navigate(ContactManagementScreenDestination) }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_contact_management_card),
                            supporting = stringResource(R.string.settings_contact_management_card_supporting),
                            leadingIcon = Icons.Outlined.Info,
                            checked = isContactManagementCardEnabled,
                            onCheckedChange = { prefs.setContactManagementCardEnabled(it) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_manage_visibility),
                            supporting = stringResource(R.string.settings_manage_visibility_supporting),
                            leadingIcon = Icons.Outlined.Visibility,
                            onClick = { navigator.navigate(ContactVisibilityScreenDestination) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_backup_restore_headline),
                            supporting = stringResource(R.string.settings_backup_restore_supporting),
                            leadingIcon = Icons.Outlined.Backup,
                            onClick = { navigator.navigate(BackupRestoreScreenDestination) }
                        )
                    }
                }
            }

            // 5. Support & About
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_group_support_about),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline
                ) {
                    if (IS_ADS_SUPPORTED) {
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_display_banner_ads),
                                supporting = stringResource(R.string.settings_display_banner_ads_supporting),
                                leadingIcon = Icons.Outlined.AdUnits,
                                checked = enableAds,
                                onCheckedChange = { checked ->
                                    if (!checked) {
                                        showDisableAdsDialog = true
                                    } else {
                                        enableAds = true
                                        prefs.setBoolean(PreferenceManager.KEY_ENABLE_ADS, true)
                                    }
                                }
                            )
                        }
                    }
                    item {
                        RivoListItem(
                            headline = if (isSupporter) "Rivo Supporter ⭐" else "Support Us",
                            supporting = if (isSupporter) "Thank you for supporting Rivo!" else "Support development via Tip Jar or Patreon",
                            leadingIcon = if (isSupporter) Icons.Outlined.Star else Icons.Outlined.Favorite,
                            onClick = { showTipJarDialog = true }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_rate_google_play),
                            supporting = stringResource(R.string.settings_rate_google_play_supporting),
                            leadingIcon = Icons.Default.Star,
                            onClick = { openLink(context, PLAY_STORE_URL) }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_about_rivo),
                            supporting = stringResource(R.string.settings_about_rivo_supporting),
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
                    text = stringResource(R.string.about_copyright),
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
                title = stringResource(R.string.ads_disable_dialog_title),
                icon = Icons.Outlined.Favorite,
                confirmAction = RivoDialogAction(
                    label = stringResource(R.string.ads_disable_dialog_confirm),
                    onClick = {
                        enableAds = false
                        prefs.setBoolean(PreferenceManager.KEY_ENABLE_ADS, false)
                        showDisableAdsDialog = false
                    }
                ),
                dismissAction = RivoDialogAction(
                    label = stringResource(R.string.ads_disable_dialog_keep),
                    onClick = { showDisableAdsDialog = false }
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.ads_disable_dialog_body),
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
                        Text(stringResource(R.string.patreon_prompt_confirm))
                    }
                }
            }
        }

        if (showTipJarDialog) {
            TipJarDialog(onDismissRequest = { showTipJarDialog = false })
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
