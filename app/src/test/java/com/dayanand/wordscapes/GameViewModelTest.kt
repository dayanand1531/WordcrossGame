package com.dayanand.wordscapes

import androidx.lifecycle.SavedStateHandle
import com.dayanand.wordscapes.data.model.Direction
import com.dayanand.wordscapes.data.model.Level
import com.dayanand.wordscapes.data.model.Word
import com.dayanand.wordscapes.data.repository.LevelRepository
import com.dayanand.wordscapes.data.repository.UserPreferencesRepository
import com.dayanand.wordscapes.domain.usecase.CalculateScoreUseCase
import com.dayanand.wordscapes.domain.usecase.GameEngine
import com.dayanand.wordscapes.domain.usecase.ValidateWordUseCase
import com.dayanand.wordscapes.presentation.gameplay.GameNavigationEvent
import com.dayanand.wordscapes.presentation.gameplay.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val level1 = Level(
        id = 1,
        letters = listOf("C", "A", "T"),
        words = listOf(
            Word("CAT", 0, 0, Direction.HORIZONTAL),
            Word("ACT", 0, 1, Direction.VERTICAL)
        ),
        bonusWords = listOf("AT")
    )

    private val level2 = Level(
        id = 2,
        letters = listOf("D", "O", "G"),
        words = listOf(
            Word("DOG", 1, 0, Direction.HORIZONTAL)
        ),
        bonusWords = listOf("DO")
    )

    private val multiLevelRepo = object : LevelRepository {
        override suspend fun getLevels(): List<Level> = listOf(level1, level2)
        override suspend fun getLevel(id: Int): Level = if (id == 2) level2 else level1
        override suspend fun getTotalLevels(): Int = 2
    }

    private val singleLevelRepo = object : LevelRepository {
        override suspend fun getLevels(): List<Level> = listOf(level1)
        override suspend fun getLevel(id: Int): Level = level1
        override suspend fun getTotalLevels(): Int = 1
    }

    private val fakeUserPrefsRepository = object : UserPreferencesRepository {
        override val unlockedLevelFlow: Flow<Int> = flowOf(1)
        override val completedLevelsFlow: Flow<Set<Int>> = flowOf(emptySet())
        override val totalScoreFlow: Flow<Int> = flowOf(0)
        override suspend fun completeLevel(levelId: Int, nextLevelId: Int, scoreGained: Int) {}
        override suspend fun updateScore(newScore: Int) {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUseHintWithSufficientScoreDeducts100PointsAnd1Hint() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "saved_level_id" to 1,
                "saved_score" to 250
            )
        )
        val viewModel = GameViewModel(
            savedStateHandle = savedStateHandle,
            levelRepository = multiLevelRepo,
            userPreferencesRepository = fakeUserPrefsRepository,
            validateWordUseCase = ValidateWordUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase(),
            gameEngine = GameEngine()
        )

        viewModel.initLevel(1)

        assertEquals(250, viewModel.uiState.value.score)
        assertEquals(3, viewModel.uiState.value.hintsRemaining)

        viewModel.useHint()

        assertEquals(150, viewModel.uiState.value.score)
        assertEquals(2, viewModel.uiState.value.hintsRemaining)
        assertEquals(150, savedStateHandle.get<Int>("saved_score"))
        assertEquals(2, savedStateHandle.get<Int>("saved_hints_remaining"))
    }

    @Test
    fun testUseHintWithScoreLessThan100IsDisabled() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "saved_level_id" to 1,
                "saved_score" to 50
            )
        )
        val viewModel = GameViewModel(
            savedStateHandle = savedStateHandle,
            levelRepository = multiLevelRepo,
            userPreferencesRepository = fakeUserPrefsRepository,
            validateWordUseCase = ValidateWordUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase(),
            gameEngine = GameEngine()
        )

        viewModel.initLevel(1)

        assertEquals(50, viewModel.uiState.value.score)
        assertEquals(3, viewModel.uiState.value.hintsRemaining)

        viewModel.useHint()

        assertEquals(50, viewModel.uiState.value.score)
        assertEquals(3, viewModel.uiState.value.hintsRemaining)
        assertEquals("Need 100 points for a hint! (Score: 50)", viewModel.uiState.value.hintFeedbackMessage)
    }

    @Test
    fun testCompletingNonFinalLevelEmitsAutoNavigateToNextLevelEvent() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("saved_level_id" to 1))
        val viewModel = GameViewModel(
            savedStateHandle = savedStateHandle,
            levelRepository = multiLevelRepo,
            userPreferencesRepository = fakeUserPrefsRepository,
            validateWordUseCase = ValidateWordUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase(),
            gameEngine = GameEngine()
        )

        viewModel.initLevel(1)

        // Solve word 1: CAT (indices 0, 1, 2)
        viewModel.onSelectionComplete(listOf(0, 1, 2))
        // Solve word 2: ACT (indices 1, 0, 2)
        viewModel.onSelectionComplete(listOf(1, 0, 2))

        assertTrue(viewModel.uiState.value.isCompleted)

        val navEvent = viewModel.navigationEvent.first()
        assertTrue(navEvent is GameNavigationEvent.AutoNavigateToNextLevel)
        assertEquals(2, (navEvent as GameNavigationEvent.AutoNavigateToNextLevel).nextLevelId)
    }

    @Test
    fun testCompletingFinalLevelDoesNotNavigateToNonExistentLevel() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("saved_level_id" to 1))
        val viewModel = GameViewModel(
            savedStateHandle = savedStateHandle,
            levelRepository = singleLevelRepo,
            userPreferencesRepository = fakeUserPrefsRepository,
            validateWordUseCase = ValidateWordUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase(),
            gameEngine = GameEngine()
        )

        viewModel.initLevel(1)

        // Solve CAT and ACT on single level
        viewModel.onSelectionComplete(listOf(0, 1, 2))
        viewModel.onSelectionComplete(listOf(1, 0, 2))

        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(1, viewModel.uiState.value.totalLevels)
    }

    @Test
    fun testShowOneSwapsExactlyTwoCharactersAndDecreasesRemainingByOne() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("saved_level_id" to 1))
        val viewModel = GameViewModel(
            savedStateHandle = savedStateHandle,
            levelRepository = multiLevelRepo,
            userPreferencesRepository = fakeUserPrefsRepository,
            validateWordUseCase = ValidateWordUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase(),
            gameEngine = GameEngine()
        )

        viewModel.initLevel(1)

        val beforeLetters = viewModel.uiState.value.letters
        val beforeScore = viewModel.uiState.value.score
        assertEquals(3, viewModel.uiState.value.showOneRemaining)

        viewModel.showOne()

        val afterLetters = viewModel.uiState.value.letters
        assertEquals(2, viewModel.uiState.value.showOneRemaining)
        assertEquals(beforeLetters.size, afterLetters.size)
        assertEquals(beforeLetters.sorted(), afterLetters.sorted())
        assertEquals(beforeScore, viewModel.uiState.value.score)
    }

    @Test
    fun testShowOneDisabledWhenRemainingIsZero() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "saved_level_id" to 1,
                "saved_show_one_remaining" to 0
            )
        )
        val viewModel = GameViewModel(
            savedStateHandle = savedStateHandle,
            levelRepository = multiLevelRepo,
            userPreferencesRepository = fakeUserPrefsRepository,
            validateWordUseCase = ValidateWordUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase(),
            gameEngine = GameEngine()
        )

        viewModel.initLevel(1)

        val beforeLetters = viewModel.uiState.value.letters
        assertEquals(0, viewModel.uiState.value.showOneRemaining)

        viewModel.showOne()

        assertEquals(0, viewModel.uiState.value.showOneRemaining)
        assertEquals(beforeLetters, viewModel.uiState.value.letters)
        assertEquals("No Show One remaining!", viewModel.uiState.value.hintFeedbackMessage)
    }

    @Test
    fun testShowOneCannotOccurWhileGestureIsActive() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("saved_level_id" to 1))
        val viewModel = GameViewModel(
            savedStateHandle = savedStateHandle,
            levelRepository = multiLevelRepo,
            userPreferencesRepository = fakeUserPrefsRepository,
            validateWordUseCase = ValidateWordUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase(),
            gameEngine = GameEngine()
        )

        viewModel.initLevel(1)

        viewModel.setGestureActive(true)
        val beforeLetters = viewModel.uiState.value.letters

        viewModel.showOne()

        assertEquals(3, viewModel.uiState.value.showOneRemaining)
        assertEquals(beforeLetters, viewModel.uiState.value.letters)
        assertEquals("Finish active gesture first!", viewModel.uiState.value.hintFeedbackMessage)
    }
}
