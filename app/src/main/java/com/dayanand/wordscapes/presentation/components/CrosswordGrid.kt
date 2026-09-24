package com.dayanand.wordscapes.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayanand.wordscapes.data.model.GridCell

@Composable
fun CrosswordGrid(
    cells: List<GridCell>,
    modifier: Modifier = Modifier
) {
    if (cells.isEmpty()) return

    val minRow = cells.minOf { it.row }
    val maxRow = cells.maxOf { it.row }
    val minCol = cells.minOf { it.col }
    val maxCol = cells.maxOf { it.col }

    val totalRows = maxRow - minRow + 1
    val totalCols = maxCol - minCol + 1

    val cellMap = remember(cells) {
        cells.associateBy { Pair(it.row, it.col) }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        val cellSize = minOf(
            (maxWidth - (totalCols * 6).dp) / totalCols,
            (maxHeight - (totalRows * 6).dp) / totalRows
        ).coerceIn(32.dp, 56.dp)

        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in minRow..maxRow) {
                Row {
                    for (c in minCol..maxCol) {
                        val cell = cellMap[Pair(r, c)]
                        if (cell != null) {
                            GridCellItem(
                                cell = cell,
                                cellSize = cellSize
                            )
                        } else {
                            // Transparent spacer for layout alignment
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(cellSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCellItem(
    cell: GridCell,
    cellSize: Dp
) {
    val scaleAnim = remember { Animatable(if (cell.isRevealed) 1f else 0.9f) }

    LaunchedEffect(cell.isRevealed, cell.isNewlyRevealed) {
        if (cell.isNewlyRevealed) {
            scaleAnim.snapTo(1.0f)
            scaleAnim.animateTo(
                targetValue = 1.15f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else if (cell.isRevealed) {
            scaleAnim.animateTo(1f)
        }
    }

    val shape = RoundedCornerShape(8.dp)

    val bgColor = when {
        cell.isNewlyRevealed -> Color(0xFFFFD54F) // Vibrant gold highlight for hint/new reveal
        cell.isRevealed -> Color(0xFFFFF8E1)
        else -> Color(0xFF1E2A38)
    }

    val borderColor = if (cell.isRevealed) Color(0xFFFFC107) else Color(0xFF37474F)
    val textColor = if (cell.isRevealed) Color(0xFF2E1C00) else Color.Transparent

    Box(
        modifier = Modifier
            .padding(3.dp)
            .size(cellSize)
            .scale(scaleAnim.value)
            .shadow(
                elevation = if (cell.isRevealed) 4.dp else 0.dp,
                shape = shape
            )
            .background(bgColor, shape)
            .border(
                width = if (cell.isRevealed) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cell.isRevealed) {
            Text(
                text = cell.char.uppercase(),
                fontSize = (cellSize.value * 0.55f).sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
