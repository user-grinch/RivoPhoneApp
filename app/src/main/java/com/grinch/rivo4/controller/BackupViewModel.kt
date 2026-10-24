package com.grinch.rivo4.controller

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.getAppVersion
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.SettingsBackupEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupViewModel(
    private val contactsRepo: IContactsRepository,
    private val callLogRepo: ICallLogRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val settingsJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    fun exportContacts(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contacts = contactsRepo.getContacts()
                val vCard = buildString {
                    contacts.forEach { contact ->
                        append("BEGIN:VCARD\n")
                        append("VERSION:3.0\n")
                        append("FN:${contact.name}\n")
                        contact.phoneNumbers.forEach { number ->
                            append("TEL;TYPE=CELL:$number\n")
                        }
                        contact.emails.forEach { email ->
                            append("EMAIL;TYPE=INTERNET:$email\n")
                        }
                        append("END:VCARD\n")
                    }
                }
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(vCard)
                    }
                }
                _status.value = context.getString(R.string.backup_contacts_export_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_export_failed, e.message)
            }
        }
    }

    fun importContacts(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        var line: String?
                        var currentName = ""
                        var currentNickname: String? = null
                        val currentNumbers = mutableListOf<String>()
                        val currentEmails = mutableListOf<String>()
                        val currentAddresses = mutableListOf<String>()
                        var currentNotes: String? = null
                        
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line!!.trim()
                            if (trimmed.equals("END:VCARD", ignoreCase = true)) {
                                if (currentName.isNotEmpty() || currentNumbers.isNotEmpty() || currentEmails.isNotEmpty()) {
                                    val resolvedName = currentName.ifBlank {
                                        currentNumbers.firstOrNull() ?: currentEmails.firstOrNull() ?: "Unknown"
                                    }
                                    contactsRepo.saveContact(
                                        Contact(
                                            id = "0",
                                            name = resolvedName,
                                            nickname = currentNickname,
                                            phoneNumbers = currentNumbers.toList(),
                                            emails = currentEmails.toList(),
                                            addresses = currentAddresses.toList(),
                                            notes = currentNotes
                                        )
                                    )
                                }
                                currentName = ""
                                currentNickname = null
                                currentNumbers.clear()
                                currentEmails.clear()
                                currentAddresses.clear()
                                currentNotes = null
                                continue
                            }
                            if (!trimmed.contains(":")) continue
                            val propKey = trimmed.substringBefore(":").substringBefore(";").substringAfterLast(".").uppercase()
                            val propVal = trimmed.substringAfter(":").trim()
                            if (propVal.isEmpty()) continue
                            when (propKey) {
                                "FN" -> currentName = propVal
                                "N" -> {
                                    if (currentName.isBlank()) {
                                        val parts = propVal.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                                        if (parts.isNotEmpty()) currentName = parts.reversed().joinToString(" ")
                                    }
                                }
                                "NICKNAME" -> currentNickname = propVal
                                "TEL" -> {
                                    val num = propVal.removePrefix("tel:").removePrefix("TEL:").trim()
                                    if (num.isNotEmpty()) currentNumbers.add(num)
                                }
                                "EMAIL" -> {
                                    val email = propVal.removePrefix("mailto:").removePrefix("MAILTO:").trim()
                                    if (email.isNotEmpty()) currentEmails.add(email)
                                }
                                "ADR" -> {
                                    val addr = propVal.split(";").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
                                    if (addr.isNotEmpty()) currentAddresses.add(addr)
                                }
                                "NOTE" -> currentNotes = propVal.replace("\\n", "\n")
                            }
                        }
                    }
                }
                _status.value = context.getString(R.string.backup_contacts_import_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_import_failed, e.message)
            }
        }
    }

    fun exportCallLogs(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logs = callLogRepo.getCallLogs()
                val jsonString = json.encodeToString(logs)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                _status.value = context.getString(R.string.backup_call_logs_export_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_export_failed, e.message)
            }
        }
    }

    fun importCallLogs(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = BufferedReader(InputStreamReader(inputStream)).readText()
                    val logs = json.decodeFromString<List<CallLogEntry>>(jsonString)
                    logs.forEach { log ->
                        if (log.subLogs.isNotEmpty()) {
                            log.subLogs.forEach { sub ->
                                callLogRepo.saveCallLog(sub)
                            }
                        } else {
                            callLogRepo.saveCallLog(log)
                        }
                    }
                }
                _status.value = context.getString(R.string.backup_call_logs_import_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_import_failed, e.message)
            }
        }
    }

    fun exportAppSettings(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (versionName, versionCode) = getAppVersion(context)
                val backup = preferenceManager.getAllExportableSettings(versionName, versionCode)
                val jsonString = settingsJson.encodeToString(backup)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(jsonString)
                    }
                }
                _status.value = context.getString(R.string.backup_settings_export_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_export_failed, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun importAppSettings(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                } ?: throw IllegalArgumentException("Could not read backup file")

                val backup = settingsJson.decodeFromString<SettingsBackupEnvelope>(jsonString)
                val count = preferenceManager.restoreSettings(backup)
                _status.value = context.getString(R.string.backup_settings_import_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_import_failed, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
