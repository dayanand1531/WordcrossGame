package com.dayanand.wordscapes.presentation.levelselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayanand.wordscapes.data.repository.LevelRepository
import com.dayanand.wordscapes.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class LevelSelectUiState(
    val totalLevels: Int = 15,
    val unlockedLevel: Int = 1,
    val completedLevels: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

class LevelSelectViewModel(
    private val levelRepository: LevelRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val total = levelRepository.getTotalLevels()
            combine(
                userPreferencesRepository.unlockedLevelFlow,
                userPreferencesRepository.completedLevelsFlow
            ) { unlocked, completed ->
                LevelSelectUiState(
                    totalLevels = total,
                    unlockedLevel = unlocked,
                    completedLevels = completed,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
