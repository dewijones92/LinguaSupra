package com.dewijones.linguasupra.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.Language
import com.dewijones.linguasupra.data.PresetLanguage
import com.dewijones.linguasupra.data.PresetLanguages
import com.dewijones.linguasupra.notify.BannerNotificationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val container = AppContainer.get(context)

    val languages: StateFlow<List<Language>> =
        container.database.languageDao().observeActive().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val presets: List<PresetLanguage> = PresetLanguages.all

    fun add(preset: PresetLanguage, dailyQuota: Int = preset.defaultDailyQuota) {
        viewModelScope.launch {
            container.repository.addLanguage(
                name = preset.name,
                dailyQuota = dailyQuota,
                flagEmoji = preset.flagEmoji,
                vibeEmoji = preset.vibeEmoji,
                motivationPhrase = preset.motivationPhrase,
            )
            BannerNotificationManager(context, container.repository).refresh()
        }
    }

    fun updateQuota(language: Language, newQuota: Int) {
        viewModelScope.launch {
            container.repository.updateLanguage(language.copy(dailyQuota = newQuota.coerceAtLeast(0)))
            BannerNotificationManager(context, container.repository).refresh()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            container.repository.deleteLanguage(id)
            BannerNotificationManager(context, container.repository).refresh()
        }
    }

    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch {
            container.repository.reorderLanguages(orderedIds)
            BannerNotificationManager(context, container.repository).refresh()
        }
    }
}
