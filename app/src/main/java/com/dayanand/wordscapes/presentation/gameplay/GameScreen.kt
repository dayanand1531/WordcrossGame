package com.dayanand.wordscapes.presentation.gameplay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayanand.wordscapes.presentation.components.BonusWordBanner
import com.dayanand.wordscapes.presentation.components.CrosswordGrid
import com.dayanand.wordscapes.presentation.components.GameTopBar
import com.dayanand.wordscapes.presentation.components.HintFeedbackBanner
import com.dayanand.wordscapes.presentation.components.KeepScreenOn
import com.dayanand.wordscapes.presentation.components.LetterWheel
import com.dayanand.wordscapes.presentation.components.LevelCompleteDialog
import com.dayanand.wordscapes.presentation.components.ShowOneButton
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    levelId: Int,
    onPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    onNextLevelClick: (nextId: Int) -> Unit,
    onLevelSelectClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    KeepScreenOn()

    BackHandler {
        onBackClick()
    }

    LaunchedEffect(levelId) {
        viewModel.initLevel(levelId)
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is GameNavigationEvent.AutoNavigateToNextLevel -> {
                    onNextLevelClick(event.nextLevelId)
                }
            }
        }
    }

    // Shake animation offset for invalid word feedback
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.showInvalidFeedback) {
        if (uiState.showInvalidFeedback) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val shakeSequence = listOf(-20f, 20f, -15f, 15f, -8f, 8f, 0f)
            for (offsetVal in shakeSequence) {
                shakeOffset.animateTo(offsetVal, spring())
            }
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    // Haptic feedback on level completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = Color(0xFFFFC107),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Bar
                GameTopBar(
                    levelId = uiState.levelId,
                    score = uiState.score,
                    hintsRemaining = uiState.hintsRemaining,
                    onHintClicked = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        viewModel.useHint()
                    },
                    onPauseClicked = onPauseClick,
                    onBackClicked = onBackClick
                )

                // 2. Feedback Banners (Bonus Word / Hint Feedback)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BonusWordBanner(
                        bonusWord = uiState.bonusWordBanner,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    HintFeedbackBanner(
                        message = uiState.hintFeedbackMessage,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                // 3. Crossword Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CrosswordGrid(
                        cells = uiState.grid,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4. Current Word Preview Bubble & Show One Powerup Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .padding(bottom = 6.dp)
                    ) {
                        if (uiState.currentWord.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (uiState.showInvalidFeedback) Color(0xFFD32F2F) else Color(0xFFFFC107),
                                shadowElevation = 6.dp
                            ) {
                                Text(
                                    text = uiState.currentWord,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.showInvalidFeedback) Color.White else Color(0xFF1E2A38),
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }

                    // Show One Button
                    ShowOneButton(
                        showOneRemaining = uiState.showOneRemaining,
                        isGestureActive = uiState.isGestureActive,
                        onShowOneClicked = {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            viewModel.showOne()
                        }
                    )
                }

                // 5. Letter Wheel
                LetterWheel(
                    letters = uiState.letters,
                    onGestureActive = { active ->
                        viewModel.setGestureActive(active)
                    },
                    onLetterSelected = { idx ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onLetterSelected(idx)
                    },
                    onSelectionComplete = { indices ->
                        viewModel.onSelectionComplete(indices)
                    },
                    onSelectionCleared = {
                        viewModel.onSelectionCleared()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(bottom = 8.dp)
                )
            }
        }

        // 6. Level Complete Overlay
        if (uiState.isCompleted) {
            val isLast = uiState.levelId >= uiState.totalLevels
            LevelCompleteDialog(
                score = uiState.score,
                isLastLevel = isLast,
                onNextLevel = {
                    onNextLevelClick(uiState.levelId + 1)
                },
                onLevelSelect = onLevelSelectClick
            )
        }
    }
}
