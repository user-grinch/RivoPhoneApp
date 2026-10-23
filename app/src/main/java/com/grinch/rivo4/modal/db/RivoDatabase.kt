package com.grinch.rivo4.modal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PrivateContactEntity::class,
        CallNoteEntity::class,
        CallbackReminderEntity::class,
        TrashedContactEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class RivoDatabase : RoomDatabase() {
    abstract fun privateContactDao(): PrivateContactDao
    abstract fun callNoteDao(): CallNoteDao
    abstract fun callbackReminderDao(): CallbackReminderDao
    abstract fun trashedContactDao(): TrashedContactDao

    companion object {
        /**
         * v5 → v6: adds RFC 6350 fields (typed addresses, organizations, websites,
         * relations) to the private contacts vault as nullable JSON columns. Existing
         * rows keep their data; new columns decode to safe defaults.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN addressEntriesJson TEXT")
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN organizationsJson TEXT")
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN websitesJson TEXT")
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN relationsJson TEXT")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_5_6)
    }
}
