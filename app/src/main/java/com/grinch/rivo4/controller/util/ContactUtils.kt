package com.grinch.rivo4.controller.util

import android.accounts.Account
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.ui.graphics.vector.ImageVector
import com.grinch.rivo4.R
import com.grinch.rivo4.modal.data.Contact

object ContactUtils {
    fun isSyncAdapterType(accountType: String?): Boolean {
        if (accountType == null) return false
        val t = accountType.lowercase()
        return t.contains("whatsapp") || t.contains("telegram") || t.contains("viber") || t.contains("skype")
    }

    fun isLocalAccount(
        accountType: String?,
        accountName: String?,
        availableAccounts: List<Account> = emptyList()
    ): Boolean {
        if (accountType == null || accountName == null) return true
        if (accountType.isBlank() || accountName.isBlank()) return true
        val typeLower = accountType.lowercase()
        val nameLower = accountName.lowercase()
        if (typeLower.contains("phone") || typeLower.contains("local") || typeLower.contains("device") ||
            typeLower.contains("default") || typeLower.contains("sec.contact") ||
            typeLower.contains("miui.contact") || typeLower == "com.android.contacts" ||
            typeLower.startsWith("com.google.android.gms.null") ||
            nameLower == "phone" || nameLower == "device" || nameLower == "default" || nameLower == "local"
        ) {
            return true
        }
        if (isSyncAdapterType(accountType)) {
            return false
        }
        val nonSyncAccounts = availableAccounts.filter { !isSyncAdapterType(it.type) }
        if (nonSyncAccounts.isEmpty()) return true
        return nonSyncAccounts.none {
            it.type.equals(accountType, ignoreCase = true) && it.name.equals(
                accountName,
                ignoreCase = true
            )
        }
    }

    fun isContactLocal(contact: Contact, availableAccounts: List<Account> = emptyList()): Boolean {
        if (contact.isPrivate) return false
        if (isLocalAccount(contact.accountType, contact.accountName, availableAccounts)) return true
        if (contact.linkedAccounts.any { isLocalAccount(it.type, it.name, availableAccounts) }) return true

        val hasCloudAccount = contact.linkedAccounts.any { acc ->
            acc.type != null && !isSyncAdapterType(acc.type) && !isLocalAccount(acc.type, acc.name, availableAccounts)
        }
        val primaryIsCloud = contact.accountType != null &&
                !isSyncAdapterType(contact.accountType) &&
                !isLocalAccount(contact.accountType, contact.accountName, availableAccounts)

        return !hasCloudAccount && !primaryIsCloud
    }

    fun getFriendlyAccountName(context: Context, account: Account): String {
        return when {
            account.type == "com.google" -> account.name
            account.type == "com.whatsapp" -> context.getString(R.string.brand_whatsapp)
            account.type.contains("telegram", ignoreCase = true) -> context.getString(R.string.brand_telegram)
            account.type.contains("xiaomi", ignoreCase = true) -> context.getString(R.string.account_mi_account)
            account.type.contains("sim", ignoreCase = true) -> context.getString(R.string.account_sim_card)
            else -> account.name
        }
    }

    fun getAccountIcon(account: Account): ImageVector {
        return when {
            account.type == "com.google" -> Icons.Default.Email
            account.type.contains("sim", ignoreCase = true) -> Icons.Default.SimCard
            else -> Icons.Default.AccountCircle
        }
    }

    data class SanitizedNameParts(
        val prefix: String?,
        val givenName: String?,
        val middleName: String?,
        val familyName: String?,
        val suffix: String?,
        val fullName: String
    )

    fun collapseAdjacentDuplicateTokens(text: String): String {
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size <= 1) return text.trim()
        val result = mutableListOf<String>()
        for (token in tokens) {
            if (result.isEmpty() || !result.last().equals(token, ignoreCase = true)) {
                result.add(token)
            }
        }
        return result.joinToString(" ")
    }

    fun sanitizeStructuredName(
        prefix: String? = null,
        givenName: String? = null,
        middleName: String? = null,
        familyName: String? = null,
        suffix: String? = null,
        fallbackFullName: String? = null
    ): SanitizedNameParts {
        val cleanPrefix = prefix?.trim()?.ifBlank { null }
        var cleanGiven = givenName?.trim()?.ifBlank { null }?.let { collapseAdjacentDuplicateTokens(it) }
        var cleanMiddle = middleName?.trim()?.ifBlank { null }?.let { collapseAdjacentDuplicateTokens(it) }
        var cleanFamily = familyName?.trim()?.ifBlank { null }?.let { collapseAdjacentDuplicateTokens(it) }
        val cleanSuffix = suffix?.trim()?.ifBlank { null }

        // Strip duplicate middleName from the end of givenName or start of familyName
        if (cleanMiddle != null) {
            if (cleanGiven != null) {
                while (cleanGiven != null && cleanGiven.length > cleanMiddle.length &&
                    cleanGiven.endsWith(" $cleanMiddle", ignoreCase = true)
                ) {
                    cleanGiven = cleanGiven.substring(0, cleanGiven.length - cleanMiddle.length).trim().ifBlank { null }
                }
                if (cleanGiven != null && cleanGiven.equals(cleanMiddle, ignoreCase = true)) {
                    cleanMiddle = null
                }
            }
            if (cleanMiddle != null && cleanFamily != null) {
                while (cleanFamily != null && cleanFamily.length > cleanMiddle.length &&
                    cleanFamily.startsWith("$cleanMiddle ", ignoreCase = true)
                ) {
                    cleanFamily = cleanFamily.substring(cleanMiddle.length).trim().ifBlank { null }
                }
                if (cleanFamily != null && cleanFamily.equals(cleanMiddle, ignoreCase = true)) {
                    cleanMiddle = null
                }
            }
        }

        // If middleName is missing, recover it from multi-word givenName or fallbackFullName
        if (cleanMiddle == null) {
            val cleanFallback = fallbackFullName?.trim()?.takeIf {
                it.isNotBlank() && !it.equals("Unnamed", ignoreCase = true) && !it.equals("Unknown", ignoreCase = true)
            }?.let { collapseAdjacentDuplicateTokens(it) }

            val fallbackWithoutAffixes = cleanFallback?.let { fb ->
                var s = fb
                if (cleanPrefix != null && s.startsWith("$cleanPrefix ", ignoreCase = true)) {
                    s = s.substring(cleanPrefix.length).trim()
                }
                if (cleanSuffix != null && s.endsWith(" $cleanSuffix", ignoreCase = true)) {
                    s = s.substring(0, s.length - cleanSuffix.length).trim()
                }
                s
            }

            if (cleanGiven == null && cleanFamily == null && !fallbackWithoutAffixes.isNullOrBlank()) {
                val (sg, sm, sf) = splitDisplayName(fallbackWithoutAffixes)
                cleanGiven = sg.ifBlank { null }
                cleanMiddle = sm.ifBlank { null }
                cleanFamily = sf.ifBlank { null }
            } else if (cleanGiven != null && cleanFamily != null) {
                val gParts = cleanGiven.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (gParts.size > 1) {
                    cleanGiven = gParts.first()
                    cleanMiddle = gParts.drop(1).joinToString(" ")
                } else if (!fallbackWithoutAffixes.isNullOrBlank()) {
                    val (sg, sm, sf) = splitDisplayName(fallbackWithoutAffixes)
                    if (sm.isNotBlank() && sg.equals(cleanGiven, ignoreCase = true) && sf.equals(cleanFamily, ignoreCase = true)) {
                        cleanMiddle = collapseAdjacentDuplicateTokens(sm)
                    } else if (sm.isNotBlank() && sg.equals(cleanGiven, ignoreCase = true) &&
                        cleanFamily.equals("$sm $sf", ignoreCase = true)
                    ) {
                        cleanMiddle = collapseAdjacentDuplicateTokens(sm)
                        cleanFamily = sf.ifBlank { null }
                    }
                }
            } else if (cleanGiven != null && cleanFamily == null) {
                val gParts = cleanGiven.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (gParts.size >= 3) {
                    cleanGiven = gParts.first()
                    cleanMiddle = gParts.subList(1, gParts.size - 1).joinToString(" ")
                    cleanFamily = gParts.last()
                } else if (!fallbackWithoutAffixes.isNullOrBlank()) {
                    val (sg, sm, sf) = splitDisplayName(fallbackWithoutAffixes)
                    if (sg.equals(cleanGiven, ignoreCase = true) && (sm.isNotBlank() || sf.isNotBlank())) {
                        cleanMiddle = sm.ifBlank { null }
                        cleanFamily = sf.ifBlank { null }
                    }
                }
            }
        }

        // Re-verify no overlap between givenName/middleName/familyName
        if (cleanMiddle != null) {
            if (cleanGiven != null && cleanGiven.length > cleanMiddle.length &&
                cleanGiven.endsWith(" $cleanMiddle", ignoreCase = true)
            ) {
                cleanGiven = cleanGiven.substring(0, cleanGiven.length - cleanMiddle.length).trim().ifBlank { null }
            }
            if (cleanFamily != null && cleanFamily.length > cleanMiddle.length &&
                cleanFamily.startsWith("$cleanMiddle ", ignoreCase = true)
            ) {
                cleanFamily = cleanFamily.substring(cleanMiddle.length).trim().ifBlank { null }
            }
        }

        val joined = listOfNotNull(cleanPrefix, cleanGiven, cleanMiddle, cleanFamily, cleanSuffix)
            .joinToString(" ")
            .let { collapseAdjacentDuplicateTokens(it) }

        val finalFullName = if (joined.isNotBlank()) {
            joined
        } else {
            fallbackFullName?.trim()?.let { collapseAdjacentDuplicateTokens(it) } ?: ""
        }

        return SanitizedNameParts(
            prefix = cleanPrefix,
            givenName = cleanGiven,
            middleName = cleanMiddle,
            familyName = cleanFamily,
            suffix = cleanSuffix,
            fullName = finalFullName
        )
    }

    fun formatContactName(name: String, displayOrder: Int): String {
        val cleanName = collapseAdjacentDuplicateTokens(name)
        if (displayOrder == 1) {
            val parts = cleanName.trim().split("\\s+".toRegex())
            if (parts.size > 1) {
                return "${parts.last()}, ${parts.dropLast(1).joinToString(" ")}"
            }
        }
        return cleanName
    }

    fun formatContactName(contact: Contact, displayOrder: Int): String {
        val sanitized = sanitizeStructuredName(
            prefix = contact.prefix,
            givenName = contact.givenName,
            middleName = contact.middleName,
            familyName = contact.familyName,
            suffix = contact.suffix,
            fallbackFullName = contact.name
        )
        val baseName = if (sanitized.fullName.isNotBlank() && sanitized.fullName != "Unnamed") {
            sanitized.fullName
        } else if (contact.name.isNotBlank() && contact.name != "Unnamed") {
            collapseAdjacentDuplicateTokens(contact.name)
        } else {
            contact.emails.firstOrNull() ?: contact.phoneNumbers.firstOrNull() ?: contact.name
        }
        return if (displayOrder == 1) {
            if (!sanitized.familyName.isNullOrBlank()) {
                val prefixPart = sanitized.prefix
                val givenMiddlePart = listOfNotNull(
                    sanitized.givenName,
                    sanitized.middleName
                ).joinToString(" ").ifBlank { null }

                when {
                    prefixPart != null && givenMiddlePart != null -> "$prefixPart ${sanitized.familyName}, $givenMiddlePart"
                    prefixPart != null -> "$prefixPart ${sanitized.familyName}"
                    givenMiddlePart != null -> "${sanitized.familyName}, $givenMiddlePart"
                    else -> sanitized.familyName
                }
            } else if (!sanitized.givenName.isNullOrBlank()) {
                baseName
            } else {
                formatContactName(baseName, displayOrder)
            }
        } else {
            baseName
        }
    }

    fun getContactSortKey(contact: Contact, displayOrder: Int): String {
        if (displayOrder == 1) {
            if (!contact.familyName.isNullOrBlank()) {
                return contact.familyName.trim()
            }
            val parts = contact.name.trim().split(Regex("\\s+"))
            if (parts.size > 1) {
                return parts.last()
            }
        }
        val effectiveName = if (contact.name.isNotBlank() && contact.name != "Unnamed") {
            contact.name
        } else {
            contact.emails.firstOrNull() ?: contact.phoneNumbers.firstOrNull() ?: contact.name
        }
        val prefix = contact.prefix?.trim()
        if (!prefix.isNullOrBlank() && effectiveName.startsWith(prefix, ignoreCase = true)) {
            val withoutPrefix = effectiveName.substring(prefix.length).trim()
            if (withoutPrefix.isNotEmpty()) return stripTitlePrefix(withoutPrefix)
        }
        return stripTitlePrefix(effectiveName)
    }

    fun getContactInitial(contact: Contact, displayOrder: Int): String {
        val key = getContactSortKey(contact, displayOrder)
        val firstChar = key.firstOrNull()?.uppercaseChar() ?: '#'
        return if (firstChar in 'A'..'Z') firstChar.toString() else "#"
    }

    fun compareContacts(c1: Contact, c2: Contact, displayOrder: Int): Int {
        val key1 = getContactSortKey(c1, displayOrder)
        val key2 = getContactSortKey(c2, displayOrder)
        val res = key1.compareTo(key2, ignoreCase = true)
        if (res != 0) return res
        val n1 = formatContactName(c1, displayOrder)
        val n2 = formatContactName(c2, displayOrder)
        val res2 = n1.compareTo(n2, ignoreCase = true)
        return if (res2 != 0) res2 else c1.name.compareTo(c2.name, ignoreCase = true)
    }
    private val COMMON_NAME_PREFIXES = setOf(
        "dr", "prof", "professor", "mr", "mrs", "ms", "miss", "sir", "madam",
        "dame", "lord", "lady", "rev", "reverend", "pastor", "father", "fr",
        "rabbi", "imam", "sheikh", "shaikh", "general", "gen", "col", "colonel",
        "maj", "major", "capt", "captain", "lt", "lieutenant", "sgt", "sergeant",
        "officer", "ofc", "judge", "justice", "hon", "honorable", "senator", "sen",
        "rep", "representative", "gov", "governor", "pres", "president", "amb",
        "ambassador", "eng", "engineer", "arch", "architect", "atty", "attorney", "doc"
    )

    fun stripTitlePrefix(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split(Regex("\\s+"))
        var index = 0
        while (index < parts.size - 1) {
            val word = parts[index]
            val cleaned = word.lowercase().trimEnd('.', ':', ',')
            if (cleaned in COMMON_NAME_PREFIXES) {
                index++
            } else {
                break
            }
        }
        return if (index > 0 && index < parts.size) {
            parts.drop(index).joinToString(" ")
        } else {
            trimmed
        }
    }

    fun getLookupKeys(number: String): List<String> {
        val digitsOnly = number.filter { it.isDigit() }
        if (digitsOnly.length < 3) {
            return if (digitsOnly.isNotEmpty()) listOf(digitsOnly) else emptyList()
        }
        val keys = mutableListOf<String>()
        keys.add(digitsOnly)
        val trimmedLeadingZero = digitsOnly.trimStart('0')
        if (trimmedLeadingZero.isNotEmpty() && trimmedLeadingZero != digitsOnly) {
            keys.add(trimmedLeadingZero)
        }
        // If number starts with international prefix (+ or 00), also add version with country code removed
        val trimmed = number.trim()
        if (trimmed.startsWith("+") || trimmed.startsWith("00")) {
            val cleanDigits = if (trimmed.startsWith("00")) digitsOnly.removePrefix("00") else digitsOnly
            for (codeLen in 1..3) {
                if (cleanDigits.length > codeLen + 5) {
                    val nationalPart = cleanDigits.substring(codeLen)
                    keys.add(nationalPart)
                    keys.add(nationalPart.trimStart('0'))
                    keys.add("0${nationalPart.trimStart('0')}")
                }
            }
        }
        for (len in listOf(11, 10, 9, 8, 7)) {
            if (digitsOnly.length >= len) {
                keys.add(digitsOnly.takeLast(len))
            }
            if (trimmedLeadingZero.length >= len) {
                keys.add(trimmedLeadingZero.takeLast(len))
            }
        }
        return keys.distinct().sortedByDescending { it.length }
    }

    fun splitDisplayName(full: String): Triple<String, String, String> {
        val trimmed = full.trim()
        if (trimmed.isEmpty()) return Triple("", "", "")
        val parts = trimmed.split(Regex("\\s+"))
        return when (parts.size) {
            1 -> Triple(parts[0], "", "")
            2 -> Triple(parts[0], "", parts[1])
            else -> Triple(
                parts.first(),
                parts.subList(1, parts.size - 1).joinToString(" "),
                parts.last()
            )
        }
    }

    fun matchesContact(
        contact: Contact,
        query: String,
        cleanQuery: String = query.filter { it.isDigit() || it == '+' || it == '*' || it == '#' },
        displayOrder: Int = 0
    ): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false

        // 1. Phone number match
        if (cleanQuery.isNotEmpty() && contact.phoneNumbers.any { number ->
            number.filter { ch -> ch.isDigit() || ch == '+' || ch == '*' || ch == '#' }.contains(cleanQuery)
        }) {
            return true
        }

        // 2. Direct string match on name or formatted representations
        val formattedName = formatContactName(contact, displayOrder)
        val displayName = contact.formattedDisplayName
        if (contact.name.contains(q, ignoreCase = true) ||
            formattedName.contains(q, ignoreCase = true) ||
            displayName.contains(q, ignoreCase = true)
        ) {
            return true
        }

        // 3. Name parts: givenName, middleName, familyName, nickname, prefix, suffix
        val sanitized = sanitizeStructuredName(
            prefix = contact.prefix,
            givenName = contact.givenName,
            middleName = contact.middleName,
            familyName = contact.familyName,
            suffix = contact.suffix,
            fallbackFullName = contact.name
        )
        val nameParts = listOfNotNull(
            contact.givenName?.ifBlank { null },
            contact.middleName?.ifBlank { null },
            contact.familyName?.ifBlank { null },
            sanitized.givenName,
            sanitized.middleName,
            sanitized.familyName,
            contact.nickname?.ifBlank { null },
            contact.prefix?.ifBlank { null },
            contact.suffix?.ifBlank { null }
        ).distinct()
        if (nameParts.any { it.contains(q, ignoreCase = true) }) {
            return true
        }

        // Fallback split if given/middle/family were not separated
        val (splitGiven, splitMiddle, splitFamily) = splitDisplayName(contact.name)
        val fallbackParts = listOfNotNull(
            splitGiven.ifBlank { null },
            splitMiddle.ifBlank { null },
            splitFamily.ifBlank { null }
        )
        if (fallbackParts.any { it.contains(q, ignoreCase = true) }) {
            return true
        }

        // 4. Email, Organization
        if (contact.emails.any { it.contains(q, ignoreCase = true) }) return true
        if (contact.organizations.any {
            it.company.contains(q, ignoreCase = true) ||
            it.title?.contains(q, ignoreCase = true) == true ||
            it.department?.contains(q, ignoreCase = true) == true
        }) return true

        // 5. Multi-token query (e.g. "Doe John", "Michael Doe", "John Michael", "Dr. Smith")
        val tokens = q.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.size > 1) {
            val allSearchableText = (nameParts + fallbackParts + listOf(
                contact.name,
                formattedName,
                displayName
            )).joinToString(" ")

            val allTokensMatch = tokens.all { token ->
                allSearchableText.contains(token, ignoreCase = true)
            }
            if (allTokensMatch) return true
        }

        return false
    }
}
