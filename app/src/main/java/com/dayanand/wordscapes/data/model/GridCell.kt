package com.dayanand.wordscapes.data.model

data class GridCell(
    val row: Int,
    val col: Int,
    val char: Char,
    val isRevealed: Boolean = false,
    val isNewlyRevealed: Boolean = false
)
