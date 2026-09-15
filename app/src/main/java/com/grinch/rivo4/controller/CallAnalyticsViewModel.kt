package com.grinch.rivo4.controller

import android.provider.CallLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.data.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AnalyticsTimeRange(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class TopContactStat(
    val number: String,
    val name: String,
    val photoUri: String?,
    val totalDurationSeconds: Long,
    val totalCalls: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int
)

data class CallAnalyticsSummary(
    val totalTalkTimeSeconds: Long = 0L,
    val totalCalls: Int = 0,
    val incomingCalls: Int = 0,
    val outgoingCalls: Int = 0,
    val missedCalls: Int = 0,
    val rejectedCalls: Int = 0,
    val avgDurationSeconds: Long = 0L,
    val topContacts: List<TopContactStat> = emptyList(),
    val hourlyDistribution: Map<Int, Int> = emptyMap(),
    val simUsage: Map<String, Long> = emptyMap()
)

class CallAnalyticsViewModel(
    private val callLogRepo: ICallLogRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(AnalyticsTimeRange.TODAY)
    val selectedRange: StateFlow<AnalyticsTimeRange> = _selectedRange.asStateFlow()

    private val _analytics = MutableStateFlow(CallAnalyticsSummary())
    val analytics: StateFlow<CallAnalyticsSummary> = _analytics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var cachedCallLogs: List<CallLogEntry>? = null

    init {
        loadAnalytics(forceRefresh = true)
    }

    fun setTimeRange(range: AnalyticsTimeRange) {
        if (_selectedRange.value != range) {
            _selectedRange.value = range
            computeAnalytics()
        }
    }

    fun loadAnalytics(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            if (forceRefresh || cachedCallLogs == null) {
                cachedCallLogs = callLogRepo.getCallLogs()
            }
            computeAnalyticsInternal()
            _isLoading.value = false
        }
    }

    private fun computeAnalytics() {
        viewModelScope.launch(Dispatchers.IO) {
            if (cachedCallLogs == null) {
                _isLoading.value = true
                cachedCallLogs = callLogRepo.getCallLogs()
                _isLoading.value = false
            }
            computeAnalyticsInternal()
        }
    }

    private fun computeAnalyticsInternal() {
        val logs = cachedCallLogs ?: emptyList()
        val cutoff = calculateCutoffTime(_selectedRange.value)
        val filtered = if (cutoff > 0L) logs.filter { it.date >= cutoff } else logs

        var totalTalkTime = 0L
        var totalCalls = 0
        var incoming = 0
        var outgoing = 0
        var missed = 0
        var rejected = 0

        val hourly = mutableMapOf<Int, Int>()
        val simMap = mutableMapOf<String, Long>()
        val contactStatsMap = mutableMapOf<String, MutableContactAccumulator>()

        val cal = Calendar.getInstance()

        for (entry in filtered) {
            val callCount = entry.count
            totalCalls += callCount
            totalTalkTime += entry.duration

            // Call types
            val typesToCheck = if (entry.types.isNotEmpty()) entry.types else listOf(entry.type)
            for (t in typesToCheck) {
                when (t) {
                    CallLog.Calls.INCOMING_TYPE -> incoming++
                    CallLog.Calls.OUTGOING_TYPE -> outgoing++
                    CallLog.Calls.MISSED_TYPE -> missed++
                    CallLog.Calls.REJECTED_TYPE -> rejected++
                }
            }

            // Hourly distribution
            cal.timeInMillis = entry.date
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourly[hour] = (hourly[hour] ?: 0) + callCount

            // SIM usage
            val sim = entry.simLabel ?: "Primary SIM"
            simMap[sim] = (simMap[sim] ?: 0L) + entry.duration

            // Contact aggregation
            val contactKey = entry.contactId ?: entry.number
            val acc = contactStatsMap.getOrPut(contactKey) {
                MutableContactAccumulator(
                    number = entry.number,
                    name = entry.name ?: entry.number,
                    photoUri = entry.photoUri
                )
            }
            acc.totalDuration += entry.duration
            acc.totalCalls += callCount
            for (t in typesToCheck) {
                when (t) {
                    CallLog.Calls.INCOMING_TYPE -> acc.incoming++
                    CallLog.Calls.OUTGOING_TYPE -> acc.outgoing++
                    CallLog.Calls.MISSED_TYPE -> acc.missed++
                }
            }
        }

        val topContacts = contactStatsMap.values
            .sortedByDescending { it.totalDuration }
            .take(20)
            .map { acc ->
                TopContactStat(
                    number = acc.number,
                    name = acc.name,
                    photoUri = acc.photoUri,
                    totalDurationSeconds = acc.totalDuration,
                    totalCalls = acc.totalCalls,
                    incomingCount = acc.incoming,
                    outgoingCount = acc.outgoing,
                    missedCount = acc.missed
                )
            }

        val avgDuration = if (totalCalls > 0) totalTalkTime / totalCalls else 0L

        _analytics.value = CallAnalyticsSummary(
            totalTalkTimeSeconds = totalTalkTime,
            totalCalls = totalCalls,
            incomingCalls = incoming,
            outgoingCalls = outgoing,
            missedCalls = missed,
            rejectedCalls = rejected,
            avgDurationSeconds = avgDuration,
            topContacts = topContacts,
            hourlyDistribution = hourly,
            simUsage = simMap
        )
    }

    private fun calculateCutoffTime(range: AnalyticsTimeRange): Long {
        val cal = Calendar.getInstance()
        return when (range) {
            AnalyticsTimeRange.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            AnalyticsTimeRange.THIS_WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            AnalyticsTimeRange.THIS_MONTH -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis
            }
            AnalyticsTimeRange.ALL_TIME -> 0L
        }
    }

    private class MutableContactAccumulator(
        val number: String,
        val name: String,
        val photoUri: String?,
        var totalDuration: Long = 0L,
        var totalCalls: Int = 0,
        var incoming: Int = 0,
        var outgoing: Int = 0,
        var missed: Int = 0
    )
}
