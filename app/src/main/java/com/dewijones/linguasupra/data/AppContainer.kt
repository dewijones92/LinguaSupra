package com.dewijones.linguasupra.data

import android.content.Context

/**
 * Manual DI surface. Wires the Application to a single AppDatabase + Repository instance.
 * Tests substitute via [overrideForTest].
 */
class AppContainer private constructor(
    val database: AppDatabase,
    val dateProvider: DateProvider,
    val userPreferences: UserPreferences,
) {
    val repository: Repository = Repository(
        languageDao = database.languageDao(),
        completionDao = database.completionDao(),
        dateProvider = dateProvider,
    )

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer = instance ?: synchronized(this) {
            val app = context.applicationContext
            instance ?: AppContainer(
                database = AppDatabase.build(app),
                dateProvider = DateProvider(),
                userPreferences = DataStoreUserPreferences(app),
            ).also { instance = it }
        }

        fun overrideForTest(
            database: AppDatabase,
            dateProvider: DateProvider,
            userPreferences: UserPreferences = FakeUserPreferences(),
        ) {
            instance = AppContainer(database, dateProvider, userPreferences)
        }

        fun reset() {
            instance = null
        }

        /**
         * Close the live Room database and null the singleton so the next
         * [get] rebuilds. Used by [DatabaseImporter] before swapping the
         * on-disk lingua.db file underneath us. The [context] parameter is
         * accepted for symmetry but not used — kept so callers don't have
         * to special-case this versus [get].
         */
        @Suppress("UNUSED_PARAMETER")
        fun closeAndReset(context: Context) {
            synchronized(this) {
                instance?.database?.close()
                instance = null
            }
        }
    }
}
