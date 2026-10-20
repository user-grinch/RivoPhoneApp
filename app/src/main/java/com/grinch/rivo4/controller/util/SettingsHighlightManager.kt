package com.grinch.rivo4.controller.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SettingsHighlightManager {
    var highlightEpoch by mutableLongStateOf(0L)
        private set

    var highlightedSettingTitle by mutableStateOf<String?>(null)
        private set

    var highlightedTargetHeadline by mutableStateOf<String?>(null)
        private set

    var highlightedKeywords by mutableStateOf<List<String>>(emptyList())
        private set

    fun setHighlight(title: String, targetHeadline: String? = null) {
        highlightedSettingTitle = title
        highlightedTargetHeadline = targetHeadline
        highlightedKeywords = emptyList()
        highlightEpoch++
    }

    fun setHighlight(title: String, keywords: List<String>) {
        setHighlight(title, null as String?)
    }

    fun clear() {
        highlightedSettingTitle = null
        highlightedTargetHeadline = null
        highlightedKeywords = emptyList()
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun extractWords(text: String): List<String> {
        return normalize(text).split(" ").filter { it.isNotBlank() }
    }

    private fun wordsMatch(w1: String, w2: String): Boolean {
        if (w1 == w2) return true
        if (w1 == "${w2}s" || "${w1}s" == w2) return true
        if (w1 == "${w2}es" || "${w1}es" == w2) return true
        return false
    }

    fun matches(text: String): Boolean {
        val title = highlightedSettingTitle ?: return false
        val targetHeadline = highlightedTargetHeadline

        val hNorm = normalize(text)
        if (hNorm.isEmpty()) return false

        val tNorm = normalize(title)
        if (hNorm == tNorm) return true

        if (targetHeadline != null) {
            val thNorm = normalize(targetHeadline)
            if (hNorm == thNorm) return true
        }

        val hWords = extractWords(text)
        val tWords = extractWords(title)
        if (hWords.isNotEmpty() && tWords.isNotEmpty()) {
            if (hWords.size == tWords.size && hWords.indices.all { i -> wordsMatch(hWords[i], tWords[i]) }) {
                return true
            }
        }

        if (targetHeadline != null) {
            val thWords = extractWords(targetHeadline)
            if (hWords.isNotEmpty() && thWords.isNotEmpty()) {
                if (hWords.size == thWords.size && hWords.indices.all { i -> wordsMatch(hWords[i], thWords[i]) }) {
                    return true
                }
            }
        }

        val significantTitleWords = tWords.filter { it.length >= 3 }
        if (significantTitleWords.size >= 2 && significantTitleWords.all { tw -> hWords.any { hw -> wordsMatch(hw, tw) } }) {
            return true
        }

        if (targetHeadline != null) {
            val significantThWords = extractWords(targetHeadline).filter { it.length >= 3 }
            if (significantThWords.size >= 2 && significantThWords.all { tw -> hWords.any { hw -> wordsMatch(hw, tw) } }) {
                return true
            }
        }

        return false
    }
}
