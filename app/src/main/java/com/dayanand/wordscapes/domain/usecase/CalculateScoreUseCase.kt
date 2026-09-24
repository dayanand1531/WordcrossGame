package com.dayanand.wordscapes.domain.usecase

class CalculateScoreUseCase {
    fun calculateNormalWordScore(wordLength: Int): Int {
        return wordLength * 10
    }

    fun calculateBonusWordScore(wordLength: Int): Int {
        return wordLength * 20
    }

    fun getLevelCompletionBonus(): Int {
        return 100
    }
}
