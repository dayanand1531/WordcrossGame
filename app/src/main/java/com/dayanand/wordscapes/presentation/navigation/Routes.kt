package com.dayanand.wordscapes.presentation.navigation

object Routes {
    const val HOME = "home"
    const val LEVEL_SELECT = "level_select"
    const val GAMEPLAY = "gameplay/{levelId}"
    const val PAUSE = "pause/{levelId}"

    fun gameplay(levelId: Int): String = "gameplay/$levelId"
    fun pause(levelId: Int): String = "pause/$levelId"
}
