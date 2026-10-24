package com.grinch.rivo4

import android.provider.CallLog
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.CallLogFilter
import com.grinch.rivo4.view.screen.RivoCallButtonOrder
import com.grinch.rivo4.view.theme.RivoPaletteStyle
import com.grinch.rivo4.view.theme.rivoFontFamilyFromId
import com.grinch.rivo4.view.theme.rivoPaletteStyleFromId
import com.grinch.rivo4.view.theme.rivoPaletteSwatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AuditRegressionTest {

    // region Call screen button order

    @Test
    fun callButtonOrderParseAlwaysProducesAllAvailableButtons() {
        val inputs = listOf(
            null,
            "",
            "   ",
            "bogus",
            "mute,mute,keypad",
            "hold,mute",
            "record,unknown,audio,,add"
        )
        for (input in inputs) {
            val parsed = RivoCallButtonOrder.parse(input)
            val expectedSize = 6
            assertEquals("parse($input) must keep expected buttons", expectedSize, parsed.size)
            assertEquals("parse($input) must not contain duplicates", expectedSize, parsed.toSet().size)
            assertTrue("parse($input) must contain every default button", parsed.containsAll(RivoCallButtonOrder.All.take(6)))
        }
    }

    @Test
    fun callButtonOrderParsePreservesSavedOrderFirst() {
        assertEquals(
            listOf(
                RivoCallButtonOrder.RECORD,
                RivoCallButtonOrder.AUDIO,
                RivoCallButtonOrder.MUTE,
                RivoCallButtonOrder.KEYPAD,
                RivoCallButtonOrder.HOLD,
                RivoCallButtonOrder.ADD
            ),
            RivoCallButtonOrder.parse("record,audio")
        )
        assertEquals(RivoCallButtonOrder.All, RivoCallButtonOrder.parse(null))
        assertEquals(RivoCallButtonOrder.All, RivoCallButtonOrder.parse(""))
    }

    @Test
    fun callButtonOrderDefaultPreferenceMatchesEnum() {
        // Fresh installs intentionally show the six core buttons; optional buttons can be enabled.
        assertEquals(
            listOf(
                RivoCallButtonOrder.MUTE,
                RivoCallButtonOrder.KEYPAD,
                RivoCallButtonOrder.AUDIO,
                RivoCallButtonOrder.RECORD,
                RivoCallButtonOrder.HOLD,
                RivoCallButtonOrder.ADD
            ),
            RivoCallButtonOrder.parse(PreferenceManager.DEFAULT_CALL_BUTTON_ORDER)
        )
    }

    @Test
    fun callButtonOrderFormatRoundTripsThroughParse() {
        val custom = listOf(
            RivoCallButtonOrder.HOLD,
            RivoCallButtonOrder.MUTE,
            RivoCallButtonOrder.KEYPAD,
            RivoCallButtonOrder.AUDIO,
            RivoCallButtonOrder.RECORD,
            RivoCallButtonOrder.ADD
        )
        val formatted = RivoCallButtonOrder.format(custom)
        assertEquals("hold,mute,keypad,audio,record,add", formatted)
        assertEquals(custom, RivoCallButtonOrder.parse(formatted))

        // Formatting a malformed list must sanitize it rather than persist junk
        assertEquals(6, RivoCallButtonOrder.parse(RivoCallButtonOrder.format(listOf("junk", RivoCallButtonOrder.MUTE))).size)
        assertEquals(RivoCallButtonOrder.All.joinToString(","), RivoCallButtonOrder.format(RivoCallButtonOrder.All))
    }

    @Test
    fun callButtonOrderParseRespectsHiddenButtons() {
        val hidden = RivoCallButtonOrder.hiddenSet("keypad,record,bogus")
        assertEquals(setOf("keypad", "record"), hidden)

        val parsed = RivoCallButtonOrder.parse(null, hidden)
        assertEquals(listOf("mute", "audio", "hold", "add"), parsed)

        // Hiding every button (e.g. stale pref restored on another device) must fall back to the full grid
        val allHidden = RivoCallButtonOrder.hiddenSet(RivoCallButtonOrder.All.joinToString(","))
        assertEquals(RivoCallButtonOrder.All, RivoCallButtonOrder.parse(null, allHidden))
        // The fallback keeps the user's saved order rather than the canonical one
        assertEquals(RivoCallButtonOrder.parse("hold,mute"), RivoCallButtonOrder.parse("hold,mute", allHidden))

        // format() round-trips must keep hidden ids so toggling visibility does not reorder the grid
        val orderWithHidden = RivoCallButtonOrder.parse("hold,mute", hidden)
        assertEquals(orderWithHidden, RivoCallButtonOrder.parse(RivoCallButtonOrder.format(orderWithHidden), hidden))
    }

    @Test
    fun fontFamilyListIsUniqueAndSane() {
        val families = PreferenceManager.ALL_FONT_FAMILIES
        assertEquals("Font family ids must be unique", families.size, families.toSet().size)
        assertTrue(families.contains(PreferenceManager.FONT_FAMILY_SANS_SERIF))
        // Unknown or legacy stored ids must resolve to the default sans serif family
        assertEquals(androidx.compose.ui.text.font.FontFamily.SansSerif, rivoFontFamilyFromId("does_not_exist"))
        assertEquals(androidx.compose.ui.text.font.FontFamily.Serif, rivoFontFamilyFromId(PreferenceManager.FONT_FAMILY_SERIF))
        // The hidden-buttons pref must export an empty default so fresh backups stay clean
        assertEquals("", PreferenceManager.getKnownDefaultStrings()[PreferenceManager.KEY_HIDDEN_CALL_BUTTONS])
    }

    // endregion

    // region Dynamic tonal palette styles

    @Test
    fun paletteStyleIdsAreUniqueAndStable() {
        val ids = RivoPaletteStyle.entries.map { it.id }
        assertEquals("Palette ids must be unique", ids.size, ids.toSet().size)
        assertTrue("Palette ids must not be blank", ids.all { it.isNotBlank() })
        // Backups and persisted prefs store these ids; renaming one would silently reset users
        val expected = setOf(
            "tonal_spot", "expressive", "neutral", "vibrant", "fruit_salad", "rainbow",
            "content", "fidelity", "monochrome", "big_clock", "candy", "deep_ocean", "sunset_glow"
        )
        assertEquals(expected, ids.toSet())
    }

    @Test
    fun paletteStyleLookupFallsBackToTonalSpot() {
        assertEquals(RivoPaletteStyle.Expressive, rivoPaletteStyleFromId("expressive"))
        assertEquals(RivoPaletteStyle.SunsetGlow, rivoPaletteStyleFromId("sunset_glow"))
        assertEquals(RivoPaletteStyle.TonalSpot, rivoPaletteStyleFromId(null))
        assertEquals(RivoPaletteStyle.TonalSpot, rivoPaletteStyleFromId("does_not_exist"))
    }

    @Test
    fun paletteResolveHueStaysWithinValidDegreeRange() {
        for (style in RivoPaletteStyle.entries) {
            for (seedHue in floatArrayOf(0f, 45f, 90f, 180f, 270f, 359.9f)) {
                val resolved = style.resolveHue(seedHue)
                assertTrue(
                    "resolveHue($style, $seedHue) = $resolved must be in [0, 360)",
                    resolved >= 0f && resolved < 360f
                )
            }
        }
    }

    @Test
    fun monochromePaletteProducesGrayscaleSwatches() {
        val swatches = rivoPaletteSwatches(0xFF6750A4.toInt(), RivoPaletteStyle.Monochrome, darkTheme = false)
        assertEquals(3, swatches.size)
        for (c in swatches) {
            assertTrue("Monochrome must be near-gray", abs(c.red - c.green) < 0.01f && abs(c.green - c.blue) < 0.01f)
        }
    }

    @Test
    fun paletteStylesProduceDistinctSwatches() {
        val seed = 0xFF6750A4.toInt()
        val tonalSpot = rivoPaletteSwatches(seed, RivoPaletteStyle.TonalSpot, darkTheme = false)
        val vibrant = rivoPaletteSwatches(seed, RivoPaletteStyle.Vibrant, darkTheme = false)
        val monochrome = rivoPaletteSwatches(seed, RivoPaletteStyle.Monochrome, darkTheme = false)
        assertFalse("Vibrant primary must differ from Tonal Spot primary", tonalSpot[0] == vibrant[0])
        assertFalse("Monochrome primary must differ from Tonal Spot primary", tonalSpot[0] == monochrome[0])
    }

    // endregion

    // region Preference defaults consistency

    @Test
    fun knownDefaultMapsNeverContainExcludedKeys() {
        val allDefaultKeys = buildSet {
            addAll(PreferenceManager.getKnownDefaultBooleans().keys)
            addAll(PreferenceManager.getKnownDefaultIntegers().keys)
            addAll(PreferenceManager.getKnownDefaultStrings().keys)
            addAll(PreferenceManager.getKnownDefaultStringSets().keys)
        }
        val leaked = allDefaultKeys.filter { PreferenceManager.isExcludedKey(it) }
        assertTrue("Excluded keys must not appear in known defaults: $leaked", leaked.isEmpty())
    }

    @Test
    fun groupCallLogsToggleIsEnabledByDefault() {
        assertEquals(true, PreferenceManager.getKnownDefaultBooleans()[PreferenceManager.KEY_GROUP_CALL_LOGS])
    }

    @Test
    fun bottomNavDefaultsCoverEveryTabExactlyOnce() {
        val order = PreferenceManager.DEFAULT_BOTTOM_NAV_ORDER
        assertEquals("Bottom nav default must list each tab once", order.size, order.toSet().size)
        val allTabs = setOf(
            PreferenceManager.TAB_RECENTS,
            PreferenceManager.TAB_FAVORITES,
            PreferenceManager.TAB_CONTACTS,
            PreferenceManager.TAB_RECORDINGS
        )
        assertEquals(allTabs, order.toSet())
    }

    @Test
    fun quickResponseDefaultsAreAllNonBlank() {
        assertTrue(PreferenceManager.DEFAULT_QUICK_RESPONSES.isNotEmpty())
        assertTrue(PreferenceManager.DEFAULT_QUICK_RESPONSES.all { it.isNotBlank() })
        assertEquals(
            "Stored default string must round-trip through the ||| separator",
            PreferenceManager.DEFAULT_QUICK_RESPONSES,
            PreferenceManager.DEFAULT_QUICK_RESPONSES.joinToString("|||").split("|||")
        )
    }

    // endregion

    // region Call log model & filters

    @Test
    fun callLogEntryCountReflectsGrouping() {
        val base = CallLogEntry(
            id = 1L,
            number = "+15550001",
            name = "Alice",
            type = CallLog.Calls.INCOMING_TYPE,
            date = 1000L,
            duration = 30L,
            photoUri = null,
            contactId = "7"
        )
        assertEquals("Ungrouped entry counts as 1", 1, base.count)

        val grouped = base.copy(
            types = listOf(CallLog.Calls.INCOMING_TYPE, CallLog.Calls.MISSED_TYPE, CallLog.Calls.OUTGOING_TYPE)
        )
        assertEquals(3, grouped.count)
    }

    @Test
    fun callLogFilterMissedCoversRejectedButNotVoicemailOrBlocked() {
        fun entry(type: Int) = CallLogEntry(
            id = type.toLong(),
            number = "+15550002",
            name = null,
            type = type,
            date = 1000L,
            duration = 0L,
            photoUri = null,
            contactId = null
        )

        assertTrue(CallLogFilter.Missed.matches(entry(CallLog.Calls.MISSED_TYPE)))
        assertTrue(CallLogFilter.Missed.matches(entry(CallLog.Calls.REJECTED_TYPE)))
        assertFalse(CallLogFilter.Missed.matches(entry(CallLog.Calls.VOICEMAIL_TYPE)))
        assertFalse(CallLogFilter.Missed.matches(entry(CallLog.Calls.INCOMING_TYPE)))

        // Voicemail and answered-externally calls are only visible under All
        assertTrue(CallLogFilter.All.matches(entry(CallLog.Calls.VOICEMAIL_TYPE)))
        assertFalse(CallLogFilter.Incoming.matches(entry(CallLog.Calls.VOICEMAIL_TYPE)))
        assertFalse(CallLogFilter.Outgoing.matches(entry(CallLog.Calls.VOICEMAIL_TYPE)))
    }

    // endregion
}
