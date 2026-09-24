package com.dayanand.wordscapes.data.model

data class Level(
    val id: Int,
    val letters: List<String>,
    val words: List<Word>,
    val bonusWords: List<String>
)
