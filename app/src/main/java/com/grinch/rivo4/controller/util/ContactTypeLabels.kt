package com.grinch.rivo4.controller.util

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.grinch.rivo4.R

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

    val eventTypeOptions: List<Int> = listOf(
        android.provider.ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
        android.provider.ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,
        android.provider.ContactsContract.CommonDataKinds.Event.TYPE_OTHER
    )

    val postalAddressTypeOptions: List<Int> = listOf(
        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK,
        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
    )

    val websiteTypeOptions: List<Int> = listOf(
        android.provider.ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE,
        android.provider.ContactsContract.CommonDataKinds.Website.TYPE_BLOG,
        android.provider.ContactsContract.CommonDataKinds.Website.TYPE_PROFILE,
        android.provider.ContactsContract.CommonDataKinds.Website.TYPE_WORK,
        android.provider.ContactsContract.CommonDataKinds.Website.TYPE_HOME,
        android.provider.ContactsContract.CommonDataKinds.Website.TYPE_FTP
    )

    val relationTypeOptions: List<Int> = listOf(
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_FATHER,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_PARENT,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_CHILD,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_SISTER,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_REFERRED_BY,
        android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE
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

    fun eventTypeLabel(context: Context, type: Int, label: String?): String {
        return try {
            android.provider.ContactsContract.CommonDataKinds.Event
                .getTypeLabel(context.resources, type, label).toString()
        } catch (e: Exception) {
            label ?: ""
        }
    }

    fun postalAddressTypeLabel(context: Context, type: Int, label: String?): String {
        return try {
            android.provider.ContactsContract.CommonDataKinds.StructuredPostal
                .getTypeLabel(context.resources, type, label).toString()
        } catch (e: Exception) {
            label ?: ""
        }
    }

    /**
     * Website exposes neither getTypeLabel(...) nor getTypeLabelResource(...) in the
     * platform, so resolve labels from app resources (mirrors AOSP relationType/websiteType).
     */
    fun websiteTypeLabel(context: Context, type: Int, label: String?): String {
        if (!label.isNullOrBlank()) return label
        val resId = when (type) {
            android.provider.ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE -> R.string.website_type_homepage
            android.provider.ContactsContract.CommonDataKinds.Website.TYPE_BLOG -> R.string.website_type_blog
            android.provider.ContactsContract.CommonDataKinds.Website.TYPE_PROFILE -> R.string.website_type_profile
            android.provider.ContactsContract.CommonDataKinds.Website.TYPE_WORK -> R.string.website_type_work
            android.provider.ContactsContract.CommonDataKinds.Website.TYPE_HOME -> R.string.website_type_home
            android.provider.ContactsContract.CommonDataKinds.Website.TYPE_FTP -> R.string.website_type_ftp
            else -> 0
        }
        return if (resId != 0) {
            try { context.getString(resId) } catch (e: Exception) {
                context.getString(R.string.contact_event_generic)
            }
        } else {
            context.getString(R.string.contact_event_generic)
        }
    }

    /**
     * Relation exposes getTypeLabelResource(...) only; use it with a graceful fallback
     * for unit-test stubs.
     */
    fun relationTypeLabel(context: Context, type: Int, label: String?): String {
        if (!label.isNullOrBlank()) return label
        val resId = try {
            android.provider.ContactsContract.CommonDataKinds.Relation.getTypeLabelResource(type)
        } catch (e: Exception) {
            0
        }
        return if (resId != 0) {
            try { context.getString(resId) } catch (e: Exception) {
                context.getString(R.string.contact_edit_relation)
            }
        } else {
            context.getString(R.string.contact_edit_relation)
        }
    }
}
