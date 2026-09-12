package com.grinch.rivo4.controller.util

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import com.grinch.rivo4.modal.data.Contact
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

data class BlockedNumber(
    val id: Long,
    val originalNumber: String,
    val strippedNumber: String?
)

data class BlocklistImportResult(
    val totalParsed: Int,
    val newlyBlocked: Int,
    val alreadyBlocked: Int,
    val failed: Int
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

    fun exportToCsv(
        context: Context,
        outputStream: OutputStream,
        contacts: List<Contact> = emptyList()
    ): Result<Int> {
        return try {
            val blocked = getAll(context)
            val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
            writer.write("\"Phone Number\",\"Name\"\n")

            blocked.forEach { b ->
                val matchedName = contacts.find { c ->
                    c.phoneNumbers.any { areNumbersEqual(it, b.originalNumber) }
                }?.name ?: ""

                val safeNumber = b.originalNumber.replace("\"", "\"\"")
                val safeName = matchedName.replace("\"", "\"\"")
                writer.write("\"$safeNumber\",\"$safeName\"\n")
            }
            writer.flush()
            writer.close()
            Result.success(blocked.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importFromCsv(
        context: Context,
        inputStream: InputStream
    ): Result<BlocklistImportResult> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            var totalParsed = 0
            var newlyBlocked = 0
            var alreadyBlocked = 0
            var failed = 0

            var isFirstLine = true
            reader.forEachLine { rawLine ->
                var line = rawLine.trim()
                if (isFirstLine) {
                    isFirstLine = false
                    if (line.startsWith("\uFEFF")) {
                        line = line.removePrefix("\uFEFF").trim()
                    }
                }

                if (line.isNotBlank() && !line.startsWith("#") && !line.startsWith("//")) {
                    val delimiter = if (line.contains(",")) ',' else if (line.contains(";")) ';' else '\t'
                    val tokens = line.split(delimiter).map { token ->
                        token.trim().trim('"', '\'')
                    }

                    val isHeader = tokens.any {
                        val lower = it.lowercase()
                        lower == "phone number" || lower == "number" || lower == "phone" || lower == "tel" || lower == "mobile"
                    }

                    if (!isHeader) {
                        val candidate = tokens.firstOrNull { col ->
                            val digits = col.count { it.isDigit() }
                            digits >= 3
                        } ?: tokens.firstOrNull { it.isNotBlank() }

                        if (!candidate.isNullOrBlank()) {
                            val cleaned = candidate.trim()
                            if (cleaned.any { it.isDigit() }) {
                                totalParsed++
                                if (isBlocked(context, cleaned)) {
                                    alreadyBlocked++
                                } else {
                                    val success = block(context, cleaned)
                                    if (success) {
                                        newlyBlocked++
                                    } else {
                                        failed++
                                    }
                                }
                            }
                        }
                    }
                }
            }
            reader.close()
            Result.success(BlocklistImportResult(totalParsed, newlyBlocked, alreadyBlocked, failed))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
