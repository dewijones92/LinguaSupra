package com.dewijones.linguasupra.schedule

import java.time.LocalTime

/**
 * The three reminder windows. Times are inexact (Doze maintenance window
 * may slip them up to ~10 min) — see CLAUDE.md invariant 2.
 *
 * Notification IDs and PendingIntent request codes are stable per slot so
 * reminders coexist on the shade and re-scheduling a slot updates the same
 * alarm record rather than creating a new one.
 */
enum class ReminderSlot(
    val key: String,
    val defaultTime: LocalTime,
    val notificationId: Int,
    val requestCode: Int,
) {
    MORNING("morning", LocalTime.of(9, 0), notificationId = 2001, requestCode = 11),
    AFTERNOON("afternoon", LocalTime.of(14, 0), notificationId = 2002, requestCode = 12),
    EVENING("evening", LocalTime.of(19, 0), notificationId = 2003, requestCode = 13);

    companion object {
        fun fromKey(key: String?): ReminderSlot? = entries.firstOrNull { it.key == key }
    }
}
