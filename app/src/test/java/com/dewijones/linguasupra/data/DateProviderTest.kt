package com.dewijones.linguasupra.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DateProviderTest {

    @Test
    fun `todayIso returns formatted local date in configured zone`() {
        val fixed = Clock.fixed(Instant.parse("2026-05-10T14:30:00Z"), ZoneId.of("Europe/London"))
        val provider = DateProvider { fixed }
        assertEquals("2026-05-10", provider.todayIso())
    }

    @Test
    fun `todayIso crosses midnight in local zone`() {
        // 23:30 UTC on the 9th = 00:30 BST on the 10th
        val fixed = Clock.fixed(Instant.parse("2026-05-09T23:30:00Z"), ZoneId.of("Europe/London"))
        val provider = DateProvider { fixed }
        assertEquals("2026-05-10", provider.todayIso())
    }

    @Test
    fun `todayIso reflects clock advancing past midnight`() {
        val clock = MutableTestClock(Instant.parse("2026-05-10T22:00:00Z"), ZoneId.of("Europe/London"))
        val provider = DateProvider { clock }
        assertEquals("2026-05-10", provider.todayIso())
        clock.advance(java.time.Duration.ofHours(3)) // → 01:00 BST on 11th
        assertEquals("2026-05-11", provider.todayIso())
    }

    @Test
    fun `now reflects supplied clock`() {
        val instant = Instant.parse("2026-05-10T12:00:00Z")
        val provider = DateProvider { Clock.fixed(instant, ZoneId.of("UTC")) }
        assertEquals(instant, provider.now())
    }
}
