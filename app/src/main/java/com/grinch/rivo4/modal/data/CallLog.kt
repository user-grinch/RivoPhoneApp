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
    val isHd: Boolean get() = (features and (4 or 8 or 64 or 128 or 256)) != 0 || subLogs.any { it.isHd }
    val count: Int get() = types.size.coerceAtLeast(1)
}
