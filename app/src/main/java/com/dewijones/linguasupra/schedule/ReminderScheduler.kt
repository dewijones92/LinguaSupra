package com.dewijones.linguasupra.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.dewijones.linguasupra.data.DateProvider
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Idempotent wrapper around [AlarmManager.setAndAllowWhileIdle]. Each call to
 * [scheduleAll] cancels and re-arms all three slots; calling it on every app
 * start, after boot, or after a settings change is therefore safe.
 *
 * Inexact by design — see CLAUDE.md invariant 2 for why we don't use
 * `setExactAndAllowWhileIdle`.
 */
class ReminderScheduler(
    private val context: Context,
    private val dateProvider: DateProvider = DateProvider(),
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll() {
        ReminderSlot.entries.forEach { schedule(it) }
    }

    fun cancelAll() {
        ReminderSlot.entries.forEach { alarmManager.cancel(pendingIntent(it)) }
    }

    /**
     * Schedules a single slot for its next firing. If today's slot time has
     * already passed, fires tomorrow instead.
     */
    fun schedule(slot: ReminderSlot) {
        val now = LocalDateTime.ofInstant(dateProvider.now(), dateProvider.zone())
        val targetToday = now.toLocalDate().atTime(slot.defaultTime)
        val target = if (target_isInPast(now, targetToday)) targetToday.plusDays(1) else targetToday
        val triggerMs = target.atZone(dateProvider.zone()).toInstant().toEpochMilli()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMs,
            pendingIntent(slot),
        )
    }

    @Suppress("FunctionName")
    private fun target_isInPast(now: LocalDateTime, target: LocalDateTime): Boolean =
        Duration.between(now, target).isNegative || Duration.between(now, target).isZero

    private fun pendingIntent(slot: ReminderSlot): PendingIntent {
        val intent = Intent(ReminderReceiver.ACTION_FIRE).apply {
            component = ComponentName(context, ReminderReceiver::class.java)
            putExtra(ReminderReceiver.EXTRA_SLOT, slot.key)
        }
        return PendingIntent.getBroadcast(
            context,
            slot.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Helper used by clients that have a fixed [ZoneId] (e.g. tests). The
     * primary entry point [scheduleAll] always uses the system zone via
     * [DateProvider].
     */
    @Suppress("unused")
    fun scheduleAtCalendarTime(slot: ReminderSlot, atZone: LocalDateTime, zone: ZoneId) {
        val triggerMs = atZone.atZone(zone).toInstant().toEpochMilli()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMs,
            pendingIntent(slot),
        )
    }
}
