package com.grinch.rivo4

import com.grinch.rivo4.controller.util.PreferenceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProEntitlementTest {
    @Test
    fun sixHourReminderIsDueOnlyAfterSixHours() {
        val start = 1_000L
        assertFalse(
            PreferenceManager.shouldShowVideoAdReminder(
                PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS,
                start,
                start + PreferenceManager.VIDEO_AD_REMINDER_INTERVAL_MILLIS - 1
            )
        )
        assertTrue(
            PreferenceManager.shouldShowVideoAdReminder(
                PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS,
                start,
                start + PreferenceManager.VIDEO_AD_REMINDER_INTERVAL_MILLIS
            )
        )
    }

    @Test
    fun reminderIsNotDueWhenDisabled() {
        assertFalse(
            PreferenceManager.shouldShowVideoAdReminder(
                PreferenceManager.VIDEO_AD_REMINDER_OFF,
                0L,
                Long.MAX_VALUE
            )
        )
    }
}
