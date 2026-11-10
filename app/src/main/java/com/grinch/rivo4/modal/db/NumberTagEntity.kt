package com.grinch.rivo4.modal.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "number_tags")
data class NumberTagEntity(
    @PrimaryKey
    val phoneNumber: String, // Normalized phone number
    val tag: String,         // Short tag, up to 30 chars
    val rawNumber: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
