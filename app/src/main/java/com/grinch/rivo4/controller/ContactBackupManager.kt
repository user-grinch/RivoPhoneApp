package com.grinch.rivo4.controller

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.data.Contact
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class ContactBackupManager(
    private val contactsRepo: IContactsRepository,
    private val preferenceManager: PreferenceManager
) {
    fun exportContacts(context: Context, uri: Uri) {
        val bytes = buildVCard(contactsRepo.getContacts()).toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { it.write(encode(bytes)) }
            ?: throw IllegalArgumentException("Could not open backup destination")
    }

    fun exportAutomatic(context: Context) {
        val directoryUri = preferenceManager.getContactBackupDirectoryUri()
            ?: throw IllegalStateException("Choose a local backup folder first")
        val directory = DocumentFile.fromTreeUri(context, Uri.parse(directoryUri))
            ?: throw IllegalStateException("The selected backup folder is unavailable")
        if (!directory.canWrite()) throw IllegalStateException("The selected backup folder is not writable")

        val extension = if (isEncryptionEnabled()) ".vcf.enc" else ".vcf"
        val fileName = "$AUTO_FILE_PREFIX${
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        }$extension"
        directory.findFile(LEGACY_PLAIN_NAME)?.delete()
        directory.findFile(LEGACY_ENCRYPTED_NAME)?.delete()
        val file = directory.createFile(
            if (isEncryptionEnabled()) "application/octet-stream" else "text/vcard",
            fileName
        ) ?: throw IllegalStateException("Could not create the automatic backup file")
        context.contentResolver.openOutputStream(file.uri)?.use {
            it.write(encode(buildVCard(contactsRepo.getContacts()).toByteArray(Charsets.UTF_8)))
        } ?: throw IllegalStateException("Could not write the automatic backup file")
        pruneAutomaticBackups(directory)
    }

    fun readContacts(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read backup file")
        return decode(bytes).toString(Charsets.UTF_8)
    }

    fun isEncryptionEnabled(): Boolean = preferenceManager.isContactBackupEncryptionEnabled()

    private fun encode(plainText: ByteArray): ByteArray {
        if (!isEncryptionEnabled()) return plainText
        require(preferenceManager.getContactBackupPassword().isNotEmpty()) {
            "Set a backup password before enabling encryption"
        }
        val salt = ByteArray(SALT_SIZE)
        val nonce = ByteArray(NONCE_SIZE)
        SecureRandom().nextBytes(salt)
        SecureRandom().nextBytes(nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key(salt), GCMParameterSpec(TAG_SIZE_BITS, nonce))
        }
        val encrypted = cipher.doFinal(plainText)
        return ByteArrayOutputStream().apply {
            write(MAGIC)
            write(VERSION)
            write(salt)
            write(nonce)
            write(encrypted)
        }.toByteArray()
    }

    private fun decode(bytes: ByteArray): ByteArray {
        if (!bytes.startsWithMagic()) return bytes
        if (bytes.size <= HEADER_SIZE) throw IllegalArgumentException("Invalid encrypted contact backup")
        val buffer = ByteBuffer.wrap(bytes)
        if (buffer.get(MAGIC.size).toInt() != VERSION) {
            throw IllegalArgumentException("Unsupported encrypted contact backup version")
        }
        buffer.position(MAGIC.size + 1)
        val salt = ByteArray(SALT_SIZE).also(buffer::get)
        val nonce = ByteArray(NONCE_SIZE).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(salt), GCMParameterSpec(TAG_SIZE_BITS, nonce))
            }.doFinal(encrypted)
        } catch (e: Exception) {
            throw IllegalArgumentException("Incorrect backup password or corrupted backup", e)
        }
    }

    private fun key(salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(
            preferenceManager.getContactBackupPassword().toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_SIZE_BITS
        )
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES"
            )
        } finally {
            spec.clearPassword()
        }
    }

    private fun buildVCard(contacts: List<Contact>): String = buildString {
        contacts.forEach { contact ->
            append("BEGIN:VCARD\nVERSION:3.0\n")
            append("FN:${contact.name}\n")
            contact.phoneNumbers.forEach { append("TEL;TYPE=CELL:$it\n") }
            contact.emails.forEach { append("EMAIL;TYPE=INTERNET:$it\n") }
            contact.addresses.forEach { append("ADR:;;${it.replace("\n", "\\n")}\n") }
            if (!contact.nickname.isNullOrBlank()) append("NICKNAME:${contact.nickname}\n")
            contact.organizations.filter { it.company.isNotBlank() }.forEach {
                append("ORG:${it.company}\n")
                if (!it.title.isNullOrBlank()) append("TITLE:${it.title}\n")
            }

            contact.websites.filter { it.url.isNotBlank() }.forEach { append("URL:${it.url}\n") }
            contact.events.forEach {
                if (it.date.isNotBlank()) {
                    when (it.type) {
                        android.provider.ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> append("BDAY:${it.date}\n")
                        android.provider.ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> append("ANNIVERSARY:${it.date}\n")
                    }
                }
            }
            if (!contact.notes.isNullOrBlank()) append("NOTE:${contact.notes.replace("\n", "\\n")}\n")
            append("END:VCARD\n")
        }
    }

    private fun pruneAutomaticBackups(directory: DocumentFile) {
        val retentionCount = preferenceManager.getContactBackupRetentionCount()
        val backups = directory.listFiles()
            .filter { it.name?.startsWith(AUTO_FILE_PREFIX) == true }
            .sortedByDescending { it.name }
        backups.drop(retentionCount).forEach { it.delete() }
    }

    private fun ByteArray.startsWithMagic(): Boolean =
        size >= MAGIC.size + 1 && copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    companion object {
        private val MAGIC = "RIVO-VCF".toByteArray(Charsets.US_ASCII)
        private const val VERSION: Int = 1
        private const val SALT_SIZE = 16
        private const val NONCE_SIZE = 12
        private const val TAG_SIZE_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val PBKDF2_ITERATIONS = 120_000
        private const val HEADER_SIZE = 8 + 1 + SALT_SIZE + NONCE_SIZE
        private const val AUTO_FILE_PREFIX = "contacts_auto_backup_"
        private const val LEGACY_PLAIN_NAME = "contacts_auto_backup.vcf"
        private const val LEGACY_ENCRYPTED_NAME = "contacts_auto_backup.vcf.enc"
    }
}
