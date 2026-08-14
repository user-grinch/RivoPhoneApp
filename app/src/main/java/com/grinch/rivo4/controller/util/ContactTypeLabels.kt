package com.grinch.rivo4.controller.util

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone

/**
 * Resolves ContactsContract phone/email type codes into human-readable labels
 * (e.g. "Mobile", "Work", or a custom label) and exposes the small set of types
 * offered in the contact editor. Keeps the framework dependency out of the data
 * layer while giving the UI real, per-number labels instead of a hardcoded "Mobile".
 */
object ContactTypeLabels {

    data class TypeOption(val type: Int, val labelResFallback: String)

    /** Types offered when editing a phone number, in display order. */
    val phoneTypeOptions: List<Int> = listOf(
        Phone.TYPE_MOBILE,
        Phone.TYPE_HOME,
        Phone.TYPE_WORK,
        Phone.TYPE_MAIN,
        Phone.TYPE_OTHER
    )

    /** Types offered when editing an email address, in display order. */
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
