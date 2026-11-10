package com.grinch.rivo4.modal.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NumberTagDao {

    @Query("SELECT * FROM number_tags ORDER BY updatedAt DESC")
    fun getAllTags(): Flow<List<NumberTagEntity>>

    @Query("SELECT * FROM number_tags")
    suspend fun getAllTagsList(): List<NumberTagEntity>

    @Query("SELECT * FROM number_tags WHERE phoneNumber = :number LIMIT 1")
    suspend fun getTagForNumber(number: String): NumberTagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tag: NumberTagEntity)

    @Query("DELETE FROM number_tags WHERE phoneNumber = :number")
    suspend fun deleteTag(number: String)
}
