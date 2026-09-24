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
    fun isLocalAccount(accountType: String?, accountName: String?, availableAccounts: List<Account>): Boolean {
        if (accountType == null || accountName == null) return true
        val typeLower = accountType.lowercase()
        val nameLower = accountName.lowercase()
        if (typeLower.contains("phone") || typeLower.contains("local") || typeLower.contains("device") ||
            typeLower.contains("default") || typeLower.contains("sec.contact") ||
            nameLower == "phone" || nameLower == "device" || nameLower == "default" || nameLower == "local") {
            return true
        }
        return availableAccounts.none { it.type.equals(accountType, ignoreCase = true) && it.name.equals(accountName, ignoreCase = true) }
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
                contact.name
            } else {
                formatContactName(contact.name, displayOrder)
            }
        } else {
            contact.name
        }
    }
}
