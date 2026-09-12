package com.grinch.rivo4.modal.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallbackReminderDao {

    @Query("SELECT * FROM callback_reminders WHERE isCompleted = 0 ORDER BY remindAtMillis ASC")
    fun getActiveReminders(): Flow<List<CallbackReminderEntity>>

    @Query("SELECT * FROM callback_reminders ORDER BY remindAtMillis DESC")
    fun getAllReminders(): Flow<List<CallbackReminderEntity>>

    @Query("SELECT * FROM callback_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): CallbackReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: CallbackReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: CallbackReminderEntity)

    @Query("UPDATE callback_reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("DELETE FROM callback_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}
