package com.dayanand.wordscapes

import com.dayanand.wordscapes.domain.usecase.CalculateScoreUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateScoreUseCaseTest {

    private lateinit var scoreCalculator: CalculateScoreUseCase

    @Before
    fun setUp() {
        scoreCalculator = CalculateScoreUseCase()
    }

    @Test
    fun `calculateNormalWordScore multiplies length by 10`() {
        val score = scoreCalculator.calculateNormalWordScore(3) // "CAT"
        assertEquals(30, score)
    }

    @Test
    fun `calculateBonusWordScore multiplies length by 20`() {
        val score = scoreCalculator.calculateBonusWordScore(2) // "AT"
        assertEquals(40, score)
    }

    @Test
    fun `getLevelCompletionBonus returns 100`() {
        val bonus = scoreCalculator.getLevelCompletionBonus()
        assertEquals(100, bonus)
    }
}
