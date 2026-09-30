package com.grinch.rivo4.modal.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PrivateContactEntity::class,
        CallNoteEntity::class,
        CallbackReminderEntity::class,
        TrashedContactEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class RivoDatabase : RoomDatabase() {
    abstract fun privateContactDao(): PrivateContactDao
    abstract fun callNoteDao(): CallNoteDao
    abstract fun callbackReminderDao(): CallbackReminderDao
    abstract fun trashedContactDao(): TrashedContactDao
}
