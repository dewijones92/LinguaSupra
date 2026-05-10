package com.dewijones.linguasupra.ui.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.CompletionWithLanguage
import com.dewijones.linguasupra.data.DailySeriesPoint
import com.dewijones.linguasupra.notify.BannerNotificationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class StatsViewModel(private val context: Context) : ViewModel() {

    private val container = AppContainer.get(context)
    private val repository = container.repository

    val series14: StateFlow<List<DailySeriesPoint>> =
        repository.observeDailySeries(days = 14).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val allCompletions: StateFlow<List<CompletionWithLanguage>> =
        repository.observeAllCompletionsWithLanguage().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun deleteCompletion(id: Long) {
        viewModelScope.launch {
            repository.deleteCompletion(id)
            BannerNotificationManager(context, repository).refresh()
        }
    }

    fun updateCompletionTime(id: Long, instant: Instant) {
        viewModelScope.launch {
            repository.updateCompletionTime(id, instant)
            BannerNotificationManager(context, repository).refresh()
        }
    }

    fun addManualCompletion(languageId: Long, instant: Instant) {
        viewModelScope.launch {
            repository.addManualCompletion(languageId, instant)
            BannerNotificationManager(context, repository).refresh()
        }
    }
}

internal fun statsViewModelFactory(context: Context): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(context.applicationContext) as T
    }
