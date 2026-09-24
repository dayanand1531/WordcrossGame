package com.dayanand.wordscapes.domain.usecase

import com.dayanand.wordscapes.data.model.Direction
import com.dayanand.wordscapes.data.model.GridCell
import com.dayanand.wordscapes.data.model.Level
import com.dayanand.wordscapes.data.model.Word

sealed class UseHintResult {
    data class Success(
        val updatedGrid: List<GridCell>,
        val revealedPosition: Pair<Int, Int>
    ) : UseHintResult()

    object NoUnrevealedCells : UseHintResult()
}

class GameEngine {

    fun generateGridCells(level: Level, foundWords: Set<String>): List<GridCell> {
        val cellMap = mutableMapOf<Pair<Int, Int>, Char>()

        // Populate cell positions and characters
        for (wordObj in level.words) {
            val upperWord = wordObj.word.uppercase()
            for (i in upperWord.indices) {
                val r = if (wordObj.direction == Direction.VERTICAL) wordObj.row + i else wordObj.row
                val c = if (wordObj.direction == Direction.HORIZONTAL) wordObj.column + i else wordObj.column
                cellMap[Pair(r, c)] = upperWord[i]
            }
        }

        // Determine which cells are revealed based on foundWords
        val revealedPositions = mutableSetOf<Pair<Int, Int>>()
        for (wordObj in level.words) {
            val upperWord = wordObj.word.uppercase()
            if (foundWords.contains(upperWord)) {
                for (i in upperWord.indices) {
                    val r = if (wordObj.direction == Direction.VERTICAL) wordObj.row + i else wordObj.row
                    val c = if (wordObj.direction == Direction.HORIZONTAL) wordObj.column + i else wordObj.column
                    revealedPositions.add(Pair(r, c))
                }
            }
        }

        return cellMap.map { (pos, char) ->
            GridCell(
                row = pos.first,
                col = pos.second,
                char = char,
                isRevealed = revealedPositions.contains(pos),
                isNewlyRevealed = false
            )
        }
    }

    fun revealWordInGrid(
        currentCells: List<GridCell>,
        wordToReveal: Word
    ): List<GridCell> {
        val upperWord = wordToReveal.word.uppercase()
        val wordPositions = mutableSetOf<Pair<Int, Int>>()
        for (i in upperWord.indices) {
            val r = if (wordToReveal.direction == Direction.VERTICAL) wordToReveal.row + i else wordToReveal.row
            val c = if (wordToReveal.direction == Direction.HORIZONTAL) wordToReveal.column + i else wordToReveal.column
            wordPositions.add(Pair(r, c))
        }

        return currentCells.map { cell ->
            val pos = Pair(cell.row, cell.col)
            if (wordPositions.contains(pos)) {
                val wasAlreadyRevealed = cell.isRevealed
                cell.copy(
                    isRevealed = true,
                    isNewlyRevealed = !wasAlreadyRevealed
                )
            } else {
                cell.copy(isNewlyRevealed = false)
            }
        }
    }

    fun applyHint(
        level: Level,
        currentGrid: List<GridCell>,
        foundWords: Set<String>
    ): UseHintResult {
        val gridMap = currentGrid.associateBy { Pair(it.row, it.col) }

        // Filter unsolved words
        val unsolvedWords = level.words.filter { wordObj ->
            !foundWords.contains(wordObj.word.uppercase())
        }

        var targetPos: Pair<Int, Int>? = null

        for (wordObj in unsolvedWords) {
            val upperWord = wordObj.word.uppercase()
            for (i in upperWord.indices) {
                val r = if (wordObj.direction == Direction.VERTICAL) wordObj.row + i else wordObj.row
                val c = if (wordObj.direction == Direction.HORIZONTAL) wordObj.column + i else wordObj.column
                val pos = Pair(r, c)
                val cell = gridMap[pos]
                if (cell != null && !cell.isRevealed) {
                    targetPos = pos
                    break
                }
            }
            if (targetPos != null) break
        }

        if (targetPos == null) {
            return UseHintResult.NoUnrevealedCells
        }

        val updatedGrid = currentGrid.map { cell ->
            if (Pair(cell.row, cell.col) == targetPos) {
                cell.copy(
                    isRevealed = true,
                    isNewlyRevealed = true
                )
            } else {
                cell.copy(isNewlyRevealed = false)
            }
        }

        return UseHintResult.Success(updatedGrid, targetPos)
    }

    fun swapTwoLetters(letters: List<Char>): List<Char> {
        if (letters.size < 2) return letters
        val mutable = letters.toMutableList()

        val pairs = mutableListOf<Pair<Int, Int>>()
        for (i in letters.indices) {
            for (j in i + 1 until letters.size) {
                pairs.add(Pair(i, j))
            }
        }

        if (pairs.isNotEmpty()) {
            val nonIdentical = pairs.filter { (i, j) -> letters[i] != letters[j] }
            val chosenPair = if (nonIdentical.isNotEmpty()) nonIdentical.random() else pairs.random()
            val idx1 = chosenPair.first
            val idx2 = chosenPair.second

            val temp = mutable[idx1]
            mutable[idx1] = mutable[idx2]
            mutable[idx2] = temp
        }

        return mutable
    }

    fun clearNewlyRevealedFlags(cells: List<GridCell>): List<GridCell> {
        return cells.map { it.copy(isNewlyRevealed = false) }
    }

    fun isLevelComplete(level: Level, foundWords: Set<String>): Boolean {
        if (level.words.isEmpty()) return false
        val requiredWords = level.words.map { it.word.uppercase() }.toSet()
        return foundWords.map { it.uppercase() }.containsAll(requiredWords)
    }
}
