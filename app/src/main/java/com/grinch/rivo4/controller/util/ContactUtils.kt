package com.grinch.rivo4.controller.util

import android.accounts.Account
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.ui.graphics.vector.ImageVector
import com.grinch.rivo4.R

object ContactUtils {
    fun getFriendlyAccountName(context: Context, account: Account): String {
        return when {
            account.type == "com.google" -> account.name
            account.type == "com.whatsapp" -> context.getString(R.string.brand_whatsapp)
            account.type.contains("telegram", ignoreCase = true) -> context.getString(R.string.brand_telegram)
            account.type.contains("xiaomi", ignoreCase = true) -> context.getString(R.string.account_mi_account)
            account.type.contains("sim", ignoreCase = true) -> context.getString(R.string.account_sim_card)
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
