package com.grinch.rivo4

import android.provider.CallLog
import android.provider.ContactsContract
import com.grinch.rivo4.controller.util.QrCodeUtils
import com.grinch.rivo4.controller.util.areNumbersEqual
import com.grinch.rivo4.controller.util.normalizePhoneNumber
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.CallLogFilter
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.ContactEvent
import com.grinch.rivo4.modal.data.EmailEntry
import com.grinch.rivo4.modal.data.PhoneNumberEntry
import com.grinch.rivo4.modal.db.PrivateContactEntity
import com.grinch.rivo4.modal.db.TrashedContactEntity
import com.grinch.rivo4.view.screen.T9Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ComprehensiveFeatureTest {

    @Test
    fun testPrivateContactEntityFullRoundTrip() {
        val original = Contact(
            id = "p42",
            name = "Dr. Jane M. Doe Jr.",
            prefix = "Dr.",
            givenName = "Jane",
            middleName = "M.",
            familyName = "Doe",
            suffix = "Jr.",
            nickname = "Janie",
            phoneNumbers = emptyList(), // Verify fallback to phones list
            emails = emptyList(),       // Verify fallback to emailEntries list
            phones = listOf(
                PhoneNumberEntry(number = "+1 555-0100", type = 2, label = "Mobile"),
                PhoneNumberEntry(number = "+1 555-0199", type = 3, label = "Work")
            ),
            emailEntries = listOf(
                EmailEntry(address = "jane@example.com", type = 1, label = "Home")
            ),
            addresses = listOf("123 Elm St, Gotham"),
            events = listOf(
                ContactEvent(
                    type = ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                    label = "Birthday",
                    date = "1992-05-14"
                )
            ),
            photoUri = "file:///data/user/0/com.grinch.rivo4/files/private_photos/photo_1.jpg",
            isFavorite = true,
            customRingtone = "content://media/internal/audio/media/42",
            isPrivate = true,
            isHidden = true,
            notes = "VIP private contact"
        )

        val entity = PrivateContactEntity.fromContact(original)
        assertEquals(42L, entity.localId)
        val restored = entity.toContact()

        assertEquals("p42", restored.id)
        assertEquals("Dr. Jane M. Doe Jr.", restored.name)
        assertEquals("Janie", restored.nickname)
        assertEquals(listOf("+1 555-0100", "+1 555-0199"), restored.phoneNumbers)
        assertEquals(2, restored.phones.size)
        assertEquals(listOf("jane@example.com"), restored.emails)
        assertEquals(1, restored.emailEntries.size)
        assertEquals(listOf("123 Elm St, Gotham"), restored.addresses)
        assertEquals(1, restored.events.size)
        assertEquals("1992-05-14", restored.events[0].date)
        assertTrue(restored.isFavorite)
        assertTrue(restored.isPrivate)
        assertTrue(restored.isHidden)
        assertEquals("VIP private contact", restored.notes)
        assertEquals("content://media/internal/audio/media/42", restored.customRingtone)
    }

    @Test
    fun testTrashedContactEntitySchemaEvolutionAndRoundTrip() {
        val contact = Contact(
            id = "99",
            name = "Bob Builder",
            givenName = "Bob",
            familyName = "Builder",
            phoneNumbers = listOf("01711223344"),
            emails = listOf("bob@builder.com"),
            events = listOf(ContactEvent(3, null, "1985-10-20")),
            notes = "Can we fix it?"
        )
        val trashed = TrashedContactEntity.fromContact(contact)
        val restored = trashed.toContact()
        assertNotNull(restored)
        assertEquals("Bob Builder", restored!!.name)
        assertEquals(listOf("01711223344"), restored.phoneNumbers)
        assertEquals(1, restored.events.size)

        // Simulate a future or legacy JSON containing unknown keys
        val jsonWithUnknownKeys = """
            {
                "id": "99",
                "name": "Bob Builder",
                "phoneNumbers": ["01711223344"],
                "futureUnknownField": "some_value",
                "anotherRemovedFlag": true
            }
        """.trimIndent()
        val evolvedEntity = trashed.copy(contactJson = jsonWithUnknownKeys)
        val evolvedContact = evolvedEntity.toContact()
        assertNotNull("TrashedContactEntity must ignore unknown JSON keys", evolvedContact)
        assertEquals("Bob Builder", evolvedContact!!.name)
        assertEquals(listOf("01711223344"), evolvedContact.phoneNumbers)
    }

    @Test
    fun testCallLogFilterContactsAccuracy() {
        val savedCall = CallLogEntry(
            id = 1L,
            number = "+15551112222",
            name = "Alice Smith",
            type = CallLog.Calls.INCOMING_TYPE,
            date = 1000L,
            duration = 60L,
            photoUri = null,
            contactId = "10"
        )
        val unsavedCallWithNumberAsName = CallLogEntry(
            id = 2L,
            number = "+15559998888",
            name = "+15559998888",
            type = CallLog.Calls.MISSED_TYPE,
            date = 2000L,
            duration = 0L,
            photoUri = null,
            contactId = null
        )
        val unknownCaller = CallLogEntry(
            id = 3L,
            number = "Unknown",
            name = "Unknown",
            type = CallLog.Calls.INCOMING_TYPE,
            date = 3000L,
            duration = 15L,
            photoUri = null,
            contactId = null
        )
        val outgoingCall = CallLogEntry(
            id = 4L,
            number = "+15553334444",
            name = null,
            type = CallLog.Calls.OUTGOING_TYPE,
            date = 4000L,
            duration = 120L,
            photoUri = null,
            contactId = null
        )

        val all = listOf(savedCall, unsavedCallWithNumberAsName, unknownCaller, outgoingCall)

        val contactsFiltered = all.filter { CallLogFilter.Contacts.matches(it) }
        assertEquals(1, contactsFiltered.size)
        assertEquals("Alice Smith", contactsFiltered[0].name)

        val missedFiltered = all.filter { CallLogFilter.Missed.matches(it) }
        assertEquals(1, missedFiltered.size)
        assertEquals(2L, missedFiltered[0].id)

        val incomingFiltered = all.filter { CallLogFilter.Incoming.matches(it) }
        assertEquals(2, incomingFiltered.size)

        val outgoingFiltered = all.filter { CallLogFilter.Outgoing.matches(it) }
        assertEquals(1, outgoingFiltered.size)
        assertEquals(4L, outgoingFiltered[0].id)
    }

    @Test
    fun testSubLogsFlatteningInTodayStatsAndAnalytics() {
        val now = System.currentTimeMillis()
        val sub1 = CallLogEntry(
            id = 101L,
            number = "01700000001",
            name = "Alice",
            type = CallLog.Calls.INCOMING_TYPE,
            date = now - 10_000L,
            duration = 45L,
            photoUri = null,
            contactId = "10"
        )
        val sub2 = CallLogEntry(
            id = 102L,
            number = "01700000001",
            name = "Alice",
            type = CallLog.Calls.OUTGOING_TYPE,
            date = now - 5_000L,
            duration = 75L,
            photoUri = null,
            contactId = "10"
        )
        val sub3Missed = CallLogEntry(
            id = 103L,
            number = "01700000001",
            name = "Alice",
            type = CallLog.Calls.MISSED_TYPE,
            date = now - 1_000L,
            duration = 0L,
            photoUri = null,
            contactId = "10"
        )
        // Parent entry only holds the most recent call's duration (0L for missed), while subLogs holds all 3
        val groupedParent = sub3Missed.copy(
            types = listOf(CallLog.Calls.MISSED_TYPE, CallLog.Calls.OUTGOING_TYPE, CallLog.Calls.INCOMING_TYPE),
            subLogs = listOf(sub3Missed, sub2, sub1)
        )

        val flattened = listOf(groupedParent).flatMap { if (it.subLogs.isNotEmpty()) it.subLogs else listOf(it) }
        val totalDuration = flattened.sumOf { it.duration }
        val incoming = flattened.count { it.type == CallLog.Calls.INCOMING_TYPE }
        val outgoing = flattened.count { it.type == CallLog.Calls.OUTGOING_TYPE }
        val missed = flattened.count { it.type == CallLog.Calls.MISSED_TYPE }
        val connectedCalls = incoming + outgoing
        val avgDuration = if (connectedCalls > 0) totalDuration / connectedCalls else 0L

        assertEquals(120L, totalDuration)
        assertEquals(1, incoming)
        assertEquals(1, outgoing)
        assertEquals(1, missed)
        assertEquals(60L, avgDuration)
    }

    @Test
    fun testPhoneNumberComparisonEdgeCases() {
        // International vs local format (last 10 digits match)
        assertTrue(areNumbersEqual("+880 1712-345678", "01712345678"))
        assertTrue(areNumbersEqual("+1 (415) 555-2671", "4155552671"))

        // Short numbers / carrier codes must require exact digit match, never suffix match
        assertFalse(areNumbersEqual("121", "01711000121"))
        assertTrue(areNumbersEqual("121", "121"))

        // Alphabetic sender IDs must not match different alphabetic IDs
        assertFalse(areNumbersEqual("VODAFONE", "TMOBILE"))
        assertTrue(areNumbersEqual("VODAFONE", "vodafone"))

        // USSD codes with * and #
        assertTrue(areNumbersEqual("*121#", "*121#"))
        assertFalse(areNumbersEqual("*121#", "*121*1#"))
        assertFalse(areNumbersEqual("*#06#", "06"))
    }

    @Test
    fun testT9Matcher() {
        // "Alice Smith" -> A(2)L(5)I(4)C(2)E(3) S(7)M(6)I(4)T(8)H(4)
        assertTrue(T9Matcher.isMatch("Alice Smith", "25423"))
        assertTrue(T9Matcher.isMatch("Alice Smith", "76484")) // Matches second word "Smith"
        assertTrue(T9Matcher.isMatch("Alice Smith", "2542376484")) // Matches across space
        assertFalse(T9Matcher.isMatch("Alice Smith", "999"))
        assertFalse(T9Matcher.isMatch("Alice Smith", ""))
    }

    @Test
    fun testQrCodeVCardOmitsUnknownPhone() {
        val vcardUnknown = QrCodeUtils.generateVCard("Mystery Caller", "Unknown", null)
        assertFalse("Unknown phone must not be added as TEL", vcardUnknown.contains("TEL;"))
        assertTrue(vcardUnknown.contains("FN:Mystery Caller"))

        val vcardValid = QrCodeUtils.generateVCard("Alice", "+15550101", "alice@example.com")
        assertTrue(vcardValid.contains("TEL;TYPE=CELL:+15550101"))
        assertTrue(vcardValid.contains("EMAIL:alice@example.com"))
    }

    @Test
    fun testSpeedDialParsingWithPipeInContactName() {
        val mapping = "Alice | Office|+15550123"
        val name = mapping.substringBeforeLast("|").takeIf { it.isNotBlank() }
        val number = mapping.substringAfterLast("|", "").takeIf { it.isNotBlank() }

        assertEquals("Alice | Office", name)
        assertEquals("+15550123", number)
    }

    @Test
    fun testCalendarDayCheckForCallLogGrouping() {
        val cal1 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 21, 23, 55, 0)
        }
        val cal2SameDay = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 21, 10, 15, 0)
        }
        val cal3DifferentDay = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 20, 23, 58, 0)
        }

        fun isSameDay(t1: Long, t2: Long): Boolean {
            val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
            val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        assertTrue(isSameDay(cal1.timeInMillis, cal2SameDay.timeInMillis))
        assertFalse(isSameDay(cal1.timeInMillis, cal3DifferentDay.timeInMillis))
    }
}
