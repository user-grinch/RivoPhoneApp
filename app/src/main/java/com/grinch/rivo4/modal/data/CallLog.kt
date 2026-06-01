package com.grinch.rivo4.modal.data

import kotlinx.serialization.Serializable

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
    val isBlocked: Boolean = false,
    val types: List<Int> = emptyList(),
    val ids: List<Long> = emptyList()
) {
    val count: Int get() = types.size.coerceAtLeast(1)
}
