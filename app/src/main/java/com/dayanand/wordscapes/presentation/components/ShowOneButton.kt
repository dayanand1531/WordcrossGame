package com.dayanand.wordscapes.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ShowOneButton(
    showOneRemaining: Int,
    isGestureActive: Boolean,
    onShowOneClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = showOneRemaining > 0 && !isGestureActive
    val buttonShape = RoundedCornerShape(16.dp)

    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val bgColor = if (enabled) Color(0xFF1E2A38) else Color(0xFF263238)
    val borderColor = if (enabled) Color(0xFF4FC3F7) else Color(0xFF37474F)
    val textColor = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .scale(scaleAnim.value)
            .clip(buttonShape)
            .background(bgColor)
            .border(width = 1.5.dp, color = borderColor, shape = buttonShape)
            .clickable(enabled = true) {
                if (enabled) {
                    scope.launch {
                        scaleAnim.animateTo(0.92f, spring())
                        scaleAnim.animateTo(1f, spring())
                    }
                }
                onShowOneClicked()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🔄 ",
                fontSize = 15.sp
            )
            Text(
                text = "Show One × $showOneRemaining",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}
