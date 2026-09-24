package com.dayanand.wordscapes.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameTopBar(
    levelId: Int,
    score: Int,
    hintsRemaining: Int,
    onHintClicked: () -> Unit,
    onPauseClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(12.dp)
    val canUseHint = hintsRemaining > 0 && score >= 100

    val scoreScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(score) {
        scoreScaleAnim.animateTo(
            targetValue = 1.22f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        scoreScaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Back Button | Level Title | Pause Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .clip(buttonShape)
                    .background(Color(0xFF1E2A38))
                    .clickable { onBackClicked() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◄",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            // Level Title
            Surface(
                shape = buttonShape,
                color = Color(0xFF1E2A38)
            ) {
                Text(
                    text = "LEVEL $levelId",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Pause Button
            Box(
                modifier = Modifier
                    .clip(buttonShape)
                    .background(Color(0xFF1E2A38))
                    .clickable { onPauseClicked() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "❚❚",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        // Row 2: Score Badge | Hint Powerup Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Score Badge
            Surface(
                shape = buttonShape,
                color = Color(0xFFFFC107),
                modifier = Modifier.scale(scoreScaleAnim.value)
            ) {
                Text(
                    text = "★ SCORE: $score",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E2A38),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Hint Button
            val hintBgColor = if (canUseHint) Color(0xFFFFB300) else Color(0xFF37474F)
            val hintTextColor = if (canUseHint) Color(0xFF1E2A38) else Color.White.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .clip(buttonShape)
                    .background(hintBgColor)
                    .clickable { onHintClicked() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💡 HINT: $hintsRemaining (-100)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = hintTextColor
                )
            }
        }
    }
}
