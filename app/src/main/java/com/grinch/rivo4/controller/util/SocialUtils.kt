package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object SocialUtils {
    fun openWhatsApp(context: Context, number: String) {
        val url = "https://api.whatsapp.com/send?phone=$number"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
    }

    fun openTelegram(context: Context, number: String) {
        val url = "tg://msg?text=&to=$number"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$number")))
            } catch (_: Exception) {}
        }
    }

    fun openSignal(context: Context, number: String) {
        val url = "sgnl://signal.me/#p/$number"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://signal.me/#p/$number")))
            } catch (_: Exception) {}
        }
    }
}
