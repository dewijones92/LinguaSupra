package com.dewijones.linguasupra.notify

import android.graphics.Color
import com.dewijones.linguasupra.data.LanguageProgress

// Public alias so other features (Stats screen, HomeScreen) can keep
// per-language colour identity consistent without coupling to this object.
object LanguagePalette {
    fun colorForName(name: String): Int = BannerAccents.colorForName(name)
}

/**
 * Per-language accent colour for the banner row stripe. Hand-picked for the
 * defaults (Welsh red, Mandarin imperial gold, Latin imperial purple); other
 * languages fall through to a stable hash over the name so rows have
 * consistent colour identity from one render to the next.
 */
internal object BannerAccents {

    private val palette = intArrayOf(
        Color.parseColor("#FF7043"), // coral
        Color.parseColor("#26A69A"), // teal
        Color.parseColor("#5C6BC0"), // indigo
        Color.parseColor("#EC407A"), // pink
        Color.parseColor("#7E57C2"), // amethyst
        Color.parseColor("#42A5F5"), // sky
        Color.parseColor("#9CCC65"), // lime
    )

    private val pinned = mapOf(
        "welsh" to Color.parseColor("#C40233"),       // Welsh flag red
        "mandarin" to Color.parseColor("#FFB400"),    // imperial gold
        "chinese" to Color.parseColor("#FFB400"),
        "latin" to Color.parseColor("#7B3FA0"),       // imperial purple
        "french" to Color.parseColor("#1F4FB6"),      // tricolour blue
        "spanish" to Color.parseColor("#E63946"),     // bandera red
        "japanese" to Color.parseColor("#E63946"),
        "german" to Color.parseColor("#222222"),
        "italian" to Color.parseColor("#009246"),
        "korean" to Color.parseColor("#003478"),
    )

    fun colorFor(progress: LanguageProgress): Int = colorForName(progress.name)

    fun colorForName(name: String): Int {
        pinned[name.lowercase()]?.let { return it }
        val idx = (name.hashCode().rem(palette.size).let { if (it < 0) it + palette.size else it })
        return palette[idx]
    }
}
