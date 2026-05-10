package com.dewijones.linguasupra.data

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * A controllable Clock for tests. Default zone Europe/London (matches the user).
 * Use [advance] to march time forward; useful for day-rollover assertions.
 */
class MutableTestClock(
    initial: Instant,
    private val zone: ZoneId = ZoneId.of("Europe/London"),
) : Clock() {
    @Volatile
    private var current: Instant = initial

    override fun getZone(): ZoneId = zone
    override fun withZone(z: ZoneId): Clock = MutableTestClock(current, z)
    override fun instant(): Instant = current

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }

    fun setTo(instant: Instant) {
        current = instant
    }
}
