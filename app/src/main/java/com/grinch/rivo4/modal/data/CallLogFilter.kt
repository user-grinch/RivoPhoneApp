package com.grinch.rivo4.modal.data

import com.grinch.rivo4.controller.util.formatDateHeader

enum class CallLogFilter {
    All,
    Contacts,
    Incoming,
    Outgoing,
    Missed;

    companion object {
        public fun filter(logs: List<CallLogEntry>, type: CallLogFilter): List<List<CallLogEntry>> {
            val filteredList = when (type) {
                All -> logs
                Contacts -> logs.filter { it.name != null && it.name.isNotEmpty() }
                Incoming -> logs.filter { it.type == android.provider.CallLog.Calls.INCOMING_TYPE }
                Outgoing -> logs.filter { it.type == android.provider.CallLog.Calls.OUTGOING_TYPE }
                Missed -> logs.filter { it.type == android.provider.CallLog.Calls.MISSED_TYPE }
            }
            return filteredList.groupBy { formatDateHeader(it.date) }.values.toList()
        }

        public fun getNames(): List<String> {
            return entries.map { it.name }
        }
    }
};