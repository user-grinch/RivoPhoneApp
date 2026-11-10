package com.grinch.rivo4.controller.util

import com.grinch.rivo4.modal.db.NumberTagDao
import com.grinch.rivo4.modal.db.NumberTagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NumberTagManager(
    private val numberTagDao: NumberTagDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // In-memory StateFlow representing normalizedPhoneNumber -> tag string
    private val _tagsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagsMap: StateFlow<Map<String, String>> = _tagsMap.asStateFlow()

    init {
        scope.launch {
            numberTagDao.getAllTags().collect { list ->
                _tagsMap.value = list.flatMap { entity ->
                    listOfNotNull(
                        entity.phoneNumber to entity.tag,
                        entity.rawNumber?.takeIf { it.isNotBlank() }?.let { it to entity.tag }
                    )
                }.toMap()
            }
        }
    }

    fun findTag(rawNumber: String?, map: Map<String, String> = tagsMap.value): String? {
        if (rawNumber.isNullOrBlank()) return null
        val normalized = normalizePhoneNumber(rawNumber)
        if (normalized.isNotBlank()) {
            map[normalized]?.let { return it }
        }
        val trimmed = rawNumber.trim()
        map[trimmed]?.let { return it }
        // Fallback matching using areNumbersEqual
        return map.entries.firstOrNull { (num, _) -> areNumbersEqual(num, rawNumber) }?.value
    }

    suspend fun setTag(rawNumber: String, tag: String) {
        val trimmedTag = tag.trim().take(30)
        val normalized = normalizePhoneNumber(rawNumber).ifBlank { rawNumber.trim() }
        val raw = rawNumber.trim()

        // 1. Instantly update in-memory map synchronously so StateFlow emits immediately to all UI collectors
        val current = _tagsMap.value.toMutableMap()
        if (trimmedTag.isEmpty()) {
            current.remove(normalized)
            current.remove(raw)
        } else {
            current[normalized] = trimmedTag
            if (raw.isNotBlank()) current[raw] = trimmedTag
        }
        _tagsMap.value = current

        // 2. Persist in database
        withContext(Dispatchers.IO) {
            if (trimmedTag.isEmpty()) {
                numberTagDao.deleteTag(normalized)
                if (normalized != raw) {
                    numberTagDao.deleteTag(raw)
                }
            } else {
                numberTagDao.insertOrUpdate(
                    NumberTagEntity(
                        phoneNumber = normalized,
                        tag = trimmedTag,
                        rawNumber = raw
                    )
                )
            }
        }
    }

    suspend fun deleteTag(rawNumber: String) {
        val normalized = normalizePhoneNumber(rawNumber).ifBlank { rawNumber.trim() }
        val raw = rawNumber.trim()

        // Instantly update in-memory map synchronously
        val current = _tagsMap.value.toMutableMap()
        current.remove(normalized)
        current.remove(raw)
        _tagsMap.value = current

        withContext(Dispatchers.IO) {
            numberTagDao.deleteTag(normalized)
            if (normalized != raw) {
                numberTagDao.deleteTag(raw)
            }
        }
    }
}
