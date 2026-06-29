package com.grinch.rivo4.modal.db

import androidx.room.*

@Dao
interface PrivateContactDao {
    @Query("SELECT * FROM private_contacts ORDER BY name ASC")
    fun getAll(): List<PrivateContactEntity>

    @Query("SELECT * FROM private_contacts WHERE localId = :id")
    fun getById(id: Long): PrivateContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contact: PrivateContactEntity): Long

    @Update
    fun update(contact: PrivateContactEntity)

    @Delete
    fun delete(contact: PrivateContactEntity)

    @Query("DELETE FROM private_contacts WHERE localId = :id")
    fun deleteById(id: Long)
}
