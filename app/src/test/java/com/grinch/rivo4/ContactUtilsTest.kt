package com.grinch.rivo4

import com.grinch.rivo4.controller.util.CallBackgroundStore
import com.grinch.rivo4.controller.util.deduplicateNumbers
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.ContactEvent
import com.grinch.rivo4.modal.db.PrivateContactEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactUtilsTest {

    @Test
    fun testPrivateContactEntityRoundTrip() {
        val contact = Contact(
            id = "p42",
            name = "Jane Doe",
            nickname = "JD",
            phoneNumbers = listOf("+1234567890", "+1987654321"),
            emails = listOf("jane@example.com"),
            addresses = listOf("123 Main St"),
            events = listOf(ContactEvent(type = 3, label = "Birthday", date = "1990-01-01")),
            photoUri = "content://media/photo/42",
            isFavorite = true,
            customRingtone = "content://media/audio/ringtone",
            isPrivate = true,
            isHidden = false,
            notes = "VIP Client"
        )

        val entity = PrivateContactEntity.fromContact(contact)
        assertEquals(42L, entity.localId)
        assertEquals("Jane Doe", entity.name)
        assertEquals("JD", entity.nickname)
        assertTrue(entity.phoneNumbersJson.contains("+1234567890"))
        assertTrue(entity.emailsJson.contains("jane@example.com"))
        assertTrue(entity.eventsJson.contains("Birthday"))
        assertTrue(entity.isFavorite)
        assertEquals("content://media/audio/ringtone", entity.customRingtone)
        assertEquals("VIP Client", entity.notes)

        val restored = entity.toContact()
        assertEquals("p42", restored.id)
        assertEquals(contact.name, restored.name)
        assertEquals(contact.nickname, restored.nickname)
        assertEquals(contact.phoneNumbers, restored.phoneNumbers)
        assertEquals(contact.emails, restored.emails)
        assertEquals(contact.addresses, restored.addresses)
        assertEquals(contact.events.size, restored.events.size)
        assertEquals(contact.events[0].label, restored.events[0].label)
        assertEquals(contact.photoUri, restored.photoUri)
        assertEquals(contact.isFavorite, restored.isFavorite)
        assertEquals(contact.customRingtone, restored.customRingtone)
        assertTrue(restored.isPrivate)
        assertEquals(contact.notes, restored.notes)
    }

    @Test
    fun testCallBackgroundStoreNumberKey() {
        // Last 9 digits or all digits
        val key1 = CallBackgroundStore.numberKey("+1 555-123-4567")
        assertNotNull(key1)
        assertEquals("551234567", key1)

        val key2 = CallBackgroundStore.numberKey("12345")
        assertEquals("12345", key2)

        val keyNull = CallBackgroundStore.numberKey("")
        assertEquals(null, keyNull)
    }

    @Test
    fun testCallBackgroundStoreNumberKeys() {
        val numbers = listOf("+1 555-123-4567", "551234567", "9876543210")
        val keys = CallBackgroundStore.numberKeys(numbers)
        // +1 555-123-4567 and 551234567 should produce same 9-digit key and be distinct
        assertEquals(2, keys.size)
        assertTrue(keys.contains("551234567"))
        assertTrue(keys.contains("876543210"))
    }

    @Test
    fun testDeduplicateNumbersExact() {
        val numbers = listOf("1234567", "1234567", "7654321")
        val result = deduplicateNumbers(numbers)
        assertEquals(2, result.size)
        assertEquals("1234567", result[0])
        assertEquals("7654321", result[1])
    }

    @Test
    fun testContactModelDefaults() {
        val contact = Contact(
            id = "101",
            name = "Test Contact"
        )
        assertFalse(contact.isFavorite)
        assertFalse(contact.isPrivate)
        assertFalse(contact.isHidden)
        assertTrue(contact.phoneNumbers.isEmpty())
        assertTrue(contact.emails.isEmpty())
        assertEquals(null, contact.accountName)
        assertEquals(null, contact.accountType)
    }
}
