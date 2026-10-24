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
     * Relation exposes neither getTypeLabel(...) in the public platform API nor reliably
     * resolves getTypeLabelResource(...) via app context, so resolve labels from app resources.
     */
    fun relationTypeLabel(context: Context, type: Int, label: String?): String {
        if (!label.isNullOrBlank()) return label
        val resId = when (type) {
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT -> R.string.relation_type_assistant
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER -> R.string.relation_type_brother
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_CHILD -> R.string.relation_type_child
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER -> R.string.relation_type_domestic_partner
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_FATHER -> R.string.relation_type_father
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND -> R.string.relation_type_friend
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER -> R.string.relation_type_manager
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER -> R.string.relation_type_mother
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_PARENT -> R.string.relation_type_parent
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER -> R.string.relation_type_partner
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_REFERRED_BY -> R.string.relation_type_referred_by
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE -> R.string.relation_type_relative
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_SISTER -> R.string.relation_type_sister
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE -> R.string.relation_type_spouse
            else -> 0
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
