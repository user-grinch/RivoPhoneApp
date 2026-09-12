package com.grinch.rivo4.modal.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_notes")
data class CallNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
