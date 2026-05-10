package com.dewijones.linguasupra.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.LanguageProgress
import com.dewijones.linguasupra.notify.BannerNotificationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val context: Context) : ViewModel() {

    private val container = AppContainer.get(context)

    val state: StateFlow<List<LanguageProgress>> =
        container.repository.observeTodayProgress().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val userName: StateFlow<String?> = container.userPreferences.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun recordCompletion(languageId: Long) {
        viewModelScope.launch {
            container.repository.recordCompletion(languageId)
            BannerNotificationManager(context, container.repository).refresh()
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            container.userPreferences.setUserName(name)
        }
    }
}
