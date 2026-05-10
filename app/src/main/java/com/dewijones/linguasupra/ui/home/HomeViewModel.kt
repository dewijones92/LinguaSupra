package com.dewijones.linguasupra.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dewijones.linguasupra.data.AppContainer
import com.dewijones.linguasupra.data.LanguageProgress
import com.dewijones.linguasupra.notify.BannerNotificationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OnboardingState {
    data object Hidden : OnboardingState
    data object AskingName : OnboardingState
    data class AskingSelfie(val name: String) : OnboardingState
}

class HomeViewModel(private val context: Context) : ViewModel() {

    private val container = AppContainer.get(context)
    private val prefs = container.userPreferences
    private val dateProvider = container.dateProvider

    val state: StateFlow<List<LanguageProgress>> =
        container.repository.observeTodayProgress().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val streak: StateFlow<Int> = container.repository.observeStreak().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
    )

    val userName: StateFlow<String?> = prefs.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val selfiePath: StateFlow<String?> = prefs.selfiePath.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val onboardingState: StateFlow<OnboardingState> = combine(
        prefs.userName,
        prefs.onboardingComplete,
    ) { name, complete ->
        when {
            name.isNullOrBlank() -> OnboardingState.AskingName
            !complete -> OnboardingState.AskingSelfie(name)
            else -> OnboardingState.Hidden
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingState.Hidden,
    )

    /** True when every active language is at quota AND we haven't celebrated yet today. */
    val showCelebration: StateFlow<Boolean> = combine(
        state,
        prefs.lastCelebrationIso,
    ) { progress, lastIso ->
        val allComplete = progress.isNotEmpty() && progress.all { it.isComplete }
        allComplete && lastIso != dateProvider.todayIso()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    fun recordCompletion(languageId: Long) {
        viewModelScope.launch {
            container.repository.recordCompletion(languageId)
            BannerNotificationManager(context, container.repository).refresh()
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { prefs.setUserName(name) }
    }

    fun setSelfiePath(path: String?) {
        viewModelScope.launch { prefs.setSelfiePath(path) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingComplete() }
    }

    fun dismissCelebration() {
        viewModelScope.launch { prefs.setLastCelebrationIso(dateProvider.todayIso()) }
    }
}
