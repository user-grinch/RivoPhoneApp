package com.grinch.rivo4.modal.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.formatDateHeader

@Composable
fun CallLogFilter.displayLabel(): String = when (this) {
    CallLogFilter.All -> stringResource(R.string.filter_all)
    CallLogFilter.Contacts -> stringResource(R.string.nav_contacts)
    CallLogFilter.Incoming -> stringResource(R.string.call_type_incoming)
    CallLogFilter.Outgoing -> stringResource(R.string.call_type_outgoing)
    CallLogFilter.Missed -> stringResource(R.string.call_type_missed)
}

enum class CallLogFilter {
    All,
    Contacts,
    Incoming,
    Outgoing,
    Missed;

    fun matches(entry: CallLogEntry): Boolean = when (this) {
        All -> true
        Contacts -> !entry.contactId.isNullOrBlank() ||
            (!entry.name.isNullOrBlank() && entry.name != entry.number && !entry.name.equals("Unknown", ignoreCase = true))
        Incoming -> entry.type == android.provider.CallLog.Calls.INCOMING_TYPE
        Outgoing -> entry.type == android.provider.CallLog.Calls.OUTGOING_TYPE
        Missed -> entry.type == android.provider.CallLog.Calls.MISSED_TYPE || entry.type == android.provider.CallLog.Calls.REJECTED_TYPE
    }

    companion object {
        public fun filter(context: Context, logs: List<CallLogEntry>, type: CallLogFilter): List<List<CallLogEntry>> {
            val filteredList = logs.filter { type.matches(it) }
            return filteredList.groupBy { formatDateHeader(context, it.date) }.values.toList()
        }

        public fun getNames(): List<String> {
            return entries.map { it.name }
        }
    }
};