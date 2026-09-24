package com.dayanand.wordscapes.data.model

enum class Direction {
    HORIZONTAL,
    VERTICAL
}

data class Word(
    val word: String,
    val row: Int,
    val column: Int,
    val direction: Direction
)
