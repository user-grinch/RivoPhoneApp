package com.grinch.rivo4.controller.util

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone

object ContactTypeLabels {

    data class TypeOption(val type: Int, val labelResFallback: String)

    val phoneTypeOptions: List<Int> = listOf(
        Phone.TYPE_MOBILE,
        Phone.TYPE_HOME,
        Phone.TYPE_WORK,
        Phone.TYPE_MAIN,
        Phone.TYPE_OTHER
    )

    val emailTypeOptions: List<Int> = listOf(
        Email.TYPE_HOME,
        Email.TYPE_WORK,
        Email.TYPE_OTHER
    )

    fun phoneTypeLabel(context: Context, type: Int, label: String?): String {
        return try {
            Phone.getTypeLabel(context.resources, type, label).toString()
        } catch (e: Exception) {
            label ?: ""
        }
    }

    fun emailTypeLabel(context: Context, type: Int, label: String?): String {
        return try {
            Email.getTypeLabel(context.resources, type, label).toString()
        } catch (e: Exception) {
            label ?: ""
        }
    }
}
