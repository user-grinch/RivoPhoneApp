package com.grinch.rivo4

import android.provider.ContactsContract
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.ContactEvent
import com.grinch.rivo4.modal.data.OrganizationEntry
import com.grinch.rivo4.modal.data.PostalAddressEntry
import com.grinch.rivo4.modal.data.RelationEntry
import com.grinch.rivo4.modal.data.WebsiteEntry
import com.grinch.rivo4.modal.db.PrivateContactEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the RFC 6350 (vCard 4.0) contact fields:
 * events (BDAY/ANNIVERSARY §6.1.4/§6.1.5), organization (ORG/TITLE §6.6.4/§6.6.1),
 * websites (URL §6.7.3), relations (RELATED §6.6.6) and typed addresses (ADR §6.3.1).
 */
class ContactRfcFieldsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    private fun fullContact(): Contact = Contact(
        id = "77",
        name = "Jane Doe",
        givenName = "Jane",
        familyName = "Doe",
        phoneNumbers = listOf("+15550001111"),
        emails = listOf("jane@example.com"),
        addresses = listOf("123 Elm St"),
        addressEntries = listOf(
            PostalAddressEntry(
                address = "123 Elm St",
                type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
            )
        ),
        events = listOf(
            ContactEvent(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY, null, "1990-04-01"),
            ContactEvent(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY, null, "2015-06-15")
        ),
        organizations = listOf(
            OrganizationEntry(
                company = "Acme Corp",
                title = "CTO",
                department = "Engineering"
            )
        ),
        websites = listOf(
            WebsiteEntry("https://janedoe.dev", ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE)
        ),
        relations = listOf(
            RelationEntry("Bob", ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER)
        ),
        notes = "note"
    )

    @Test
    fun contactSerializationRoundTripsAllRfcFields() {
        val encoded = json.encodeToString(fullContact())
        val decoded = json.decodeFromString<Contact>(encoded)

        assertEquals(2, decoded.events.size)
        assertEquals("1990-04-01", decoded.events[0].date)
        assertEquals(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY, decoded.events[1].type)

        assertEquals(1, decoded.organizations.size)
        assertEquals("Acme Corp", decoded.organizations[0].company)
        assertEquals("CTO", decoded.organizations[0].title)
        assertEquals("Engineering", decoded.organizations[0].department)

        assertEquals(1, decoded.websites.size)
        assertEquals("https://janedoe.dev", decoded.websites[0].url)

        assertEquals(1, decoded.relations.size)
        assertEquals("Bob", decoded.relations[0].name)
        assertEquals(ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER, decoded.relations[0].type)

        assertEquals(1, decoded.addressEntries.size)
        assertEquals(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK, decoded.addressEntries[0].type)
    }

    @Test
    fun legacyJsonWithoutNewFieldsStillDecodes() {
        // Pre-feature backups/trash snapshots must keep decoding (forward compatibility)
        val legacyJson = """
            {
                "id": "5",
                "name": "Old Contact",
                "phoneNumbers": ["+15550002222"]
            }
        """.trimIndent()
        val decoded = json.decodeFromString<Contact>(legacyJson)
        assertEquals("Old Contact", decoded.name)
        assertTrue(decoded.events.isEmpty())
        assertTrue(decoded.organizations.isEmpty())
        assertTrue(decoded.websites.isEmpty())
        assertTrue(decoded.relations.isEmpty())
        assertTrue(decoded.addressEntries.isEmpty())
    }

    @Test
    fun futureJsonWithUnknownFieldsStillDecodes() {
        val futureJson = """
            {
                "id": "6",
                "name": "Future Contact",
                "phoneNumbers": [],
                "someFutureField": true,
                "organizations": [{"company": "X", "title": "Y", "department": "Z"}]
            }
        """.trimIndent()
        val decoded = json.decodeFromString<Contact>(futureJson)
        assertEquals("Future Contact", decoded.name)
        assertEquals(1, decoded.organizations.size)
        assertEquals("X", decoded.organizations[0].company)
    }

    @Test
    fun privateContactEntityRoundTripsRfcFields() {
        val contact = fullContact().copy(id = "p7", isPrivate = true)
        val entity = PrivateContactEntity.fromContact(contact)

        assertEquals("Acme Corp", entity.organizationsJson?.let { json.decodeFromString<List<OrganizationEntry>>(it) }?.get(0)?.company)
        assertEquals("https://janedoe.dev", entity.websitesJson?.let { json.decodeFromString<List<WebsiteEntry>>(it) }?.get(0)?.url)
        assertEquals("Bob", entity.relationsJson?.let { json.decodeFromString<List<RelationEntry>>(it) }?.get(0)?.name)

        val restored = entity.toContact()
        assertEquals("p7", restored.id)
        assertEquals(2, restored.events.size)
        assertEquals(1, restored.organizations.size)
        assertEquals("CTO", restored.organizations[0].title)
        assertEquals(1, restored.websites.size)
        assertEquals(1, restored.relations.size)
        assertEquals(1, restored.addressEntries.size)
    }

    @Test
    fun privateContactEntityWithoutNewColumnsDecodesToEmptyLists() {
        // Simulates rows created before the v6 migration (null JSON columns)
        val legacyEntity = PrivateContactEntity(
            localId = 3L,
            name = "Legacy",
            phoneNumbersJson = "[]",
            emailsJson = "[]",
            addressesJson = "[]",
            eventsJson = "[]",
            organizationsJson = null,
            websitesJson = null,
            relationsJson = null
        )
        val restored = legacyEntity.toContact()
        assertTrue(restored.organizations.isEmpty())
        assertTrue(restored.websites.isEmpty())
        assertTrue(restored.relations.isEmpty())
    }

    @Test
    fun addressEntriesStayInSyncWithPlainAddressList() {
        val contact = fullContact()
        // Plain list and typed list must describe the same addresses
        assertEquals(contact.addresses.size, contact.addressEntries.size)
        assertTrue(contact.addressEntries.all { entry -> contact.addresses.contains(entry.address) })
    }
}
