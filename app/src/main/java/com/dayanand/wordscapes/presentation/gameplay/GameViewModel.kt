package com.dayanand.wordscapes.presentation.gameplay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayanand.wordscapes.data.model.Direction
import com.dayanand.wordscapes.data.model.GridCell
import com.dayanand.wordscapes.data.model.Level
import com.dayanand.wordscapes.data.repository.LevelRepository
import com.dayanand.wordscapes.data.repository.UserPreferencesRepository
import com.dayanand.wordscapes.domain.usecase.CalculateScoreUseCase
import com.dayanand.wordscapes.domain.usecase.GameEngine
import com.dayanand.wordscapes.domain.usecase.UseHintResult
import com.dayanand.wordscapes.domain.usecase.ValidateWordUseCase
import com.dayanand.wordscapes.domain.usecase.WordValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class GameNavigationEvent {
    data class AutoNavigateToNextLevel(val nextLevelId: Int) : GameNavigationEvent()
}

class GameViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val levelRepository: LevelRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val validateWordUseCase: ValidateWordUseCase = ValidateWordUseCase(),
    private val calculateScoreUseCase: CalculateScoreUseCase = CalculateScoreUseCase(),
    private val gameEngine: GameEngine = GameEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<GameNavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<GameNavigationEvent> = _navigationEvent.receiveAsFlow()

    private var bannerJob: Job? = null
    private var feedbackJob: Job? = null
    private var hintMessageJob: Job? = null

    // SavedStateHandle keys for persistence across process recreation
    private companion object {
        const val KEY_LEVEL_ID = "saved_level_id"
        const val KEY_FOUND_WORDS = "saved_found_words"
        const val KEY_BONUS_WORDS = "saved_bonus_words"
        const val KEY_SCORE = "saved_score"
        const val KEY_HINTS_REMAINING = "saved_hints_remaining"
        const val KEY_SHOW_ONE_REMAINING = "saved_show_one_remaining"
        const val KEY_LETTER_ORDER = "saved_letter_order"
    }

    fun initLevel(levelId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val total = levelRepository.getTotalLevels()
            val level = levelRepository.getLevel(levelId) ?: levelRepository.getLevel(1)
            val persistedTotalScore = userPreferencesRepository.totalScoreFlow.firstOrNull() ?: 0

            if (level != null) {
                // Restore saved state if exists for this level
                val savedLevelId: Int? = savedStateHandle[KEY_LEVEL_ID]
                val foundWords: Set<String> = if (savedLevelId == level.id) {
                    savedStateHandle.get<List<String>>(KEY_FOUND_WORDS)?.toSet() ?: emptySet()
                } else {
                    emptySet()
                }

                val bonusWords: Set<String> = if (savedLevelId == level.id) {
                    savedStateHandle.get<List<String>>(KEY_BONUS_WORDS)?.toSet() ?: emptySet()
                } else {
                    emptySet()
                }

                val score: Int = if (savedLevelId == level.id && savedStateHandle.contains(KEY_SCORE)) {
                    savedStateHandle[KEY_SCORE] ?: persistedTotalScore
                } else {
                    persistedTotalScore
                }

                val hints: Int = savedStateHandle[KEY_HINTS_REMAINING] ?: 3
                val showOneRem: Int = savedStateHandle[KEY_SHOW_ONE_REMAINING] ?: GameUiState.DEFAULT_SHOW_ONE_LIMIT

                val savedLetterOrderStr = if (savedLevelId == level.id) {
                    savedStateHandle.get<List<String>>(KEY_LETTER_ORDER)
                } else null

                val charLetters = if (savedLetterOrderStr != null && savedLetterOrderStr.isNotEmpty()) {
                    savedLetterOrderStr.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                } else {
                    level.letters.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                }

                // Save to SavedStateHandle
                savedStateHandle[KEY_LEVEL_ID] = level.id
                savedStateHandle[KEY_FOUND_WORDS] = foundWords.toList()
                savedStateHandle[KEY_BONUS_WORDS] = bonusWords.toList()
                savedStateHandle[KEY_SCORE] = score
                savedStateHandle[KEY_HINTS_REMAINING] = hints
                savedStateHandle[KEY_SHOW_ONE_REMAINING] = showOneRem
                savedStateHandle[KEY_LETTER_ORDER] = charLetters.map { it.toString() }

                val initialGrid = gameEngine.generateGridCells(level, foundWords)
                val isComp = gameEngine.isLevelComplete(level, foundWords)

                _uiState.update {
                    it.copy(
                        levelId = level.id,
                        totalLevels = total,
                        grid = initialGrid,
                        letters = charLetters,
                        foundWords = foundWords,
                        bonusWords = bonusWords,
                        score = score,
                        hintsRemaining = hints,
                        showOneRemaining = showOneRem,
                        isCompleted = isComp,
                        isLevelCompleting = false,
                        isLoading = false,
                        levelObj = level
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun showOne() {
        val currentState = _uiState.value
        if (currentState.isGestureActive) {
            showHintFeedback("Finish active gesture first!")
            return
        }

        if (currentState.showOneRemaining <= 0) {
            showHintFeedback("No Show One remaining!")
            return
        }

        if (currentState.letters.size < 2) return

        val swapped = gameEngine.swapTwoLetters(currentState.letters)
        val newRemaining = (currentState.showOneRemaining - 1).coerceAtLeast(0)

        savedStateHandle[KEY_SHOW_ONE_REMAINING] = newRemaining
        savedStateHandle[KEY_LETTER_ORDER] = swapped.map { it.toString() }

        _uiState.update {
            it.copy(
                letters = swapped,
                showOneRemaining = newRemaining
            )
        }
    }

    fun setGestureActive(active: Boolean) {
        _uiState.update { it.copy(isGestureActive = active) }
    }

    fun useHint() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val level = currentState.levelObj
                ?: levelRepository.getLevel(currentState.levelId)
                ?: return@launch

            if (currentState.score < 100) {
                showHintFeedback("Need 100 points for a hint! (Score: ${currentState.score})")
                return@launch
            }

            if (currentState.hintsRemaining <= 0) {
                showHintFeedback("No hints remaining")
                return@launch
            }

            val result = gameEngine.applyHint(
                level = level,
                currentGrid = currentState.grid,
                foundWords = currentState.foundWords
            )

            when (result) {
                is UseHintResult.Success -> {
                    val newHints = (currentState.hintsRemaining - 1).coerceAtLeast(0)
                    val newScore = (currentState.score - 100).coerceAtLeast(0)

                    savedStateHandle[KEY_HINTS_REMAINING] = newHints
                    savedStateHandle[KEY_SCORE] = newScore
                    userPreferencesRepository.updateScore(newScore)

                    val (updatedFound, isComplete) = checkAndCompleteRevealedWords(level, result.updatedGrid, currentState.foundWords)
                    if (updatedFound != currentState.foundWords) {
                        savedStateHandle[KEY_FOUND_WORDS] = updatedFound.toList()
                    }

                    _uiState.update {
                        it.copy(
                            grid = result.updatedGrid,
                            hintsRemaining = newHints,
                            score = newScore,
                            foundWords = updatedFound,
                            levelObj = level
                        )
                    }

                    if (isComplete) {
                        handleLevelCompletion(level, newScore + calculateScoreUseCase.getLevelCompletionBonus())
                    }
                }
                is UseHintResult.NoUnrevealedCells -> {
                    showHintFeedback("No unrevealed letters remaining")
                }
            }
        }
    }

    private fun handleLevelCompletion(level: Level, finalScore: Int) {
        val currentState = _uiState.value
        if (currentState.isLevelCompleting) return

        savedStateHandle[KEY_SCORE] = finalScore

        _uiState.update {
            it.copy(
                score = finalScore,
                isCompleted = true,
                isLevelCompleting = true
            )
        }

        viewModelScope.launch {
            userPreferencesRepository.completeLevel(
                levelId = level.id,
                nextLevelId = level.id + 1,
                scoreGained = finalScore
            )

            // Brief delay (750 ms) to show level completion feedback
            delay(750)

            if (level.id < currentState.totalLevels) {
                _navigationEvent.send(GameNavigationEvent.AutoNavigateToNextLevel(level.id + 1))
            } else {
                _uiState.update { it.copy(isLevelCompleting = false) }
            }
        }
    }

    private fun showHintFeedback(msg: String) {
        hintMessageJob?.cancel()
        _uiState.update { it.copy(hintFeedbackMessage = msg) }
        hintMessageJob = viewModelScope.launch {
            delay(1800)
            _uiState.update { it.copy(hintFeedbackMessage = null) }
        }
    }

    private fun checkAndCompleteRevealedWords(
        level: Level,
        grid: List<GridCell>,
        currentFound: Set<String>
    ): Pair<Set<String>, Boolean> {
        val gridMap = grid.associateBy { Pair(it.row, it.col) }
        val newFound = currentFound.toMutableSet()

        for (wordObj in level.words) {
            val upperWord = wordObj.word.uppercase()
            if (!newFound.contains(upperWord)) {
                var allRevealed = true
                for (i in upperWord.indices) {
                    val r = if (wordObj.direction == Direction.VERTICAL) wordObj.row + i else wordObj.row
                    val c = if (wordObj.direction == Direction.HORIZONTAL) wordObj.column + i else wordObj.column
                    val cell = gridMap[Pair(r, c)]
                    if (cell == null || !cell.isRevealed) {
                        allRevealed = false
                        break
                    }
                }
                if (allRevealed) {
                    newFound.add(upperWord)
                }
            }
        }

        val isComplete = gameEngine.isLevelComplete(level, newFound)
        return Pair(newFound, isComplete)
    }

    fun onLetterSelected(index: Int) {
        _uiState.update { state ->
            if (index in state.letters.indices && !state.selectedIndices.contains(index)) {
                val newIndices = state.selectedIndices + index
                val currentW = newIndices.map { state.letters[it] }.joinToString("")
                state.copy(
                    selectedIndices = newIndices,
                    currentWord = currentW
                )
            } else {
                state
            }
        }
    }

    fun onSelectionComplete(indices: List<Int>) {
        val currentState = _uiState.value
        val level = currentState.levelObj ?: return
        if (currentState.isLevelCompleting) return
        if (indices.isEmpty()) {
            onSelectionCleared()
            return
        }

        val candidate = indices.mapNotNull { currentState.letters.getOrNull(it) }.joinToString("")
        val result = validateWordUseCase.validate(
            candidate = candidate,
            crosswordWords = level.words,
            bonusWords = level.bonusWords,
            foundWords = currentState.foundWords,
            foundBonusWords = currentState.bonusWords
        )

        when (result) {
            is WordValidationResult.ValidCrossword -> {
                val upperWord = result.wordObj.word.uppercase()
                val updatedFound = currentState.foundWords + upperWord
                val points = calculateScoreUseCase.calculateNormalWordScore(upperWord.length)
                val newScore = currentState.score + points

                val updatedGrid = gameEngine.revealWordInGrid(currentState.grid, result.wordObj)
                val isNowComplete = gameEngine.isLevelComplete(level, updatedFound)

                val finalScore = if (isNowComplete) newScore + calculateScoreUseCase.getLevelCompletionBonus() else newScore

                // Save state
                savedStateHandle[KEY_FOUND_WORDS] = updatedFound.toList()
                savedStateHandle[KEY_SCORE] = finalScore
                viewModelScope.launch {
                    userPreferencesRepository.updateScore(finalScore)
                }

                _uiState.update {
                    it.copy(
                        grid = updatedGrid,
                        foundWords = updatedFound,
                        score = finalScore,
                        selectedIndices = emptyList(),
                        currentWord = ""
                    )
                }

                if (isNowComplete) {
                    handleLevelCompletion(level, finalScore)
                }
            }

            is WordValidationResult.ValidBonus -> {
                val upperBonus = result.word.uppercase()
                val updatedBonus = currentState.bonusWords + upperBonus
                val points = calculateScoreUseCase.calculateBonusWordScore(upperBonus.length)
                val newScore = currentState.score + points

                savedStateHandle[KEY_BONUS_WORDS] = updatedBonus.toList()
                savedStateHandle[KEY_SCORE] = newScore
                viewModelScope.launch {
                    userPreferencesRepository.updateScore(newScore)
                }

                _uiState.update {
                    it.copy(
                        bonusWords = updatedBonus,
                        score = newScore,
                        bonusWordBanner = upperBonus,
                        selectedIndices = emptyList(),
                        currentWord = ""
                    )
                }

                bannerJob?.cancel()
                bannerJob = viewModelScope.launch {
                    delay(2200)
                    _uiState.update { it.copy(bonusWordBanner = null) }
                }
            }

            is WordValidationResult.AlreadyFound, WordValidationResult.Invalid -> {
                triggerInvalidFeedback()
            }
        }
    }

    fun onSelectionCleared() {
        _uiState.update {
            it.copy(
                selectedIndices = emptyList(),
                currentWord = ""
            )
        }
    }

    private fun triggerInvalidFeedback() {
        _uiState.update {
            it.copy(
                showInvalidFeedback = true,
                selectedIndices = emptyList(),
                currentWord = ""
            )
        }

        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(600)
            _uiState.update { it.copy(showInvalidFeedback = false) }
        }
    }

    fun pauseGame() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeGame() {
        _uiState.update { it.copy(isPaused = false) }
    }
}
