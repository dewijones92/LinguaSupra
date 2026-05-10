package com.dewijones.linguasupra.data

import android.content.Context

/**
 * Manual DI surface. Wires the Application to a single AppDatabase + Repository instance.
 * Tests substitute via [overrideForTest].
 */
class AppContainer private constructor(
    val database: AppDatabase,
    val dateProvider: DateProvider,
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
            instance ?: AppContainer(
                database = AppDatabase.build(context.applicationContext),
                dateProvider = DateProvider(),
            ).also { instance = it }
        }

        fun overrideForTest(database: AppDatabase, dateProvider: DateProvider) {
            instance = AppContainer(database, dateProvider)
        }

        fun reset() {
            instance = null
        }
    }
}
