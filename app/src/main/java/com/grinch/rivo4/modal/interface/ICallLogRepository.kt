package com.grinch.rivo4.modal.`interface`

import com.grinch.rivo4.modal.data.CallLogEntry

interface ICallLogRepository {
    fun getCallLogs(): List<CallLogEntry>
}