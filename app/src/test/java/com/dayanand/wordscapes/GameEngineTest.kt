package com.dayanand.wordscapes

import com.dayanand.wordscapes.data.model.Direction
import com.dayanand.wordscapes.data.model.Level
import com.dayanand.wordscapes.data.model.Word
import com.dayanand.wordscapes.domain.usecase.GameEngine
import com.dayanand.wordscapes.domain.usecase.UseHintResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var gameEngine: GameEngine

    private val sampleLevel = Level(
        id = 1,
        letters = listOf("C", "A", "T"),
        words = listOf(
            Word("CAT", 0, 0, Direction.HORIZONTAL),
            Word("ACT", 0, 1, Direction.VERTICAL)
        ),
        bonusWords = listOf("AT")
    )

    @Before
    fun setUp() {
        gameEngine = GameEngine()
    }

    @Test
    fun `generateGridCells produces correct cell count and characters`() {
        val cells = gameEngine.generateGridCells(sampleLevel, emptySet())
        assertEquals(5, cells.size)

        val unrevealedCount = cells.count { !it.isRevealed }
        assertEquals(5, unrevealedCount)
    }

    @Test
    fun `revealWordInGrid updates revealed state for word cells`() {
        val initialCells = gameEngine.generateGridCells(sampleLevel, emptySet())
        val catWord = sampleLevel.words[0]

        val updatedCells = gameEngine.revealWordInGrid(initialCells, catWord)

        val revealedCatCells = updatedCells.filter { it.isRevealed }
        assertEquals(3, revealedCatCells.size)
    }

    @Test
    fun `isLevelComplete returns true when all required words are found`() {
        val incomplete = gameEngine.isLevelComplete(sampleLevel, setOf("CAT"))
        assertFalse(incomplete)

        val complete = gameEngine.isLevelComplete(sampleLevel, setOf("CAT", "ACT"))
        assertTrue(complete)
    }

    @Test
    fun `applyHint reveals one and only one cell`() {
        val initialCells = gameEngine.generateGridCells(sampleLevel, emptySet())
        val initialRevealedCount = initialCells.count { it.isRevealed }
        assertEquals(0, initialRevealedCount)

        val result = gameEngine.applyHint(sampleLevel, initialCells, emptySet())
        assertTrue(result is UseHintResult.Success)

        val success = result as UseHintResult.Success
        val newRevealedCount = success.updatedGrid.count { it.isRevealed }
        assertEquals(1, newRevealedCount)
    }

    @Test
    fun `applyHint skips already solved words`() {
        // "CAT" is already solved
        val initialCells = gameEngine.generateGridCells(sampleLevel, setOf("CAT"))
        // "ACT" has cells at (0,1) A, (1,1) C, (2,1) T. (0,1) was revealed by CAT. Unrevealed cells in ACT are (1,1) and (2,1).
        val result = gameEngine.applyHint(sampleLevel, initialCells, setOf("CAT"))

        assertTrue(result is UseHintResult.Success)
        val success = result as UseHintResult.Success
        val revealedPos = success.revealedPosition

        // Position revealed must belong to unsolved word "ACT" (not (0,0) or (0,2))
        assertTrue(revealedPos == Pair(1, 1) || revealedPos == Pair(2, 1))
    }

    @Test
    fun `applyHint returns NoUnrevealedCells when all cells are revealed`() {
        // Both "CAT" and "ACT" found
        val initialCells = gameEngine.generateGridCells(sampleLevel, setOf("CAT", "ACT"))
        val result = gameEngine.applyHint(sampleLevel, initialCells, setOf("CAT", "ACT"))

        assertTrue(result is UseHintResult.NoUnrevealedCells)
    }
}
