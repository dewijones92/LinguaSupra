package com.dewijones.linguasupra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetLanguagesTest {

    @Test
    fun catalogue_is_non_trivial_and_alphabetical() {
        val names = PresetLanguages.all.map { it.name }
        assertTrue("expect a catalogue of at least 30 languages", names.size >= 30)
        assertEquals("entries are sorted by name", names.sorted(), names)
    }

    @Test
    fun every_entry_has_a_flag_and_vibe_glyph() {
        PresetLanguages.all.forEach { p ->
            assertTrue("flag for ${p.name}", p.flagEmoji.isNotBlank())
            assertTrue("vibe for ${p.name}", p.vibeEmoji.isNotBlank())
        }
    }

    @Test
    fun the_three_seeded_languages_are_present() {
        assertNotNull(PresetLanguages.byName("Welsh"))
        assertNotNull(PresetLanguages.byName("Mandarin"))
        assertNotNull(PresetLanguages.byName("Latin"))
    }

    @Test
    fun welsh_default_quota_is_five() {
        assertEquals(5, PresetLanguages.byName("Welsh")!!.defaultDailyQuota)
    }

    @Test
    fun byName_is_case_insensitive() {
        assertEquals("Spanish", PresetLanguages.byName("spanish")!!.name)
        assertEquals("Spanish", PresetLanguages.byName("SPANISH")!!.name)
        assertNull(PresetLanguages.byName("does not exist"))
    }

    @Test
    fun toLanguage_carries_all_fields() {
        val french = PresetLanguages.byName("French")!!
        val asEntity = french.toLanguage(displayOrder = 7, dailyQuota = 3)
        assertEquals("French", asEntity.name)
        assertEquals(3, asEntity.dailyQuota)
        assertEquals(7, asEntity.displayOrder)
        assertEquals("🇫🇷", asEntity.flagEmoji)
        assertEquals("🥐", asEntity.vibeEmoji)
        assertEquals("Continue!", asEntity.motivationPhrase)
    }
}
