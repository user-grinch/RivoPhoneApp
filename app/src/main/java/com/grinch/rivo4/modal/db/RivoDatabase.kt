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
        TrashedContactEntity::class,
        NumberTagEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class RivoDatabase : RoomDatabase() {
    abstract fun privateContactDao(): PrivateContactDao
    abstract fun callNoteDao(): CallNoteDao
    abstract fun callbackReminderDao(): CallbackReminderDao
    abstract fun trashedContactDao(): TrashedContactDao
    abstract fun numberTagDao(): NumberTagDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN addressEntriesJson TEXT")
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN organizationsJson TEXT")
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN websitesJson TEXT")
                db.execSQL("ALTER TABLE private_contacts ADD COLUMN relationsJson TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `number_tags` (
                        `phoneNumber` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `rawNumber` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`phoneNumber`)
                    )
                    """.trimIndent()
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_5_6, MIGRATION_6_7)
    }
}
