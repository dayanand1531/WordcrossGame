package com.dayanand.wordscapes

import com.dayanand.wordscapes.data.model.Direction
import com.dayanand.wordscapes.data.model.Word
import com.dayanand.wordscapes.domain.usecase.ValidateWordUseCase
import com.dayanand.wordscapes.domain.usecase.WordValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateWordUseCaseTest {

    private lateinit var useCase: ValidateWordUseCase

    private val crosswordWords = listOf(
        Word("CAT", 0, 0, Direction.HORIZONTAL),
        Word("ACT", 0, 1, Direction.VERTICAL)
    )
    private val bonusWords = listOf("AT")

    @Before
    fun setUp() {
        useCase = ValidateWordUseCase()
    }

    @Test
    fun `validate valid crossword word returns ValidCrossword`() {
        val result = useCase.validate(
            candidate = "cat",
            crosswordWords = crosswordWords,
            bonusWords = bonusWords,
            foundWords = emptySet(),
            foundBonusWords = emptySet()
        )

        assertTrue(result is WordValidationResult.ValidCrossword)
        val validResult = result as WordValidationResult.ValidCrossword
        assertEquals("CAT", validResult.wordObj.word)
    }

    @Test
    fun `validate valid bonus word returns ValidBonus`() {
        val result = useCase.validate(
            candidate = "at",
            crosswordWords = crosswordWords,
            bonusWords = bonusWords,
            foundWords = emptySet(),
            foundBonusWords = emptySet()
        )

        assertTrue(result is WordValidationResult.ValidBonus)
        val bonusResult = result as WordValidationResult.ValidBonus
        assertEquals("AT", bonusResult.word)
    }

    @Test
    fun `validate duplicate crossword word returns AlreadyFound`() {
        val result = useCase.validate(
            candidate = "CAT",
            crosswordWords = crosswordWords,
            bonusWords = bonusWords,
            foundWords = setOf("CAT"),
            foundBonusWords = emptySet()
        )

        assertTrue(result is WordValidationResult.AlreadyFound)
    }

    @Test
    fun `validate invalid word returns Invalid`() {
        val result = useCase.validate(
            candidate = "DOG",
            crosswordWords = crosswordWords,
            bonusWords = bonusWords,
            foundWords = emptySet(),
            foundBonusWords = emptySet()
        )

        assertEquals(WordValidationResult.Invalid, result)
    }
}
