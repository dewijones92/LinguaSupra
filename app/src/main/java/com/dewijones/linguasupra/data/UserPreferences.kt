package com.dewijones.linguasupra.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    /** Absolute file path of the latest selfie shown as the home avatar. */
    val selfiePath: Flow<String?>
    suspend fun setSelfiePath(path: String?)

    /** Flips to true when the user has resolved (taken or skipped) the onboarding selfie. */
    val onboardingComplete: Flow<Boolean>
    suspend fun setOnboardingComplete()

    /** ISO date the user last saw the daily-quota celebration; used for fire-once-per-day. */
    val lastCelebrationIso: Flow<String?>
    suspend fun setLastCelebrationIso(iso: String)
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

    override val selfiePath: Flow<String?> = context.userPrefsStore.data.map { prefs ->
        prefs[SELFIE_KEY]?.takeIf { it.isNotBlank() }
    }

    override suspend fun setSelfiePath(path: String?) {
        context.userPrefsStore.edit { prefs ->
            if (path == null) prefs.remove(SELFIE_KEY) else prefs[SELFIE_KEY] = path
        }
    }

    override val onboardingComplete: Flow<Boolean> = context.userPrefsStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE_KEY] == true
    }

    override suspend fun setOnboardingComplete() {
        context.userPrefsStore.edit { it[ONBOARDING_COMPLETE_KEY] = true }
    }

    override val lastCelebrationIso: Flow<String?> = context.userPrefsStore.data.map { prefs ->
        prefs[LAST_CELEBRATION_KEY]?.takeIf { it.isNotBlank() }
    }

    override suspend fun setLastCelebrationIso(iso: String) {
        context.userPrefsStore.edit { it[LAST_CELEBRATION_KEY] = iso }
    }

    companion object {
        private val NAME_KEY = stringPreferencesKey("user_name")
        private val SELFIE_KEY = stringPreferencesKey("selfie_path")
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        private val LAST_CELEBRATION_KEY = stringPreferencesKey("last_celebration_iso")
    }
}

/** In-memory fake for tests. */
class FakeUserPreferences(
    initialName: String? = null,
    initialSelfiePath: String? = null,
    initialOnboardingComplete: Boolean = false,
    initialLastCelebrationIso: String? = null,
) : UserPreferences {
    private val _name = MutableStateFlow(initialName)
    private val _selfie = MutableStateFlow(initialSelfiePath)
    private val _onboardingComplete = MutableStateFlow(initialOnboardingComplete)
    private val _lastCelebration = MutableStateFlow(initialLastCelebrationIso)

    override val userName: Flow<String?> = _name.asStateFlow()
    override suspend fun setUserName(name: String) {
        _name.value = name.trim().takeIf { it.isNotBlank() }
    }
    override suspend fun clearUserName() { _name.value = null }

    override val selfiePath: Flow<String?> = _selfie.asStateFlow()
    override suspend fun setSelfiePath(path: String?) { _selfie.value = path }

    override val onboardingComplete: Flow<Boolean> = _onboardingComplete.asStateFlow()
    override suspend fun setOnboardingComplete() { _onboardingComplete.value = true }

    override val lastCelebrationIso: Flow<String?> = _lastCelebration.asStateFlow()
    override suspend fun setLastCelebrationIso(iso: String) { _lastCelebration.value = iso }
}
