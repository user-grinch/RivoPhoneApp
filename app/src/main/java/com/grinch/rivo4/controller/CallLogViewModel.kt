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

class CallLogViewModel(
    private val callLogRepo: ICallLogRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _allCallLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val allCallLogs: StateFlow<List<CallLogEntry>> = _allCallLogs.asStateFlow()

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
        contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, contentObserver)
    }

    override fun onCleared() {
        super.onCleared()
        contentResolver.unregisterContentObserver(contentObserver)
    }

    fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter
    }

    fun fetchLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val result = callLogRepo.getCallLogs()
            _allCallLogs.value = result
            _isLoading.value = false
        }
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
