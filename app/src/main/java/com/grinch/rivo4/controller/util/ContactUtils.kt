package com.grinch.rivo4.controller.util

import android.accounts.Account
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.ui.graphics.vector.ImageVector

object ContactUtils {
    fun getFriendlyAccountName(account: Account): String {
        return when {
            account.type == "com.google" -> account.name
            account.type == "com.whatsapp" -> "WhatsApp"
            account.type.contains("telegram", ignoreCase = true) -> "Telegram"
            account.type.contains("xiaomi", ignoreCase = true) -> "Mi Account"
            account.type.contains("sim", ignoreCase = true) -> "SIM Card"
            account.name.contains("@") -> account.name.substringBefore("@")
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
        if (displayOrder == 1) { // Last Name First
            val parts = name.trim().split("\\s+".toRegex())
            if (parts.size > 1) {
                return "${parts.last()}, ${parts.dropLast(1).joinToString(" ")}"
            }
        }
        return name
    }
}
