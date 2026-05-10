package com.dewijones.linguasupra.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DateProvider(
    private val clockSupplier: () -> Clock = { Clock.systemDefaultZone() },
) {
    fun now(): Instant = Instant.now(clockSupplier())
    fun today(): LocalDate = LocalDate.now(clockSupplier())
    fun todayIso(): String = today().toString()
    fun zone(): ZoneId = clockSupplier().zone
}
