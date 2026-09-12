package com.grinch.rivo4.modal.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "callback_reminders")
data class CallbackReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val remindAtMillis: Long,
    val note: String? = null,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
