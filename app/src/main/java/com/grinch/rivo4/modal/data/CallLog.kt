package com.grinch.rivo4.modal.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long,
    val photoUri: String?,
    val contactId: String?,
    val simLabel: String? = null,
    val simNumber: Int? = null,
    val isBlocked: Boolean = false,
    val features: Int = 0,
    val types: List<Int> = emptyList(),
    val ids: List<Long> = emptyList(),
    val subLogs: List<CallLogEntry> = emptyList()
) {
    companion object {
        const val FEATURES_WIFI = 8
        const val FEATURES_VOLTE = 64
    }

    val isVolte: Boolean get() = (features and FEATURES_VOLTE) != 0 && (features and FEATURES_WIFI) == 0
    val isHd: Boolean get() = isVolte || subLogs.any { it.isVolte }
    val count: Int get() = types.size.coerceAtLeast(1)
}
