package com.dewijones.linguasupra.data

data class LanguageProgress(
    val languageId: Long,
    val name: String,
    val flagEmoji: String,
    val vibeEmoji: String,
    val motivationPhrase: String?,
    val dailyQuota: Int,
    val completedToday: Int,
    val displayOrder: Int,
) {
    val isComplete: Boolean get() = completedToday >= dailyQuota
    val outstanding: Int get() = (dailyQuota - completedToday).coerceAtLeast(0)
    val progressFraction: Float
        get() = if (dailyQuota <= 0) 0f else completedToday.toFloat() / dailyQuota.toFloat()
}
