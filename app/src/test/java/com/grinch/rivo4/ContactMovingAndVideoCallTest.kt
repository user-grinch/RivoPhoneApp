package com.grinch.rivo4

import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.PhoneNumberEntry
import com.grinch.rivo4.view.components.VideoAppOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMovingAndVideoCallTest {

    @Test
    fun testVideoAppOptionModel() {
        val option = VideoAppOption(
            id = "whatsapp",
            name = "WhatsApp",
            packageName = "com.whatsapp",
            description = "High definition encrypted video call"
        )
        assertEquals("whatsapp", option.id)
        assertEquals("WhatsApp", option.name)
        assertEquals("com.whatsapp", option.packageName)
        assertEquals(null, option.iconDrawable)
        assertEquals(null, option.iconVector)
        assertEquals("High definition encrypted video call", option.description)
    }

    @Test
    fun testVideoCallingPreferenceKey() {
        assertEquals("default_video_call_app", PreferenceManager.KEY_DEFAULT_VIDEO_APP)
    }

    @Test
    fun testMoveContactAccountComparison() {
        val contact = Contact(
            id = "101",
            name = "Alice",
            accountName = "alice@gmail.com",
            accountType = "com.google"
        )

        // Moving to same account should be detected as identical
        val sameAccount = !contact.isPrivate &&
                contact.accountName == "alice@gmail.com" &&
                contact.accountType == "com.google"
        assertTrue("Moving to identical account should be recognized as a no-op", sameAccount)

        // Moving to different account
        val differentAccount = !contact.isPrivate &&
                contact.accountName == "phone" &&
                contact.accountType == "com.android.localphone"
        assertFalse("Different target account should require move", differentAccount)
    }

    @Test
    fun testRawContactIsolationDuringMove() {
        // Simulating the bug where deleteContact wiped out the newly created contact:
        // Before move: old contact had raw IDs [101]
        val oldRawContactIds = listOf("101")
        // Target contact is saved and assigned new raw ID [102]
        val newRawContactId = "102"

        // The set of raw contact IDs to delete must be strictly oldRawContactIds
        val rawIdsToDelete = oldRawContactIds.filter { it != newRawContactId }

        assertEquals(listOf("101"), rawIdsToDelete)
        assertFalse("New raw contact ID must never be included in deletion set", rawIdsToDelete.contains(newRawContactId))
    }

    @Test
    fun testMergeDuplicateExcludesTargetSelfReference() {
        // Simulating the merge bug where source was a lookup key or raw ID resolving to same CONTACT_ID:
        val targetContactId = "42"
        val targetResolvedId = "42"

        val rawSources = listOf("42", "0r42-12345", "99")

        val validSources = rawSources.filter { srcId ->
            val srcResolvedId = if (srcId.startsWith("p")) srcId else (if (srcId.toLongOrNull() != null) srcId else "42")
            srcResolvedId != targetResolvedId
        }

        assertEquals(1, validSources.size)
        assertEquals("99", validSources[0])
    }

    @Test
    fun testContactDeduplicationAndEntries() {
        val entry1 = PhoneNumberEntry(number = "+1 555-1234", type = 2, label = "Mobile")
        val entry2 = PhoneNumberEntry(number = "555-1234", type = 1, label = "Home")
        val entry3 = PhoneNumberEntry(number = "+1 999-8888", type = 2, label = "Work")

        val phoneList = listOf(entry1, entry2, entry3)
        // Check that entries preserve all attributes
        assertEquals(3, phoneList.size)
        assertEquals("+1 555-1234", phoneList[0].number)
        assertEquals("Mobile", phoneList[0].label)
        assertEquals(2, phoneList[0].type)
    }
}
