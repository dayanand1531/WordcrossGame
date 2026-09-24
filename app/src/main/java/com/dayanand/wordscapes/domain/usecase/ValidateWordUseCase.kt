package com.dayanand.wordscapes.domain.usecase

import com.dayanand.wordscapes.data.model.Word

sealed class WordValidationResult {
    data class ValidCrossword(val wordObj: Word) : WordValidationResult()
    data class ValidBonus(val word: String) : WordValidationResult()
    data class AlreadyFound(val word: String) : WordValidationResult()
    object Invalid : WordValidationResult()
}

class ValidateWordUseCase {

    fun validate(
        candidate: String,
        crosswordWords: List<Word>,
        bonusWords: List<String>,
        foundWords: Set<String>,
        foundBonusWords: Set<String>
    ): WordValidationResult {
        val normalizedCandidate = candidate.trim().uppercase()
        if (normalizedCandidate.isEmpty()) return WordValidationResult.Invalid

        // Check if already found in crossword
        val matchingCrosswordWord = crosswordWords.find { it.word.uppercase() == normalizedCandidate }
        if (matchingCrosswordWord != null) {
            return if (foundWords.contains(matchingCrosswordWord.word.uppercase())) {
                WordValidationResult.AlreadyFound(matchingCrosswordWord.word.uppercase())
            } else {
                WordValidationResult.ValidCrossword(matchingCrosswordWord)
            }
        }

        // Check if already found in bonus
        val matchingBonusWord = bonusWords.find { it.uppercase() == normalizedCandidate }
        if (matchingBonusWord != null) {
            val upperBonus = matchingBonusWord.uppercase()
            return if (foundBonusWords.contains(upperBonus)) {
                WordValidationResult.AlreadyFound(upperBonus)
            } else {
                WordValidationResult.ValidBonus(upperBonus)
            }
        }

        return WordValidationResult.Invalid
    }
}
