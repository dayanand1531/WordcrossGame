package com.dayanand.wordscapes.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayanand.wordscapes.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val unlockedLevel: Int = 1,
    val totalScore: Int = 0
)

class HomeViewModel(
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        userPreferencesRepository.unlockedLevelFlow,
        userPreferencesRepository.totalScoreFlow
    ) { unlocked, score ->
        HomeUiState(
            unlockedLevel = unlocked,
            totalScore = score
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
