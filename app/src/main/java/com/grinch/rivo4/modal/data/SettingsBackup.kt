package com.grinch.rivo4.modal.data

import kotlinx.serialization.Serializable

@Serializable
data class SettingsBackupEnvelope(
    val app: String = DEFAULT_APP_IDENTIFIER,
    val backupType: String = BACKUP_TYPE_SETTINGS,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersion: String = "",
    val appVersionCode: Long = 0L,
    val createdAt: Long = 0L,
    val timestamp: Long = 0L,
    val settings: SettingsData = SettingsData(),
    // Fallback support if a backup has flat fields
    val booleans: Map<String, Boolean> = emptyMap(),
    val integers: Map<String, Int> = emptyMap(),
    val longs: Map<String, Long> = emptyMap(),
    val floats: Map<String, Float> = emptyMap(),
    val strings: Map<String, String> = emptyMap(),
    val stringSets: Map<String, Set<String>> = emptyMap()
) {
    fun resolvedSettings(): SettingsData {
        return SettingsData(
            booleans = if (settings.booleans.isNotEmpty()) settings.booleans else booleans,
            integers = if (settings.integers.isNotEmpty()) settings.integers else integers,
            longs = if (settings.longs.isNotEmpty()) settings.longs else longs,
            floats = if (settings.floats.isNotEmpty()) settings.floats else floats,
            strings = if (settings.strings.isNotEmpty()) settings.strings else strings,
            stringSets = if (settings.stringSets.isNotEmpty()) settings.stringSets else stringSets
        )
    }

    val effectiveTimestamp: Long
        get() = if (createdAt > 0L) createdAt else if (timestamp > 0L) timestamp else System.currentTimeMillis()

    companion object {
        const val DEFAULT_APP_IDENTIFIER = "RivoPhone"
        const val BACKUP_TYPE_SETTINGS = "settings"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class SettingsData(
    val booleans: Map<String, Boolean> = emptyMap(),
    val integers: Map<String, Int> = emptyMap(),
    val longs: Map<String, Long> = emptyMap(),
    val floats: Map<String, Float> = emptyMap(),
    val strings: Map<String, String> = emptyMap(),
    val stringSets: Map<String, Set<String>> = emptyMap()
)
