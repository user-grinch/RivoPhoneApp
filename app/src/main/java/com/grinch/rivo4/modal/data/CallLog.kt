package com.grinch.rivo4.modal.data

data class CallLogEntry(
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long,
    val photoUri: String?,
    val contactId: String?,
    val types: List<Int> = emptyList()
) {
    val count: Int get() = types.size.coerceAtLeast(1)
}