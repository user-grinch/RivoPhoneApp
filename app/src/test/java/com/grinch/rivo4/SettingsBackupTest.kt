package com.grinch.rivo4

import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.data.SettingsBackupEnvelope
import com.grinch.rivo4.modal.data.SettingsData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testSerializationRoundTrip() {
        val original = SettingsBackupEnvelope(
            app = "RivoPhone",
            backupType = "settings",
            schemaVersion = 1,
            appVersion = "2.2",
            appVersionCode = 2026092011L,
            createdAt = 1729485100000L,
            settings = SettingsData(
                booleans = mapOf(
                    PreferenceManager.KEY_DYNAMIC_COLORS to true,
                    PreferenceManager.KEY_AMOLED_MODE to true
                ),
                integers = mapOf(
                    PreferenceManager.KEY_DIALPAD_STYLE to 2,
                    PreferenceManager.KEY_CARD_ROUNDNESS to 28
                ),
                strings = mapOf(
                    PreferenceManager.KEY_SECRET_DIALPAD_CODE to "*#1234#"
                ),
                stringSets = mapOf(
                    PreferenceManager.KEY_PRIORITY_CONTACTS to setOf("123", "456")
                )
            )
        )

        val jsonString = json.encodeToString(original)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("RivoPhone"))
        assertTrue(jsonString.contains("dynamic_colors"))

        val decoded = json.decodeFromString<SettingsBackupEnvelope>(jsonString)
        assertEquals(original.app, decoded.app)
        assertEquals(original.schemaVersion, decoded.schemaVersion)
        assertEquals(original.appVersionCode, decoded.appVersionCode)
        assertEquals(original.settings.booleans, decoded.settings.booleans)
        assertEquals(original.settings.integers, decoded.settings.integers)
        assertEquals(original.settings.strings, decoded.settings.strings)
        assertEquals(original.settings.stringSets, decoded.settings.stringSets)
    }

    @Test
    fun testBackwardCompatibilityWithMinimalOldBackup() {
        // Minimal older format with only top-level fields and empty/missing structures
        val oldJson = """
            {
                "app": "RivoPhone",
                "schemaVersion": 1,
                "settings": {
                    "booleans": {
                        "dynamic_colors": false
                    }
                }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<SettingsBackupEnvelope>(oldJson)
        assertEquals("RivoPhone", decoded.app)
        assertEquals(1, decoded.schemaVersion)
        assertEquals(false, decoded.resolvedSettings().booleans["dynamic_colors"])
        assertTrue(decoded.resolvedSettings().integers.isEmpty())
        assertTrue(decoded.resolvedSettings().strings.isEmpty())
    }

    @Test
    fun testForwardCompatibilityWithFutureUnknownKeys() {
        // Future version with new unknown keys and metadata
        val futureJson = """
            {
                "app": "RivoPhone",
                "schemaVersion": 99,
                "appVersion": "5.0",
                "futureMetadataField": "unrecognized_extra_data",
                "settings": {
                    "booleans": {
                        "dynamic_colors": true,
                        "future_ai_dialing": true
                    },
                    "integers": {
                        "dialpad_style": 3
                    },
                    "futureSpecialCollection": [1, 2, 3]
                }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<SettingsBackupEnvelope>(futureJson)
        assertEquals("RivoPhone", decoded.app)
        assertEquals(99, decoded.schemaVersion)
        assertEquals("5.0", decoded.appVersion)
        assertEquals(true, decoded.resolvedSettings().booleans["dynamic_colors"])
        assertEquals(true, decoded.resolvedSettings().booleans["future_ai_dialing"])
        assertEquals(3, decoded.resolvedSettings().integers["dialpad_style"])
    }

    @Test
    fun testFlatFormatFallbackCompatibility() {
        // Hand-crafted or flat format without nested "settings" wrapper
        val flatJson = """
            {
                "app": "RivoPhone",
                "schemaVersion": 1,
                "booleans": {
                    "dynamic_colors": false
                },
                "integers": {
                    "dialpad_size": 2
                }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<SettingsBackupEnvelope>(flatJson)
        val resolved = decoded.resolvedSettings()
        assertEquals(false, resolved.booleans["dynamic_colors"])
        assertEquals(2, resolved.integers["dialpad_size"])
    }

    @Test
    fun testExcludedKeysProtection() {
        assertTrue(PreferenceManager.isExcludedKey(PreferenceManager.KEY_APP_LOCK_PIN))
        assertTrue(PreferenceManager.isExcludedKey(PreferenceManager.KEY_APP_LOCK_ENABLED))
        assertTrue(PreferenceManager.isExcludedKey(PreferenceManager.KEY_IS_SUPPORTER))
        assertTrue(PreferenceManager.isExcludedKey(PreferenceManager.KEY_APP_USAGE_SECONDS))
        assertTrue(PreferenceManager.isExcludedKey(PreferenceManager.KEY_RATE_APP_SHOWN))
        assertTrue(PreferenceManager.isExcludedKey(PreferenceManager.KEY_SETTINGS_HISTORY))
        assertTrue(PreferenceManager.isExcludedKey(PreferenceManager.KEY_CALL_RECORDING_FOLDER_URI))

        // Normal settings must not be excluded
        assertFalse(PreferenceManager.isExcludedKey(PreferenceManager.KEY_DYNAMIC_COLORS))
        assertFalse(PreferenceManager.isExcludedKey(PreferenceManager.KEY_DIALPAD_STYLE))
        assertFalse(PreferenceManager.isExcludedKey(PreferenceManager.KEY_AMOLED_MODE))
    }
}
