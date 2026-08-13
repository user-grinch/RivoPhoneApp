package com.grinch.rivo4.controller.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.SystemClock
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CallBackgroundStore {

    private const val DIRECTORY = "call_backgrounds"
    private const val DEFAULT_KEY = "default"
    private const val FILE_PREFIX = "bg_"
    private const val FILE_SUFFIX = ".jpg"
    private const val JPEG_QUALITY = 92
    private const val MIN_MATCH_DIGITS = 9
    private const val MAX_DIMENSION = 1920
    private const val FALLBACK_DIMENSION = 1280
    private const val MISS_TOLERANCE = 2
    private const val PRUNE_INTERVAL_MS = 15L * 60L * 1000L

    data class BackgroundResult(val uri: String?, val failed: Boolean)

    private data class NumberQuery(val numbers: List<String>, val failed: Boolean)

    private data class ContactLink(
        val confirmedId: String?,
        val confirmedNumbers: List<String>,
        val failed: Boolean
    )

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileState = ConcurrentHashMap<String, Boolean>()
    private val pendingChecks: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val missStrikes = ConcurrentHashMap<String, Int>()

    @Volatile
    private var lastPruneAt = 0L

    @Volatile
    private var cachedPrefs: PreferenceManager? = null

    fun attach(manager: PreferenceManager) {
        if (cachedPrefs != null) return
        synchronized(this) {
            if (cachedPrefs == null) cachedPrefs = manager
        }
    }

    private fun prefs(context: Context): PreferenceManager {
        val existing = cachedPrefs
        if (existing != null) return existing
        return synchronized(this) {
            cachedPrefs ?: PreferenceManager(context.applicationContext).also { cachedPrefs = it }
        }
    }

    fun numberKey(number: String?): String? {
        val digits = numberDigits(number)
        if (digits.isEmpty()) return null
        return if (digits.length > MIN_MATCH_DIGITS) digits.takeLast(MIN_MATCH_DIGITS) else digits
    }

    fun numberKeys(numbers: List<String>): List<String> {
        return numbers.mapNotNull { numberKey(it) }.distinct()
    }

    private fun numberDigits(number: String?): String {
        val raw = number?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        val normalized = try {
            normalizePhoneNumber(raw).filter { it.isDigit() }
        } catch (e: Exception) {
            ""
        }
        return if (normalized.isNotEmpty()) normalized else raw.filter { it.isDigit() }
    }

    fun peek(context: Context, contactId: String?, numbers: List<String>): String? {
        val manager = prefs(context.applicationContext)
        return storedCandidates(manager, contactId, numberKeys(numbers))
            .firstNotNullOfOrNull { cachedModel(it) }
    }

    fun peekStored(context: Context, contactId: String?, numbers: List<String>): String? {
        val manager = prefs(context.applicationContext)
        return storedCandidates(manager, contactId, numberKeys(numbers))
            .firstOrNull { cachedModel(it) != null }
    }

    suspend fun peekAsync(context: Context, contactId: String?, numbers: List<String>): String? =
        withContext(Dispatchers.IO) {
            val manager = prefs(context.applicationContext)
            storedCandidates(manager, contactId, numberKeys(numbers))
                .firstNotNullOfOrNull { toModel(it) }
        }

    suspend fun resolve(context: Context, phoneNumber: String?, contactId: String?): String? =
        resolveResult(context, phoneNumber, contactId).uri

    suspend fun resolveResult(
        context: Context,
        phoneNumber: String?,
        contactId: String?
    ): BackgroundResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val manager = prefs(app)
        val handleKey = numberKey(phoneNumber)
        var failed = false

        if (handleKey != null) {
            val stored = manager.getContactBackgroundForNumber(handleKey)
            if (stored != null) {
                val model = promote(app, manager, stored, null, listOf(handleKey))
                if (model != null) {
                    schedulePrune(app, manager)
                    return@withContext BackgroundResult(model, false)
                }
                if (isTransientMiss(stored)) failed = true
            }
        }

        if (handleKey == null) {
            if (!contactId.isNullOrBlank()) {
                val stored = manager.getContactBackground(contactId)
                if (stored != null) {
                    val model = materialize(app, manager, stored)
                    if (model != null) return@withContext BackgroundResult(model, false)
                    if (isTransientMiss(stored)) failed = true
                }
            }
            return@withContext BackgroundResult(null, failed)
        }

        val link = contactLink(app, phoneNumber, handleKey, contactId)
        if (link.failed) failed = true

        val confirmedId = link.confirmedId
        if (!confirmedId.isNullOrBlank()) {
            val stored = manager.getContactBackground(confirmedId)
            if (stored != null) {
                val model = promote(app, manager, stored, confirmedId, listOf(handleKey))
                if (model != null) {
                    schedulePrune(app, manager)
                    return@withContext BackgroundResult(model, false)
                }
                if (isTransientMiss(stored)) failed = true
            }
            numberKeys(link.confirmedNumbers).forEach { key ->
                if (key != handleKey) {
                    val related = manager.getContactBackgroundForNumber(key)
                    if (related != null) {
                        val model = promote(
                            app,
                            manager,
                            related,
                            confirmedId,
                            listOf(handleKey, key)
                        )
                        if (model != null) {
                            schedulePrune(app, manager)
                            return@withContext BackgroundResult(model, false)
                        }
                        if (isTransientMiss(related)) failed = true
                    }
                }
            }
        }

        val swept = sweep(app, manager, handleKey, numberDigits(phoneNumber))
        schedulePrune(app, manager)
        BackgroundResult(swept, swept == null && failed)
    }

    suspend fun save(context: Context, contactId: String?, numbers: List<String>, source: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val manager = prefs(app)
            val keys = numberKeys(numbers)
            if (contactId.isNullOrBlank() && keys.isEmpty()) return@withContext false
            val imported = importImage(app, source) ?: return@withContext false
            writePointers(manager, contactId, keys, imported.absolutePath)
            collectGarbage(app, manager)
            true
        }

    suspend fun saveDefault(context: Context, source: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val manager = prefs(app)
            val imported = importImage(app, source) ?: return@withContext false
            manager.updateContactBackgroundEntries(
                mapOf(manager.contactBackgroundIdKey(DEFAULT_KEY) to imported.absolutePath)
            )
            collectGarbage(app, manager)
            true
        }

    fun hasDefault(context: Context): Boolean {
        return prefs(context.applicationContext).getContactBackground(DEFAULT_KEY) != null
    }

    fun defaultModel(context: Context): String? {
        val manager = prefs(context.applicationContext)
        val stored = manager.getContactBackground(DEFAULT_KEY) ?: return null
        return cachedModel(stored)
    }

    suspend fun defaultModelAsync(context: Context): String? = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val manager = prefs(app)
        val stored = manager.getContactBackground(DEFAULT_KEY) ?: return@withContext null
        materialize(app, manager, stored)
    }

    fun clearDefault(context: Context) {
        val app = context.applicationContext
        val manager = prefs(app)
        if (manager.getContactBackground(DEFAULT_KEY) != null) {
            manager.updateContactBackgroundEntries(
                mapOf(manager.contactBackgroundIdKey(DEFAULT_KEY) to null)
            )
            scheduleGarbageCollection(app, manager)
        }
    }

    suspend fun clear(context: Context, contactId: String?, numbers: List<String>) {
        withContext(Dispatchers.IO) { clearBlocking(context, contactId, numbers) }
    }

    fun clearBlocking(context: Context, contactId: String?, numbers: List<String>) {
        val app = context.applicationContext
        val manager = prefs(app)
        val updates = mutableMapOf<String, String?>()
        if (!contactId.isNullOrBlank() && manager.getContactBackground(contactId) != null) {
            updates[manager.contactBackgroundIdKey(contactId)] = null
        }
        numberKeys(numbers).forEach { key ->
            if (manager.getContactBackgroundForNumber(key) != null) {
                updates[manager.contactBackgroundNumberKey(key)] = null
            }
        }
        if (updates.isEmpty()) return
        manager.updateContactBackgroundEntries(updates)
        scheduleGarbageCollection(app, manager)
    }

    fun carryBlocking(context: Context, fromContactId: String?, toContactId: String?, numbers: List<String>) {
        val app = context.applicationContext
        val manager = prefs(app)
        val keys = numberKeys(numbers)
        val stored = storedCandidates(manager, fromContactId, keys).firstOrNull { toModel(it) != null }
        if (stored != null) {
            writePointers(manager, toContactId, keys, stored)
        }
        if (!fromContactId.isNullOrBlank() && fromContactId != toContactId &&
            manager.getContactBackground(fromContactId) != null
        ) {
            manager.updateContactBackgroundEntries(
                mapOf(manager.contactBackgroundIdKey(fromContactId) to null)
            )
        }
        scheduleGarbageCollection(app, manager)
    }

    private fun storedCandidates(
        manager: PreferenceManager,
        contactId: String?,
        keys: List<String>
    ): List<String> {
        val candidates = mutableListOf<String>()
        if (!contactId.isNullOrBlank()) {
            manager.getContactBackground(contactId)?.let { candidates.add(it) }
        }
        keys.forEach { key ->
            manager.getContactBackgroundForNumber(key)?.let { candidates.add(it) }
        }
        return candidates.distinct()
    }

    private fun sweep(
        context: Context,
        manager: PreferenceManager,
        handleKey: String,
        fullDigits: String
    ): String? {
        manager.getContactBackgroundNumberEntries().forEach { (key, stored) ->
            if (strictKeyMatch(handleKey, fullDigits, key)) {
                val model = materialize(context, manager, stored)
                if (model != null) return model
            }
        }
        return null
    }

    private fun strictKeyMatch(handleKey: String, fullDigits: String, storedKey: String): Boolean {
        if (storedKey.isEmpty()) return false
        if (handleKey == storedKey) return true
        if (storedKey.length < MIN_MATCH_DIGITS) return false
        if (handleKey.length < MIN_MATCH_DIGITS) return false
        return fullDigits.length >= storedKey.length && fullDigits.endsWith(storedKey)
    }

    private fun promote(
        context: Context,
        manager: PreferenceManager,
        stored: String,
        contactId: String?,
        keys: List<String>
    ): String? {
        val effective = migrateIfNeeded(context, manager, stored) ?: return null
        writePointers(manager, contactId, keys.distinct(), effective)
        return toModel(effective)
    }

    private fun materialize(context: Context, manager: PreferenceManager, stored: String): String? {
        val effective = migrateIfNeeded(context, manager, stored) ?: return null
        return toModel(effective)
    }

    private fun migrateIfNeeded(context: Context, manager: PreferenceManager, stored: String): String? {
        if (isLocalPath(stored)) {
            val file = File(stored)
            if (file.exists() && file.length() > 0L) {
                fileState[stored] = true
                missStrikes.remove(stored)
                return stored
            }
            fileState[stored] = false
            val strikes = (missStrikes[stored] ?: 0) + 1
            if (strikes >= MISS_TOLERANCE) {
                missStrikes.remove(stored)
                dropValue(manager, stored)
            } else {
                missStrikes[stored] = strikes
            }
            return null
        }
        val imported = importImage(context, Uri.parse(stored))
        if (imported == null) return stored
        replaceValue(manager, stored, imported.absolutePath)
        collectGarbage(context, manager)
        return imported.absolutePath
    }

    private fun isTransientMiss(stored: String): Boolean {
        return missStrikes.containsKey(stored)
    }

    private fun writePointers(
        manager: PreferenceManager,
        contactId: String?,
        keys: List<String>,
        value: String
    ) {
        val updates = mutableMapOf<String, String?>()
        if (!contactId.isNullOrBlank() && manager.getContactBackground(contactId) != value) {
            updates[manager.contactBackgroundIdKey(contactId)] = value
        }
        keys.forEach { key ->
            if (manager.getContactBackgroundForNumber(key) != value) {
                updates[manager.contactBackgroundNumberKey(key)] = value
            }
        }
        if (updates.isEmpty()) return
        manager.updateContactBackgroundEntries(updates)
    }

    private fun dropValue(manager: PreferenceManager, value: String) {
        val updates = mutableMapOf<String, String?>()
        manager.getContactBackgroundEntries().forEach { (key, stored) ->
            if (stored == value) updates[key] = null
        }
        if (updates.isEmpty()) return
        manager.updateContactBackgroundEntries(updates)
    }

    private fun replaceValue(manager: PreferenceManager, oldValue: String, newValue: String) {
        val updates = mutableMapOf<String, String?>()
        manager.getContactBackgroundEntries().forEach { (key, stored) ->
            if (stored == oldValue) updates[key] = newValue
        }
        if (updates.isEmpty()) return
        manager.updateContactBackgroundEntries(updates)
    }

    private fun isLocalPath(value: String): Boolean {
        return value.startsWith("/")
    }

    private fun toModel(stored: String): String? {
        if (stored.isBlank()) return null
        if (!isLocalPath(stored)) return stored
        val file = File(stored)
        val usable = file.exists() && file.length() > 0L
        fileState[stored] = usable
        if (!usable) return null
        return Uri.fromFile(file).toString()
    }

    private fun cachedModel(stored: String): String? {
        if (stored.isBlank()) return null
        if (!isLocalPath(stored)) return stored
        val known = fileState[stored]
        if (known == false) return null
        if (known == null) verifyAsync(stored)
        return Uri.fromFile(File(stored)).toString()
    }

    private fun verifyAsync(path: String) {
        if (!pendingChecks.add(path)) return
        ioScope.launch {
            try {
                val file = File(path)
                fileState[path] = file.exists() && file.length() > 0L
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingChecks.remove(path)
            }
        }
    }

    private fun storageDir(context: Context): File? {
        return deviceProtectedDir(context) ?: regularDir(context)
    }

    private fun deviceProtectedDir(context: Context): File? {
        return try {
            val base = context.createDeviceProtectedStorageContext() ?: return null
            File(base.filesDir, DIRECTORY).takeIf { it.exists() || it.mkdirs() }
        } catch (e: Exception) {
            null
        }
    }

    private fun regularDir(context: Context): File? {
        return try {
            File(context.filesDir, DIRECTORY).takeIf { it.exists() || it.mkdirs() }
        } catch (e: Exception) {
            null
        }
    }

    private fun openStream(context: Context, source: Uri): InputStream? {
        val resolver = context.contentResolver
        runCatching { resolver.openInputStream(source) }.getOrNull()?.let { return it }
        return runCatching {
            resolver.openAssetFileDescriptor(source, "r")?.createInputStream()
        }.getOrNull()
    }

    private fun scheduleGarbageCollection(context: Context, manager: PreferenceManager) {
        ioScope.launch { collectGarbage(context, manager) }
    }

    private fun collectGarbage(context: Context, manager: PreferenceManager) {
        val dir = storageDir(context) ?: return
        val referenced = manager.getContactBackgroundEntries().values.toSet()
        try {
            dir.listFiles()?.forEach { file ->
                val path = file.absolutePath
                if (!referenced.contains(path) && file.delete()) {
                    fileState[path] = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun schedulePrune(context: Context, manager: PreferenceManager) {
        val now = SystemClock.elapsedRealtime()
        if (lastPruneAt != 0L && now - lastPruneAt < PRUNE_INTERVAL_MS) return
        lastPruneAt = now
        ioScope.launch { pruneStaleIdKeys(context, manager) }
    }

    private fun pruneStaleIdKeys(context: Context, manager: PreferenceManager) {
        if (!hasContactsPermission(context)) return
        val existing = mutableSetOf<String>()
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.let { existing.add(it) }
                }
            } ?: return
        } catch (e: Exception) {
            return
        }
        if (existing.isEmpty()) return

        val idPrefix = PreferenceManager.CONTACT_BACKGROUND_PREFIX
        val numberPrefix = PreferenceManager.CONTACT_BACKGROUND_NUMBER_PREFIX
        val updates = mutableMapOf<String, String?>()
        manager.getContactBackgroundEntries().keys.forEach { key ->
            if (!key.startsWith(numberPrefix) && key.startsWith(idPrefix)) {
                val id = key.removePrefix(idPrefix)
                if (id.toLongOrNull() != null && !existing.contains(id)) {
                    updates[key] = null
                }
            }
        }
        if (updates.isEmpty()) return
        manager.updateContactBackgroundEntries(updates)
        collectGarbage(context, manager)
    }

    private fun hasContactsPermission(context: Context): Boolean {
        return try {
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private fun importImage(context: Context, source: Uri): File? {
        val dir = storageDir(context) ?: return null
        val bitmap = try {
            decodeScaled(context, source, targetSize(context))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return copyRaw(context, dir, source)
        return try {
            val target = File(dir, FILE_PREFIX + UUID.randomUUID().toString() + FILE_SUFFIX)
            FileOutputStream(target).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                output.flush()
            }
            if (target.length() <= 0L) {
                target.delete()
                null
            } else {
                fileState[target.absolutePath] = true
                missStrikes.remove(target.absolutePath)
                target
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun copyRaw(context: Context, dir: File, source: Uri): File? {
        return try {
            val target = File(dir, FILE_PREFIX + UUID.randomUUID().toString() + FILE_SUFFIX)
            openStream(context, source)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return null
            if (target.length() <= 0L) {
                target.delete()
                null
            } else {
                fileState[target.absolutePath] = true
                missStrikes.remove(target.absolutePath)
                target
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun targetSize(context: Context): Int {
        val metrics = context.resources.displayMetrics
        val largest = maxOf(metrics.widthPixels, metrics.heightPixels)
        return if (largest > 0) minOf(largest, MAX_DIMENSION) else FALLBACK_DIMENSION
    }

    private fun decodeScaled(context: Context, source: Uri, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxSize) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = openStream(context, source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val oriented = applyOrientation(context, source, decoded)
        val largest = maxOf(oriented.width, oriented.height)
        if (largest <= maxSize) return oriented

        val scale = maxSize.toFloat() / largest.toFloat()
        val scaledWidth = (oriented.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (oriented.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(oriented, scaledWidth, scaledHeight, true)
        if (scaled !== oriented) oriented.recycle()
        return scaled
    }

    private fun applyOrientation(context: Context, source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            openStream(context, source)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun contactLink(
        context: Context,
        phoneNumber: String?,
        handleKey: String,
        contactId: String?
    ): ContactLink {
        if (phoneNumber.isNullOrBlank()) return ContactLink(null, emptyList(), false)
        if (!hasContactsPermission(context)) return ContactLink(null, emptyList(), true)

        var failed = false

        if (!contactId.isNullOrBlank()) {
            val direct = numbersForContactId(context, contactId)
            if (direct.failed) failed = true
            if (numberKeys(direct.numbers).contains(handleKey)) {
                return ContactLink(contactId, direct.numbers, failed)
            }
        }

        val lookupId = lookupContactId(context, phoneNumber)
        if (lookupId == null) {
            return ContactLink(null, emptyList(), true)
        }
        if (lookupId.isBlank() || lookupId == contactId) {
            return ContactLink(null, emptyList(), failed)
        }

        val matched = numbersForContactId(context, lookupId)
        if (matched.failed) failed = true
        if (numberKeys(matched.numbers).contains(handleKey)) {
            return ContactLink(lookupId, matched.numbers, failed)
        }
        return ContactLink(null, emptyList(), failed)
    }

    private fun numbersForContactId(context: Context, contactId: String): NumberQuery {
        val numbers = mutableListOf<String>()
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            ) ?: return NumberQuery(emptyList(), true)
            cursor.use {
                while (it.moveToNext()) {
                    it.getString(0)?.let { number -> numbers.add(number) }
                }
            }
            NumberQuery(numbers, false)
        } catch (e: Exception) {
            NumberQuery(emptyList(), true)
        }
    }

    private fun lookupContactId(context: Context, number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            var found = ""
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.CONTACT_ID),
                null,
                null,
                null
            ) ?: return null
            cursor.use {
                if (it.moveToFirst()) found = it.getString(0).orEmpty()
            }
            found
        } catch (e: Exception) {
            null
        }
    }
}
