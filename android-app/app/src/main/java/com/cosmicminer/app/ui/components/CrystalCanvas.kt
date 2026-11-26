package com.cosmicminer.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmicminer.app.model.FloatingNumber
import com.cosmicminer.app.ui.GameViewModel
import com.cosmicminer.app.ui.theme.CosmicColors
import com.cosmicminer.app.util.NumberFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CrystalArena(
    isVibrationEnabled: Boolean,
    onCrystalTap: () -> GameViewModel.ClickResult,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val floatingNumbers = remember { mutableStateListOf<FloatingNumber>() }
    var nextId by remember { mutableLongStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "crystal_ambient")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .pointerInput(isVibrationEnabled) {
                    detectTapGestures { tapOffset ->
                        val result = onCrystalTap()

                        if (isVibrationEnabled) {
                            haptic.performHapticFeedback(
                                if (result.isCritical) HapticFeedbackType.LongPress
                                else HapticFeedbackType.TextHandleMove
                            )
                        }

                        scope.launch {
                            scaleAnim.snapTo(0.86f)
                            scaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }

                        val id = nextId++
                        val formatted = "+${NumberFormatter.formatRate(result.gained)}"
                        val label = if (result.isCritical) "$formatted КРИТ!" else formatted
                        floatingNumbers.add(
                            FloatingNumber(
                                id = id,
                                text = label,
                                x = tapOffset.x,
                                y = tapOffset.y,
                                isCritical = result.isCritical
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.42f

                // Ambient cosmic glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmicColors.NeonPurple.copy(alpha = 0.40f),
                            CosmicColors.NeonCyan.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.45f * pulseGlow
                    ),
                    radius = radius * 1.45f * pulseGlow,
                    center = center
                )

                // Rotating orbital ring
                rotate(degrees = orbitAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                CosmicColors.NeonCyan.copy(alpha = 0.85f),
                                Color.Transparent,
                                CosmicColors.NeonPurpleBright.copy(alpha = 0.75f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 1.05f, center.y - radius * 1.05f),
                        size = androidx.compose.ui.geometry.Size(radius * 2.1f, radius * 2.1f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Orbiting energy node
                    val nodeAngleRad = Math.toRadians(35.0)
                    val nodePos = Offset(
                        x = center.x + (radius * 1.05f * cos(nodeAngleRad)).toFloat(),
                        y = center.y + (radius * 1.05f * sin(nodeAngleRad)).toFloat()
                    )
                    drawCircle(
                        color = CosmicColors.NeonCyan,
                        radius = 5.dp.toPx(),
                        center = nodePos
                    )
                }

                // Crystal Geometry (6-point faceted cosmic crystal)
                val top = Offset(center.x, center.y - radius * 0.88f)
                val upperRight = Offset(center.x + radius * 0.58f, center.y - radius * 0.28f)
                val lowerRight = Offset(center.x + radius * 0.48f, center.y + radius * 0.45f)
                val bottom = Offset(center.x, center.y + radius * 0.90f)
                val lowerLeft = Offset(center.x - radius * 0.48f, center.y + radius * 0.45f)
                val upperLeft = Offset(center.x - radius * 0.58f, center.y - radius * 0.28f)
                val core = Offset(center.x, center.y - radius * 0.04f)

                val outerPath = Path().apply {
                    moveTo(top.x, top.y)
                    lineTo(upperRight.x, upperRight.y)
                    lineTo(lowerRight.x, lowerRight.y)
                    lineTo(bottom.x, bottom.y)
                    lineTo(lowerLeft.x, lowerLeft.y)
                    lineTo(upperLeft.x, upperLeft.y)
                    close()
                }

                // Base crystal gradient fill
                drawPath(
                    path = outerPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF67E8F9),
                            CosmicColors.NeonPurple,
                            Color(0xFF4C1D95)
                        ),
                        start = top,
                        end = bottom
                    )
                )

                // Left bright facet
                val leftFacet = Path().apply {
                    moveTo(top.x, top.y)
                    lineTo(core.x, core.y)
                    lineTo(lowerLeft.x, lowerLeft.y)
                    lineTo(upperLeft.x, upperLeft.y)
                    close()
                }
                drawPath(
                    path = leftFacet,
                    color = CosmicColors.NeonCyan.copy(alpha = 0.28f)
                )

                // Right deep facet
                val rightFacet = Path().apply {
                    moveTo(core.x, core.y)
                    lineTo(upperRight.x, upperRight.y)
                    lineTo(lowerRight.x, lowerRight.y)
                    lineTo(bottom.x, bottom.y)
                    close()
                }
                drawPath(
                    path = rightFacet,
                    color = Color(0xFF1E1B4B).copy(alpha = 0.42f)
                )

                // Facet edges & specular highlight
                val facetLines = listOf(
                    top to core,
                    upperLeft to core,
                    upperRight to core,
                    lowerLeft to core,
                    lowerRight to core,
                    bottom to core
                )
                facetLines.forEach { (start, end) ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = start,
                        end = end,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                drawPath(
                    path = outerPath,
                    color = CosmicColors.NeonCyan.copy(alpha = 0.8f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Floating "+X" numbers over tap coordinates
            floatingNumbers.forEach { item ->
                key(item.id) {
                    FloatingTapNumber(
                        item = item,
                        onFinished = { floatingNumbers.removeAll { it.id == item.id } }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingTapNumber(
    item: FloatingNumber,
    onFinished: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val density = LocalDensity.current

    LaunchedEffect(item.id) {
        launch {
            offsetY.animateTo(
                targetValue = with(density) { -96.dp.toPx() },
                animationSpec = tween(durationMillis = 780)
            )
        }
        launch {
            delay(250L)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 530)
            )
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = item.text,
            color = if (item.isCritical) CosmicColors.NeonGold else CosmicColors.NeonCyan,
            fontSize = if (item.isCritical) 20.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (item.x - 24f).roundToInt(),
                        y = (item.y + offsetY.value - 32f).roundToInt()
                    )
                }
                .graphicsLayer { this.alpha = alpha.value }
        )
    }
}
