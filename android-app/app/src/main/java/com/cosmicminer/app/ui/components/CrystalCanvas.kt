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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.random.Random

private data class SparkParticle(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radiusPx: Float
)

@Composable
fun CrystalArena(
    rankLevel: Int,
    overchargeSecondsLeft: Int,
    isCometVisible: Boolean,
    cometSeed: Long,
    isVibrationEnabled: Boolean,
    onCrystalTap: () -> GameViewModel.ClickResult,
    onCometTap: () -> Double,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    val floatingNumbers = remember { mutableStateListOf<FloatingNumber>() }
    val sparks = remember { mutableStateListOf<SparkParticle>() }
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
    val counterOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_orbit"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Evolution stage based on player level (1..200)
    val evolutionStage = when {
        rankLevel >= 150 -> 4 // Black hole / plasma event horizon
        rankLevel >= 100 -> 3 // Twin rings + golden plasma core
        rankLevel >= 50 -> 2  // Gold facet edges + 2nd ring
        else -> 1             // Classic cosmic crystal
    }
    val isOvercharged = overchargeSecondsLeft > 0

    Box(
        modifier = modifier.graphicsLayer {
            translationX = shakeX.value
            translationY = shakeY.value
        },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isOvercharged) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicColors.NeonGold.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ПЕРЕГРУЗКА x5 · ${overchargeSecondsLeft}с",
                        color = CosmicColors.NeonGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(230.dp)
                    .pointerInput(isVibrationEnabled, isOvercharged) {
                        detectTapGestures { tapOffset ->
                            val result = onCrystalTap()

                            if (isVibrationEnabled) {
                                haptic.performHapticFeedback(
                                    if (result.isCritical) HapticFeedbackType.LongPress
                                    else HapticFeedbackType.TextHandleMove
                                )
                            }

                            scope.launch {
                                scaleAnim.snapTo(if (result.isCritical) 0.80f else 0.87f)
                                scaleAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }

                            if (result.isCritical) {
                                scope.launch {
                                    flashAlpha.snapTo(0.75f)
                                    flashAlpha.animateTo(0f, tween(260))
                                }
                                scope.launch {
                                    repeat(4) {
                                        shakeX.snapTo(Random.nextFloat() * 14f - 7f)
                                        shakeY.snapTo(Random.nextFloat() * 14f - 7f)
                                        delay(28L)
                                    }
                                    shakeX.snapTo(0f)
                                    shakeY.snapTo(0f)
                                }
                            }

                            val sparkCount = if (result.isCritical) 12 else 5
                            for (i in 0 until sparkCount) {
                                val angle = (Math.PI * 2.0 * i / sparkCount) + Random.nextDouble() * 0.5
                                val speed = if (result.isCritical) 135f + Random.nextFloat() * 70f else 65f + Random.nextFloat() * 55f
                                val sparkId = nextId++
                                sparks.add(
                                    SparkParticle(
                                        id = sparkId,
                                        startX = tapOffset.x,
                                        startY = tapOffset.y,
                                        vx = (cos(angle) * speed).toFloat(),
                                        vy = (sin(angle) * speed).toFloat(),
                                        color = if (result.isCritical) CosmicColors.NeonGold else if (i % 2 == 0) CosmicColors.NeonCyan else CosmicColors.NeonPurpleBright,
                                        radiusPx = if (result.isCritical) 9f else 5.5f
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
                    val radius = size.minDimension * 0.40f

                    // Stage 4: Black Hole Accretion Halo
                    if (evolutionStage >= 4) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color(0xFF3B0764).copy(alpha = 0.85f),
                                    CosmicColors.NeonGold.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius * 1.65f * pulseGlow
                            ),
                            radius = radius * 1.65f * pulseGlow,
                            center = center
                        )
                    } else {
                        val glowPrimary = if (isOvercharged) CosmicColors.NeonGold else CosmicColors.NeonPurple
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowPrimary.copy(alpha = if (evolutionStage >= 3) 0.52f else 0.40f),
                                    CosmicColors.NeonCyan.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius * 1.48f * pulseGlow
                            ),
                            radius = radius * 1.48f * pulseGlow,
                            center = center
                        )
                    }

                    // Primary Orbital Ring
                    rotate(degrees = orbitAngle, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    if (isOvercharged) CosmicColors.NeonGold else CosmicColors.NeonCyan,
                                    Color.Transparent,
                                    CosmicColors.NeonPurpleBright.copy(alpha = 0.8f),
                                    Color.Transparent
                                ),
                                center = center
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius * 1.08f, center.y - radius * 1.08f),
                            size = Size(radius * 2.16f, radius * 2.16f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val nodeAngleRad = Math.toRadians(35.0)
                        val nodePos = Offset(
                            x = center.x + (radius * 1.08f * cos(nodeAngleRad)).toFloat(),
                            y = center.y + (radius * 1.08f * sin(nodeAngleRad)).toFloat()
                        )
                        drawCircle(
                            color = if (isOvercharged) CosmicColors.NeonGold else CosmicColors.NeonCyan,
                            radius = 5.dp.toPx(),
                            center = nodePos
                        )
                    }

                    // Stage 2+: Second Counter-Rotating Ring
                    if (evolutionStage >= 2) {
                        rotate(degrees = counterOrbitAngle, pivot = center) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        CosmicColors.NeonGold.copy(alpha = 0.85f),
                                        Color.Transparent,
                                        CosmicColors.NeonCyan.copy(alpha = 0.55f),
                                        Color.Transparent
                                    ),
                                    center = center
                                ),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(center.x - radius * 1.24f, center.y - radius * 0.86f),
                                size = Size(radius * 2.48f, radius * 1.72f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )

                            if (evolutionStage >= 3) {
                                val satPos = Offset(
                                    x = center.x - radius * 1.24f,
                                    y = center.y
                                )
                                drawCircle(
                                    color = CosmicColors.NeonGold,
                                    radius = 4.dp.toPx(),
                                    center = satPos
                                )
                            }
                        }
                    }

                    // Crystal Facets
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

                    val crystalColors = when {
                        isOvercharged -> listOf(Color(0xFFFEF08A), CosmicColors.NeonGold, Color(0xFF78350F))
                        evolutionStage >= 4 -> listOf(Color(0xFF22D3EE), Color(0xFF6D28D9), Color(0xFF090518))
                        evolutionStage >= 3 -> listOf(Color(0xFFFDE68A), Color(0xFF8B5CF6), Color(0xFF312E81))
                        else -> listOf(Color(0xFF67E8F9), CosmicColors.NeonPurple, Color(0xFF4C1D95))
                    }

                    drawPath(
                        path = outerPath,
                        brush = Brush.linearGradient(
                            colors = crystalColors,
                            start = top,
                            end = bottom
                        )
                    )

                    val leftFacet = Path().apply {
                        moveTo(top.x, top.y)
                        lineTo(core.x, core.y)
                        lineTo(lowerLeft.x, lowerLeft.y)
                        lineTo(upperLeft.x, upperLeft.y)
                        close()
                    }
                    drawPath(
                        path = leftFacet,
                        color = if (evolutionStage >= 3) CosmicColors.NeonGold.copy(alpha = 0.22f)
                        else CosmicColors.NeonCyan.copy(alpha = 0.28f)
                    )

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

                    val edgeColor = if (evolutionStage >= 2) CosmicColors.NeonGold.copy(alpha = 0.55f)
                    else Color.White.copy(alpha = 0.35f)

                    listOf(
                        top to core,
                        upperLeft to core,
                        upperRight to core,
                        lowerLeft to core,
                        lowerRight to core,
                        bottom to core
                    ).forEach { (start, end) ->
                        drawLine(
                            color = edgeColor,
                            start = start,
                            end = end,
                            strokeWidth = 1.6.dp.toPx()
                        )
                    }

                    drawPath(
                        path = outerPath,
                        color = when {
                            isOvercharged || evolutionStage >= 3 -> CosmicColors.NeonGold.copy(alpha = 0.9f)
                            evolutionStage >= 2 -> CosmicColors.NeonGold.copy(alpha = 0.65f)
                            else -> CosmicColors.NeonCyan.copy(alpha = 0.85f)
                        },
                        style = Stroke(width = 2.6.dp.toPx())
                    )

                    // Critical Hit Flash Overlay
                    if (flashAlpha.value > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = flashAlpha.value),
                                    CosmicColors.NeonGold.copy(alpha = flashAlpha.value * 0.6f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius * 1.45f
                            ),
                            radius = radius * 1.45f,
                            center = center
                        )
                    }
                }

                // Burst sparks
                sparks.forEach { spark ->
                    key(spark.id) {
                        AnimatedSpark(
                            spark = spark,
                            onFinished = { sparks.removeAll { it.id == spark.id } }
                        )
                    }
                }

                // Floating "+X" numbers
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

        // Random Cosmic Comet Event Overlay
        if (isCometVisible) {
            key(cometSeed) {
                FlyingComet(
                    isVibrationEnabled = isVibrationEnabled,
                    onCatch = { x, y ->
                        val bonus = onCometTap()
                        if (bonus > 0.0) {
                            if (isVibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            val id = nextId++
                            floatingNumbers.add(
                                FloatingNumber(
                                    id = id,
                                    text = "КОМЕТА! +${NumberFormatter.format(bonus)} · x5",
                                    x = x,
                                    y = y,
                                    isCritical = true,
                                    isComet = true
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FlyingComet(
    isVibrationEnabled: Boolean,
    onCatch: (Float, Float) -> Unit
) {
    val progress = remember { Animatable(0f) }
    val startFromLeft = remember { Random.nextBoolean() }
    val yFactor = remember { 0.18f + Random.nextFloat() * 0.55f }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6200, easing = LinearEasing)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val t = progress.value
        val xFraction = if (startFromLeft) t else 1f - t
        val arcOffset = sin(t * Math.PI).toFloat() * -45f

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (xFraction * 720f).roundToInt(),
                        y = (yFactor * 420f + arcOffset).roundToInt()
                    )
                }
                .size(64.dp)
                .pointerInput(isVibrationEnabled) {
                    detectTapGestures {
                        onCatch(115f, 40f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val tailDir = if (startFromLeft) -1f else 1f

                // Glowing tail
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(CosmicColors.NeonGold, Color.Transparent),
                        start = c,
                        end = Offset(c.x + tailDir * 36.dp.toPx(), c.y + 10.dp.toPx())
                    ),
                    start = c,
                    end = Offset(c.x + tailDir * 36.dp.toPx(), c.y + 10.dp.toPx()),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Outer aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmicColors.NeonGold.copy(alpha = 0.85f),
                            CosmicColors.NeonCyan.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = c,
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx(),
                    center = c
                )

                // Core
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = c
                )
            }
        }
    }
}

@Composable
private fun AnimatedSpark(
    spark: SparkParticle,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(spark.id) {
        progress.animateTo(1f, tween(durationMillis = 420))
        onFinished()
    }

    val p = progress.value
    val curX = spark.startX + spark.vx * p
    val curY = spark.startY + spark.vy * p

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = spark.color.copy(alpha = (1f - p).coerceIn(0f, 1f)),
            radius = spark.radiusPx * (1f - p * 0.4f),
            center = Offset(curX, curY)
        )
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
                animationSpec = tween(durationMillis = 800)
            )
        }
        launch {
            delay(260L)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 540)
            )
            onFinished()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = item.text,
            color = if (item.isCritical) CosmicColors.NeonGold else CosmicColors.NeonCyan,
            fontSize = if (item.isComet) 18.sp else if (item.isCritical) 20.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (item.x - 28f).roundToInt(),
                        y = (item.y + offsetY.value - 32f).roundToInt()
                    )
                }
                .graphicsLayer { this.alpha = alpha.value }
        )
    }
}
