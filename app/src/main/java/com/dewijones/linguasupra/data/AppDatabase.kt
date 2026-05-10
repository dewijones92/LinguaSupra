package com.dewijones.linguasupra.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Language::class, Completion::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun languageDao(): LanguageDao
    abstract fun completionDao(): CompletionDao

    companion object {
        const val NAME = "lingua.db"

        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            NAME,
        ).addCallback(SeedCallback).build()
    }
}

internal object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultLanguages.forEachIndexed { index, lang ->
            db.execSQL(
                """
                INSERT INTO languages
                    (name, daily_quota, display_order, active, flag_emoji, vibe_emoji, motivation_phrase)
                VALUES (?, ?, ?, 1, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    lang.name,
                    lang.dailyQuota,
                    index,
                    lang.flagEmoji,
                    lang.vibeEmoji,
                    lang.motivationPhrase,
                ),
            )
        }
    }
}

val DefaultLanguages: List<Language> = listOf(
    Language(
        name = "Mandarin",
        dailyQuota = 1,
        displayOrder = 0,
        flagEmoji = "🇨🇳",
        vibeEmoji = "🐼",
        motivationPhrase = "加油!",
    ),
    Language(
        name = "Latin",
        dailyQuota = 1,
        displayOrder = 1,
        flagEmoji = "🏛️",
        vibeEmoji = "🦅",
        motivationPhrase = "Festina lente!",
    ),
    Language(
        name = "Welsh",
        dailyQuota = 5,
        displayOrder = 2,
        flagEmoji = "🏴󠁧󠁢󠁷󠁬󠁳󠁿",
        vibeEmoji = "🐉",
        motivationPhrase = "Da iawn!",
    ),
)
