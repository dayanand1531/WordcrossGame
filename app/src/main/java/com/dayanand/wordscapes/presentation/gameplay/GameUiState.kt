package com.dayanand.wordscapes.presentation.gameplay

import com.dayanand.wordscapes.data.model.GridCell
import com.dayanand.wordscapes.data.model.Level

data class GameUiState(
    val levelId: Int = 1,
    val totalLevels: Int = 15,
    val grid: List<GridCell> = emptyList(),
    val letters: List<Char> = emptyList(),
    val selectedIndices: List<Int> = emptyList(),
    val currentWord: String = "",
    val foundWords: Set<String> = emptySet(),
    val bonusWords: Set<String> = emptySet(),
    val score: Int = 0,
    val hintsRemaining: Int = 3,
    val showOneRemaining: Int = DEFAULT_SHOW_ONE_LIMIT,
    val isGestureActive: Boolean = false,
    val isCompleted: Boolean = false,
    val isLevelCompleting: Boolean = false,
    val isPaused: Boolean = false,
    val showInvalidFeedback: Boolean = false,
    val bonusWordBanner: String? = null,
    val hintFeedbackMessage: String? = null,
    val isLoading: Boolean = true,
    val levelObj: Level? = null
) {
    companion object {
        const val DEFAULT_SHOW_ONE_LIMIT = 3
    }
}
