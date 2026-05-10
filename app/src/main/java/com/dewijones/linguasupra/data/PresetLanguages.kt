package com.dewijones.linguasupra.data

/**
 * The catalogue of languages the user can pick from in Settings → Add language.
 *
 * Sourced from Duolingo's public English-from-X course list (best-effort as of 2026).
 * Each entry has:
 *  - a flag emoji (regional indicator pair, or where there's no national flag, a thematic
 *    glyph like 🏛️ for Latin or 🌍 for Esperanto)
 *  - a "vibe" emoji — a friendly mascot/motif that complements the flag in the banner
 *  - an optional motivation phrase in the target language, shown on quota-met
 *
 * Adding a new language is a one-line addition; nothing else needs to change.
 */
data class PresetLanguage(
    val name: String,
    val flagEmoji: String,
    val vibeEmoji: String,
    val motivationPhrase: String?,
    val defaultDailyQuota: Int = 1,
)

object PresetLanguages {

    val all: List<PresetLanguage> = listOf(
        // — Most-popular Duolingo courses —
        PresetLanguage("Spanish", "🇪🇸", "🌶️", "¡Vamos!"),
        PresetLanguage("French", "🇫🇷", "🥐", "Continue!"),
        PresetLanguage("German", "🇩🇪", "🥨", "Weiter so!"),
        PresetLanguage("Italian", "🇮🇹", "🍝", "Bravo!"),
        PresetLanguage("Portuguese", "🇵🇹", "🌊", "Muito bem!"),
        PresetLanguage("Brazilian Portuguese", "🇧🇷", "🌴", "Beleza!"),
        PresetLanguage("Japanese", "🇯🇵", "🌸", "頑張って!"),
        PresetLanguage("Korean", "🇰🇷", "🥢", "화이팅!"),
        PresetLanguage("Mandarin", "🇨🇳", "🐼", "加油!"),
        PresetLanguage("Cantonese", "🇭🇰", "🐲", "加油!"),

        // — European —
        PresetLanguage("Dutch", "🇳🇱", "🌷", "Goed bezig!"),
        PresetLanguage("Russian", "🇷🇺", "🪆", "Молодец!"),
        PresetLanguage("Swedish", "🇸🇪", "🦌", "Bra jobbat!"),
        PresetLanguage("Norwegian", "🇳🇴", "⛷️", "Bra!"),
        PresetLanguage("Danish", "🇩🇰", "🦢", "Godt klaret!"),
        PresetLanguage("Finnish", "🇫🇮", "🌲", "Hienoa!"),
        PresetLanguage("Polish", "🇵🇱", "🦅", "Brawo!"),
        PresetLanguage("Czech", "🇨🇿", "🍺", "Výborně!"),
        PresetLanguage("Greek", "🇬🇷", "🫒", "Μπράβο!"),
        PresetLanguage("Hungarian", "🇭🇺", "🐎", "Szuper!"),
        PresetLanguage("Romanian", "🇷🇴", "🐺", "Bravo!"),
        PresetLanguage("Ukrainian", "🇺🇦", "🌻", "Молодець!"),
        PresetLanguage("Catalan", "🇪🇸", "🍷", "Molt bé!"),
        PresetLanguage("Welsh", "🏴󠁧󠁢󠁷󠁬󠁳󠁿", "🐉", "Da iawn!", defaultDailyQuota = 5),
        PresetLanguage("Irish", "🇮🇪", "🍀", "Maith thú!"),
        PresetLanguage("Scottish Gaelic", "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "🥃", "Glè mhath!"),
        PresetLanguage("Esperanto", "🌍", "🌟", "Bone!"),
        PresetLanguage("Latin", "🏛️", "🦅", "Festina lente!"),
        PresetLanguage("Yiddish", "✡️", "📜", "Zeyer gut!"),

        // — Asian, Pacific —
        PresetLanguage("Vietnamese", "🇻🇳", "🍜", "Tốt lắm!"),
        PresetLanguage("Indonesian", "🇮🇩", "🐊", "Bagus!"),
        PresetLanguage("Hindi", "🇮🇳", "🐘", "शाबाश!"),
        PresetLanguage("Hawaiian", "🌺", "🌴", "ʻAno ʻoluʻolu!"),
        PresetLanguage("Tagalog", "🇵🇭", "🥭", "Magaling!"),

        // — Middle Eastern —
        PresetLanguage("Arabic", "🇸🇦", "🐪", "أحسنت!"),
        PresetLanguage("Hebrew", "🇮🇱", "🕎", "כל הכבוד!"),
        PresetLanguage("Turkish", "🇹🇷", "🌙", "Aferin!"),

        // — African —
        PresetLanguage("Swahili", "🇹🇿", "🦁", "Vizuri sana!"),
        PresetLanguage("Zulu", "🇿🇦", "🪘", "Halala!"),
        PresetLanguage("Xhosa", "🇿🇦", "🥁", "Phambili!"),
        PresetLanguage("Yoruba", "🇳🇬", "🦓", "Ó dáa!"),
        PresetLanguage("Haitian Creole", "🇭🇹", "🌅", "Bravo!"),

        // — Constructed / fictional —
        PresetLanguage("Klingon", "🖖", "⚔️", "Qapla'!"),
        PresetLanguage("High Valyrian", "🐲", "🔥", "Valar morghulis!"),

        // — Indigenous —
        PresetLanguage("Navajo", "🪶", "🏜️", "Ahéhee'!"),
    ).sortedBy { it.name }

    fun byName(name: String): PresetLanguage? =
        all.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

/** Convert a PresetLanguage to a domain Language ready for insertion. */
fun PresetLanguage.toLanguage(displayOrder: Int, dailyQuota: Int = defaultDailyQuota): Language =
    Language(
        name = name,
        dailyQuota = dailyQuota,
        displayOrder = displayOrder,
        flagEmoji = flagEmoji,
        vibeEmoji = vibeEmoji,
        motivationPhrase = motivationPhrase,
    )
