package com.grinch.rivo4.modal.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PrivateContactEntity::class], version = 2, exportSchema = false)
abstract class RivoDatabase : RoomDatabase() {
    abstract fun privateContactDao(): PrivateContactDao
}
