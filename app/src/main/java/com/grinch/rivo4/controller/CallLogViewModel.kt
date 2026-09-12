package com.grinch.rivo4.controller

import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.CallLogFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog

data class TodayCallStats(
    val totalCalls: Int = 0,
    val missedCalls: Int = 0,
    val totalDurationSeconds: Long = 0L
)

class CallLogViewModel(
    private val callLogRepo: ICallLogRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _allCallLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val allCallLogs: StateFlow<List<CallLogEntry>> = _allCallLogs.asStateFlow()

    private val _todayStats = MutableStateFlow(TodayCallStats())
    val todayStats: StateFlow<TodayCallStats> = _todayStats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallLogFilter.All)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            fetchLogs()
        }
    }

    init {
        try {
            contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, contentObserver)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            contentResolver.unregisterContentObserver(contentObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter
    }

    fun fetchLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_allCallLogs.value.isEmpty()) {
                _isLoading.value = true
            }
            val result = callLogRepo.getCallLogs()
            _allCallLogs.value = result
            _todayStats.value = calculateTodayStats(result)
            _isLoading.value = false
        }
    }

    private fun calculateTodayStats(logs: List<CallLogEntry>): TodayCallStats {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val todayLogs = logs.filter { it.date >= startOfDay }
        val totalCalls = todayLogs.sumOf { it.count }
        val missedCalls = todayLogs.sumOf { entry ->
            if (entry.types.isNotEmpty()) entry.types.count { it == CallLog.Calls.MISSED_TYPE }
            else if (entry.type == CallLog.Calls.MISSED_TYPE) entry.count
            else 0
        }
        val totalDuration = todayLogs.sumOf { it.duration }
        return TodayCallStats(
            totalCalls = totalCalls,
            missedCalls = missedCalls,
            totalDurationSeconds = totalDuration
        )
    }

    fun deleteCallLog(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            callLogRepo.deleteCallLog(number)
            fetchLogs()
        }
    }

    fun deleteCallLogsByIds(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            callLogRepo.deleteCallLogsByIds(ids)
            fetchLogs()
        }
    }

    fun clearCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            callLogRepo.clearCallLogs()
            fetchLogs()
        }
    }
}
