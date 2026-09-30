package com.grinch.rivo4.modal.db

import androidx.room.*

@Dao
interface TrashedContactDao {
    @Query("SELECT * FROM trashed_contacts ORDER BY trashedAt DESC")
    fun getAll(): List<TrashedContactEntity>

    @Query("SELECT * FROM trashed_contacts WHERE localId = :id")
    fun getById(id: Long): TrashedContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: TrashedContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<TrashedContactEntity>)

    @Query("DELETE FROM trashed_contacts WHERE localId = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM trashed_contacts")
    fun deleteAll()

    @Query("DELETE FROM trashed_contacts WHERE trashedAt < :olderThanTimestamp")
    fun pruneOlderThan(olderThanTimestamp: Long): Int
}
