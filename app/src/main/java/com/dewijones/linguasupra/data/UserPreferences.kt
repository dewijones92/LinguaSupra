package com.dewijones.linguasupra.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

interface UserPreferences {
    val userName: Flow<String?>
    suspend fun setUserName(name: String)
    suspend fun clearUserName()
}

private val Context.userPrefsStore by preferencesDataStore(name = "user_prefs")

class DataStoreUserPreferences(private val context: Context) : UserPreferences {
    override val userName: Flow<String?> = context.userPrefsStore.data.map { prefs: Preferences ->
        prefs[NAME_KEY]?.takeIf { it.isNotBlank() }
    }

    override suspend fun setUserName(name: String) {
        context.userPrefsStore.edit { it[NAME_KEY] = name.trim() }
    }

    override suspend fun clearUserName() {
        context.userPrefsStore.edit { it.remove(NAME_KEY) }
    }

    companion object {
        private val NAME_KEY = stringPreferencesKey("user_name")
    }
}

/** In-memory fake for tests. */
class FakeUserPreferences(initial: String? = null) : UserPreferences {
    private val _name = MutableStateFlow(initial)
    override val userName: Flow<String?> = _name.asStateFlow()
    override suspend fun setUserName(name: String) {
        _name.value = name.trim().takeIf { it.isNotBlank() }
    }
    override suspend fun clearUserName() {
        _name.value = null
    }
}
