package com.dayanand.wordscapes.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun LetterWheel(
    letters: List<Char>,
    modifier: Modifier = Modifier,
    onGestureActive: (Boolean) -> Unit = {},
    onLetterSelected: (index: Int) -> Unit = {},
    onSelectionComplete: (indices: List<Int>) -> Unit = {},
    onSelectionCleared: () -> Unit = {}
) {
    if (letters.isEmpty()) return

    val selectedIndices = remember { mutableStateListOf<Int>() }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val swapAnim = remember { Animatable(1f) }
    LaunchedEffect(letters) {
        swapAnim.snapTo(1.15f)
        swapAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val center = Offset(width / 2f, height / 2f)

        // Calculate layout geometry
        val outerRadius = (minOf(width, height) / 2f) * 0.85f
        val letterCircleRadius = outerRadius * 0.30f
        val hitRadius = letterCircleRadius * 1.4f
        val wheelPlacementRadius = outerRadius * 0.68f

        val letterCenters = remember(letters.size, center, wheelPlacementRadius) {
            val count = letters.size
            val startAngle = -PI / 2.0 // Top center
            val angleStep = 2.0 * PI / count

            (0 until count).map { i ->
                val angle = startAngle + i * angleStep
                Offset(
                    x = (center.x + wheelPlacementRadius * cos(angle)).toFloat(),
                    y = (center.y + wheelPlacementRadius * sin(angle)).toFloat()
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(letters) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            onGestureActive(true)
                            selectedIndices.clear()
                            currentTouchPos = startOffset

                            // Check if initial touch lands on a letter
                            for (i in letterCenters.indices) {
                                val dist = sqrt(
                                    (startOffset.x - letterCenters[i].x) * (startOffset.x - letterCenters[i].x) +
                                            (startOffset.y - letterCenters[i].y) * (startOffset.y - letterCenters[i].y)
                                )
                                if (dist <= hitRadius) {
                                    selectedIndices.add(i)
                                    onLetterSelected(i)
                                    break
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            currentTouchPos = pos

                            // Check collision with any letter
                            for (i in letterCenters.indices) {
                                val dist = sqrt(
                                    (pos.x - letterCenters[i].x) * (pos.x - letterCenters[i].x) +
                                            (pos.y - letterCenters[i].y) * (pos.y - letterCenters[i].y)
                                )
                                if (dist <= hitRadius) {
                                    if (!selectedIndices.contains(i)) {
                                        selectedIndices.add(i)
                                        onLetterSelected(i)
                                    }
                                    break
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onGestureActive(false)
                            currentTouchPos = null
                            val finalIndices = selectedIndices.toList()
                            selectedIndices.clear()
                            onSelectionComplete(finalIndices)
                        },
                        onDragCancel = {
                            isDragging = false
                            onGestureActive(false)
                            currentTouchPos = null
                            selectedIndices.clear()
                            onSelectionCleared()
                        }
                    )
                }
        ) {
            // 1. Draw outer circular wheel background
            drawCircle(
                color = Color(0x33000000),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF1E2A38).copy(alpha = 0.85f),
                radius = outerRadius * 0.96f,
                center = center
            )
            drawCircle(
                color = Color(0x1AFFFFFF),
                radius = outerRadius * 0.96f,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 2. Draw connecting lines between selected letters + line to current touch position
            if (selectedIndices.isNotEmpty()) {
                val path = Path()
                val firstCenter = letterCenters[selectedIndices.first()]
                path.moveTo(firstCenter.x, firstCenter.y)

                for (idx in 1 until selectedIndices.size) {
                    val pt = letterCenters[selectedIndices[idx]]
                    path.lineTo(pt.x, pt.y)
                }

                if (isDragging && currentTouchPos != null) {
                    val lastPt = letterCenters[selectedIndices.last()]
                    path.lineTo(lastPt.x, lastPt.y)
                    path.lineTo(currentTouchPos!!.x, currentTouchPos!!.y)
                }

                // Draw background path glow
                drawPath(
                    path = path,
                    color = Color(0xFFFFC107).copy(alpha = 0.4f),
                    style = Stroke(
                        width = letterCircleRadius * 0.8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw main path line
                drawPath(
                    path = path,
                    color = Color(0xFFFFCA28),
                    style = Stroke(
                        width = 12.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Draw letter nodes
            for (i in letters.indices) {
                val nodeCenter = letterCenters[i]
                val isSelected = selectedIndices.contains(i)

                val baseRadius = if (isSelected) letterCircleRadius * 1.12f else letterCircleRadius
                val nodeRadius = baseRadius * swapAnim.value

                // Node shadow / glow
                if (isSelected) {
                    drawCircle(
                        color = Color(0xFFFFCA28).copy(alpha = 0.5f),
                        radius = nodeRadius + 6.dp.toPx(),
                        center = nodeCenter
                    )
                } else {
                    drawCircle(
                        color = Color(0x33000000),
                        radius = nodeRadius + 2.dp.toPx(),
                        center = nodeCenter + Offset(0f, 4.dp.toPx())
                    )
                }

                // Node background
                val bgColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF2C3E50)
                drawCircle(
                    color = bgColor,
                    radius = nodeRadius,
                    center = nodeCenter
                )

                // Node border
                val borderColor = if (isSelected) Color.White else Color(0xFF455A64)
                drawCircle(
                    color = borderColor,
                    radius = nodeRadius,
                    center = nodeCenter,
                    style = Stroke(width = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx())
                )

                // Render letter text
                val charStr = letters[i].uppercase()
                val fontSizeSp = (nodeRadius * 0.9f / density.density).sp
                val textStyle = TextStyle(
                    color = if (isSelected) Color(0xFF1E2A38) else Color.White,
                    fontSize = fontSizeSp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                val textLayoutResult = textMeasurer.measure(
                    text = charStr,
                    style = textStyle
                )

                val topLeft = Offset(
                    x = nodeCenter.x - textLayoutResult.size.width / 2f,
                    y = nodeCenter.y - textLayoutResult.size.height / 2f
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = topLeft
                )
            }
        }
    }
}
