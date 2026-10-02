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

    /**
     * Helper to determine whether a contact is stored locally (either as primary account or in its linked accounts).
     */
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

    fun formatContactName(name: String, displayOrder: Int): String {
        if (displayOrder == 1) {
            val parts = name.trim().split("\\s+".toRegex())
            if (parts.size > 1) {
                return "${parts.last()}, ${parts.dropLast(1).joinToString(" ")}"
            }
        }
        return name
    }

    fun formatContactName(contact: Contact, displayOrder: Int): String {
        val baseName = if (contact.name.isNotBlank() && contact.name != "Unnamed") {
            contact.name
        } else {
            contact.emails.firstOrNull() ?: contact.phoneNumbers.firstOrNull() ?: contact.name
        }
        return if (displayOrder == 1) {
            if (!contact.familyName.isNullOrBlank()) {
                val prefixPart = contact.prefix?.trim()?.ifBlank { null }
                val givenMiddlePart = listOfNotNull(
                    contact.givenName?.trim()?.ifBlank { null },
                    contact.middleName?.trim()?.ifBlank { null }
                ).joinToString(" ").ifBlank { null }

                when {
                    prefixPart != null && givenMiddlePart != null -> "$prefixPart ${contact.familyName}, $givenMiddlePart"
                    prefixPart != null -> "$prefixPart ${contact.familyName}"
                    givenMiddlePart != null -> "${contact.familyName}, $givenMiddlePart"
                    else -> contact.familyName
                }
            } else if (!contact.givenName.isNullOrBlank()) {
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
}
