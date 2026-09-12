package com.grinch.rivo4.modal.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallNoteDao {

    @Query("SELECT * FROM call_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<CallNoteEntity>>

    @Query("SELECT * FROM call_notes WHERE phoneNumber = :number ORDER BY timestamp DESC")
    fun getNotesForNumber(number: String): Flow<List<CallNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CallNoteEntity): Long

    @Update
    suspend fun updateNote(note: CallNoteEntity)

    @Delete
    suspend fun deleteNote(note: CallNoteEntity)

    @Query("DELETE FROM call_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}
