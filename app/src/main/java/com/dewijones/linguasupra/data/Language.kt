package com.dewijones.linguasupra.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "languages")
data class Language(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "daily_quota") val dailyQuota: Int,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    val active: Boolean = true,
    @ColumnInfo(name = "flag_emoji") val flagEmoji: String,
    @ColumnInfo(name = "vibe_emoji") val vibeEmoji: String,
    @ColumnInfo(name = "motivation_phrase") val motivationPhrase: String? = null,
)
