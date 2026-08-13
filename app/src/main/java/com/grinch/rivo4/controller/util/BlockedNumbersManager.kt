package com.grinch.rivo4.controller.util

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract

data class BlockedNumber(
    val id: Long,
    val originalNumber: String,
    val strippedNumber: String?
)

object BlockedNumbersManager {

    fun canBlockNumbers(context: Context): Boolean {
        return try {
            BlockedNumberContract.canCurrentUserBlockNumbers(context)
        } catch (e: Exception) {
            false
        }
    }

    fun isBlocked(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        return try {
            BlockedNumberContract.isBlocked(context, number)
        } catch (e: Exception) {
            false
        }
    }

    fun block(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        if (isBlocked(context, number)) return true
        return try {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            }
            context.contentResolver.insert(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                values
            ) != null
        } catch (e: Exception) {
            false
        }
    }

    fun unblock(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        return try {
            BlockedNumberContract.unblock(context, number)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun unblockById(context: Context, id: Long): Boolean {
        return try {
            val uri = android.content.ContentUris.withAppendedId(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                id
            )
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun getAll(context: Context): List<BlockedNumber> {
        val results = mutableListOf<BlockedNumber>()
        try {
            context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(
                    BlockedNumberContract.BlockedNumbers.COLUMN_ID,
                    BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                    BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_ID)
                val originalIndex = cursor.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                val e164Index = cursor.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER)
                while (cursor.moveToNext()) {
                    val original = cursor.getString(originalIndex) ?: continue
                    results.add(
                        BlockedNumber(
                            id = cursor.getLong(idIndex),
                            originalNumber = original,
                            strippedNumber = cursor.getString(e164Index)
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        return results
    }
}
