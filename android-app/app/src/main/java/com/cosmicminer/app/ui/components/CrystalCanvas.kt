package com.cosmicminer.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmicminer.app.model.BossUiState
import com.cosmicminer.app.model.CrystalSkin
import com.cosmicminer.app.model.FloatingNumber
import com.cosmicminer.app.model.SpaceLocation
import com.cosmicminer.app.ui.GameViewModel
import com.cosmicminer.app.ui.theme.CosmicColors
import com.cosmicminer.app.util.NumberFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private enum class ParticleStyle {
    SPARK,
    INWARD_VOID,
    MATRIX_BIT,
    SNOWFLAKE
}

private data class SparkParticle(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val color: Color,
    val radiusPx: Float,
    val style: ParticleStyle
)

private data class BgStar(
    val xFrac: Float,
    val yFrac: Float,
    val sizeDp: Float,
    val depth: Float,
    val alpha: Float,
    val tint: Color
)

@Composable
fun CrystalArena(
    rankLevel: Int,
    overchargeSecondsLeft: Int,
    feverMeter: Float,
    feverSecondsLeft: Int,
    comboCount: Int,
    isCometVisible: Boolean,
    cometSeed: Long,
    visualDroneCount: Int,
    selectedSkin: CrystalSkin,
    selectedLocation: SpaceLocation,
    bossState: BossUiState,
    isVibrationEnabled: Boolean,
    onCrystalTap: () -> GameViewModel.ClickResult,
    onCometTap: () -> Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    var rainbowHue by remember { mutableFloatStateOf(190f) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    // Gyroscope / Accelerometer parallax listener
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val gy = ((event.values[1] - 4.5f) / 9.81f).coerceIn(-1f, 1f)
                tiltX = tiltX * 0.84f + (-gx * 24f) * 0.16f
                tiltY = tiltY * 0.84f + (gy * 24f) * 0.16f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensorManager != null && accelSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val floatingNumbers = remember { mutableStateListOf<FloatingNumber>() }
    val sparks = remember { mutableStateListOf<SparkParticle>() }
    var nextId by remember { mutableLongStateOf(0L) }

    val stars = remember(selectedLocation) {
        val rnd = Random(selectedLocation.ordinal * 997 + 42)
        List(36) {
            val tint = when (selectedLocation) {
                SpaceLocation.RED_GIANT -> if (it % 3 == 0) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
                SpaceLocation.ASTEROID_BELT -> if (it % 2 == 0) Color(0xFF99F6E4) else Color(0xFFCBD5E1)
                SpaceLocation.DEEP_VOID -> if (it % 4 == 0) CosmicColors.NeonCyan else Color.White
                SpaceLocation.NEBULA -> if (it % 3 == 0) CosmicColors.NeonPurpleBright else Color.White
            }
            BgStar(
                xFrac = rnd.nextFloat(),
                yFrac = rnd.nextFloat(),
                sizeDp = 1.1f + rnd.nextFloat() * 1.9f,
                depth = 0.35f + rnd.nextFloat() * 1.25f,
                alpha = 0.25f + rnd.nextFloat() * 0.65f,
                tint = tint
            )
        }
    }

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
    val shootingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shooting_star"
    )

    val evolutionStage = when {
        rankLevel >= 150 -> 4
        rankLevel >= 100 -> 3
        rankLevel >= 50 -> 2
        else -> 1
    }
    val isOvercharged = overchargeSecondsLeft > 0
    val isFever = feverSecondsLeft > 0

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .graphicsLayer {
                translationX = shakeX.value
                translationY = shakeY.value
            }
    ) {
        val arenaWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val arenaHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

        // Parallax Space Location Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (selectedLocation) {
                SpaceLocation.NEBULA -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF3B0764).copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(size.width * 0.25f + tiltX * 0.6f, size.height * 0.35f + tiltY * 0.6f),
                            radius = size.maxDimension * 0.55f
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.25f + tiltX * 0.6f, size.height * 0.35f + tiltY * 0.6f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CosmicColors.NeonCyan.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.78f - tiltX * 0.5f, size.height * 0.68f - tiltY * 0.5f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = Offset(size.width * 0.78f - tiltX * 0.5f, size.height * 0.68f - tiltY * 0.5f)
                    )
                }
                SpaceLocation.RED_GIANT -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFEA580C).copy(alpha = 0.36f),
                                Color(0xFF7F1D1D).copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.75f + tiltX * 0.7f, size.height * 0.28f + tiltY * 0.7f),
                            radius = size.maxDimension * 0.62f
                        ),
                        radius = size.maxDimension * 0.62f,
                        center = Offset(size.width * 0.75f + tiltX * 0.7f, size.height * 0.28f + tiltY * 0.7f)
                    )
                }
                SpaceLocation.ASTEROID_BELT -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0F766E).copy(alpha = 0.26f), Color.Transparent),
                            center = Offset(size.width * 0.5f + tiltX * 0.4f, size.height * 0.5f + tiltY * 0.4f),
                            radius = size.maxDimension * 0.65f
                        ),
                        radius = size.maxDimension * 0.65f,
                        center = Offset(size.width * 0.5f + tiltX * 0.4f, size.height * 0.5f + tiltY * 0.4f)
                    )
                    for (i in 0..5) {
                        val drift = (orbitAngle / 360f + i * 0.17f) % 1f
                        val ax = size.width * drift
                        val ay = size.height * (0.14f + (i * 0.14f)) + tiltY * 0.8f
                        drawCircle(
                            color = Color(0xFF64748B).copy(alpha = 0.32f),
                            radius = (5 + (i % 3) * 3).dp.toPx(),
                            center = Offset(ax, ay)
                        )
                    }
                }
                SpaceLocation.DEEP_VOID -> {
                    drawRect(color = Color(0xFF02030A).copy(alpha = 0.55f))
                    if (shootingPhase < 0.32f) {
                        val p = shootingPhase / 0.32f
                        val sx = size.width * (0.15f + p * 0.65f)
                        val sy = size.height * (0.10f + p * 0.45f)
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.85f)),
                                start = Offset(sx - 44.dp.toPx(), sy - 28.dp.toPx()),
                                end = Offset(sx, sy)
                            ),
                            start = Offset(sx - 44.dp.toPx(), sy - 28.dp.toPx()),
                            end = Offset(sx, sy),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Parallax starfield
            stars.forEach { star ->
                val sx = (star.xFrac * size.width + tiltX * star.depth).coerceIn(0f, size.width)
                val sy = (star.yFrac * size.height + tiltY * star.depth).coerceIn(0f, size.height)
                drawCircle(
                    color = star.tint.copy(alpha = star.alpha),
                    radius = star.sizeDp.dp.toPx(),
                    center = Offset(sx, sy)
                )
            }
        }

        // Top-safe status strip & Boss HP bar (keeps top comet lane separate from central crystal)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bossState.isActive) {
                val hpFrac = (bossState.currentHp / bossState.maxHp.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF450A0A).copy(alpha = 0.82f))
                        .border(1.dp, Color(0xFFF87171).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "БОСС: ${bossState.name}",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${bossState.remainingSeconds}с",
                            color = CosmicColors.NeonGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { hpFrac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFEF4444),
                        trackColor = Color(0xFF1F2937)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFever) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                                .border(1.dp, CosmicColors.NeonGold, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ЯРОСТЬ КРИТОВ · ${feverSecondsLeft}с",
                                color = CosmicColors.NeonGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else if (comboCount >= 3) {
                        Text(
                            text = "КОМБО x$comboCount · ${(feverMeter * 100).roundToInt()}%",
                            color = CosmicColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isOvercharged) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicColors.NeonGold.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ПЕРЕГРУЗКА x5 · ${overchargeSecondsLeft}с",
                                color = CosmicColors.NeonGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Top Comet Lane (strictly above crystal tap zone to avoid accidental taps!)
        if (isCometVisible) {
            key(cometSeed) {
                FlyingComet(
                    arenaWidthPx = arenaWidthPx,
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
                                    x = x.coerceIn(70f, maxOf(80f, arenaWidthPx - 140f)),
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

        // Central Interactive Crystal / Boss Arena (shifted down slightly below top comet lane)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 26.dp)
                .size(196.dp)
                .pointerInput(
                    isVibrationEnabled,
                    selectedSkin,
                    bossState.isActive
                ) {
                    detectTapGestures { tapOffset ->
                        val result = onCrystalTap()
                        if (selectedSkin == CrystalSkin.RAINBOW_PRISM) {
                            rainbowHue = (rainbowHue + 42f) % 360f
                        }

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
                                flashAlpha.snapTo(0.72f)
                                flashAlpha.animateTo(0f, tween(240))
                            }
                            scope.launch {
                                repeat(4) {
                                    shakeX.snapTo(Random.nextFloat() * 12f - 6f)
                                    shakeY.snapTo(Random.nextFloat() * 12f - 6f)
                                    delay(26L)
                                }
                                shakeX.snapTo(0f)
                                shakeY.snapTo(0f)
                            }
                        }

                        val centerPx = Offset(size.width / 2f, size.height / 2f)
                        val pStyle = when {
                            bossState.isActive -> ParticleStyle.SPARK
                            selectedSkin == CrystalSkin.BLACK_HOLE -> ParticleStyle.INWARD_VOID
                            selectedSkin == CrystalSkin.CYBER_CUBE -> ParticleStyle.MATRIX_BIT
                            selectedSkin == CrystalSkin.ICE_RELIC -> ParticleStyle.SNOWFLAKE
                            else -> ParticleStyle.SPARK
                        }
                        val sparkCount = if (result.isCritical) 12 else 6
                        for (i in 0 until sparkCount) {
                            val angle = (Math.PI * 2.0 * i / sparkCount) + Random.nextDouble() * 0.5
                            val dist = if (result.isCritical) 85f + Random.nextFloat() * 35f else 48f + Random.nextFloat() * 30f
                            val pColor = when {
                                selectedSkin == CrystalSkin.CYBER_CUBE -> if (i % 2 == 0) Color(0xFF4ADE80) else Color(0xFF22D3EE)
                                selectedSkin == CrystalSkin.ICE_RELIC -> if (i % 2 == 0) Color(0xFFBAE6FD) else Color.White
                                selectedSkin == CrystalSkin.SOLAR_CORE -> if (i % 2 == 0) Color(0xFFFDE047) else Color(0xFFF97316)
                                selectedSkin == CrystalSkin.RAINBOW_PRISM -> Color.hsv((rainbowHue + i * 32f) % 360f, 0.85f, 1f)
                                selectedSkin == CrystalSkin.BLACK_HOLE -> if (i % 2 == 0) CosmicColors.NeonPurpleBright else CosmicColors.NeonGold
                                result.isCritical -> CosmicColors.NeonGold
                                i % 2 == 0 -> CosmicColors.NeonCyan
                                else -> CosmicColors.NeonPurpleBright
                            }
                            val sx = if (pStyle == ParticleStyle.INWARD_VOID) {
                                centerPx.x + (cos(angle) * dist).toFloat()
                            } else tapOffset.x
                            val sy = if (pStyle == ParticleStyle.INWARD_VOID) {
                                centerPx.y + (sin(angle) * dist).toFloat()
                            } else tapOffset.y
                            val tx = if (pStyle == ParticleStyle.INWARD_VOID) {
                                centerPx.x
                            } else tapOffset.x + (cos(angle) * dist).toFloat()
                            val ty = if (pStyle == ParticleStyle.INWARD_VOID) {
                                centerPx.y
                            } else tapOffset.y + (sin(angle) * dist).toFloat()

                            sparks.add(
                                SparkParticle(
                                    id = nextId++,
                                    startX = sx,
                                    startY = sy,
                                    targetX = tx,
                                    targetY = ty,
                                    color = pColor,
                                    radiusPx = if (result.isCritical) 8f else 5.2f,
                                    style = pStyle
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
                val radius = size.minDimension * 0.37f

                // Outer Fever / Combo Heat Arc
                if (feverMeter > 0.01f || isFever) {
                    val feverColor = if (isFever) CosmicColors.NeonGold else CosmicColors.NeonCyan
                    drawArc(
                        color = feverColor.copy(alpha = if (isFever) 0.95f else 0.72f),
                        startAngle = -90f,
                        sweepAngle = 360f * feverMeter.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 1.28f, center.y - radius * 1.28f),
                        size = Size(radius * 2.56f, radius * 2.56f),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Ambient Aura
                val auraColor = when {
                    bossState.isActive -> Color(0xFFEF4444)
                    isFever || isOvercharged -> CosmicColors.NeonGold
                    selectedSkin == CrystalSkin.SOLAR_CORE -> Color(0xFFF97316)
                    selectedSkin == CrystalSkin.CYBER_CUBE -> Color(0xFF22C55E)
                    selectedSkin == CrystalSkin.ICE_RELIC -> Color(0xFF38BDF8)
                    selectedSkin == CrystalSkin.RAINBOW_PRISM -> Color.hsv(rainbowHue, 0.85f, 1f)
                    else -> CosmicColors.NeonPurple
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = 0.45f),
                            CosmicColors.NeonCyan.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.5f * pulseGlow
                    ),
                    radius = radius * 1.5f * pulseGlow,
                    center = center
                )

                // Draw Either Boss Asteroid OR Selected Crystal Skin
                if (bossState.isActive) {
                    drawBossAsteroid(center, radius, orbitAngle, bossState)
                } else {
                    when (selectedSkin) {
                        CrystalSkin.CLASSIC -> drawClassicCrystal(center, radius, orbitAngle, counterOrbitAngle, evolutionStage, isOvercharged || isFever)
                        CrystalSkin.BLACK_HOLE -> drawBlackHoleSkin(center, radius, orbitAngle, counterOrbitAngle, pulseGlow)
                        CrystalSkin.SOLAR_CORE -> drawSolarCoreSkin(center, radius, orbitAngle, pulseGlow)
                        CrystalSkin.CYBER_CUBE -> drawCyberCubeSkin(center, radius, orbitAngle, counterOrbitAngle)
                        CrystalSkin.ICE_RELIC -> drawIceRelicSkin(center, radius, orbitAngle, counterOrbitAngle)
                        CrystalSkin.RAINBOW_PRISM -> drawRainbowPrismSkin(center, radius, orbitAngle, rainbowHue)
                    }
                }

                // Orbiting Fleet Drones + Periodic Laser Shots
                if (visualDroneCount > 0) {
                    val beamTargetIdx = ((orbitAngle / 36f).toInt()) % visualDroneCount
                    for (i in 0 until visualDroneCount) {
                        val baseDeg = orbitAngle * (if (i % 2 == 0) 1.4f else -1.1f) + (360f / visualDroneCount) * i
                        val rad = Math.toRadians(baseDeg.toDouble())
                        val orbitR = radius * (1.16f + (i % 3) * 0.12f)
                        val dx = center.x + (orbitR * cos(rad)).toFloat()
                        val dy = center.y + (orbitR * 0.72f * sin(rad)).toFloat()
                        val dronePos = Offset(dx, dy)

                        // Periodic laser beam from active drone
                        val beamPulse = ((orbitAngle % 36f) / 36f)
                        if (i == beamTargetIdx && beamPulse < 0.35f) {
                            drawLine(
                                color = CosmicColors.NeonCyan.copy(alpha = (1f - beamPulse / 0.35f) * 0.85f),
                                start = dronePos,
                                end = center,
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        drawCircle(
                            color = if (i % 2 == 0) CosmicColors.NeonCyan else CosmicColors.NeonGold,
                            radius = 3.2.dp.toPx(),
                            center = dronePos
                        )
                    }
                }

                // Critical Flash Overlay
                if (flashAlpha.value > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = flashAlpha.value),
                                CosmicColors.NeonGold.copy(alpha = flashAlpha.value * 0.55f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.4f
                        ),
                        radius = radius * 1.4f,
                        center = center
                    )
                }
            }

            // Sparks
            sparks.forEach { spark ->
                key(spark.id) {
                    AnimatedSpark(
                        spark = spark,
                        onFinished = { sparks.removeAll { it.id == spark.id } }
                    )
                }
            }

            // Floating numbers
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

private fun DrawScope.drawClassicCrystal(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float,
    evolutionStage: Int,
    isGolden: Boolean
) {
    rotate(degrees = orbitAngle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    if (isGolden) CosmicColors.NeonGold else CosmicColors.NeonCyan,
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
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    if (evolutionStage >= 2) {
        rotate(degrees = counterOrbitAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(CosmicColors.NeonGold.copy(alpha = 0.85f), Color.Transparent, CosmicColors.NeonCyan.copy(alpha = 0.55f), Color.Transparent),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.22f, center.y - radius * 0.82f),
                size = Size(radius * 2.44f, radius * 1.64f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

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
        isGolden -> listOf(Color(0xFFFEF08A), CosmicColors.NeonGold, Color(0xFF78350F))
        evolutionStage >= 4 -> listOf(Color(0xFF22D3EE), Color(0xFF6D28D9), Color(0xFF090518))
        evolutionStage >= 3 -> listOf(Color(0xFFFDE68A), Color(0xFF8B5CF6), Color(0xFF312E81))
        else -> listOf(Color(0xFF67E8F9), CosmicColors.NeonPurple, Color(0xFF4C1D95))
    }
    drawPath(outerPath, Brush.linearGradient(crystalColors, top, bottom))
    listOf(top, upperLeft, upperRight, lowerLeft, lowerRight, bottom).forEach { pt ->
        drawLine(
            color = if (evolutionStage >= 2) CosmicColors.NeonGold.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.35f),
            start = pt,
            end = core,
            strokeWidth = 1.5.dp.toPx()
        )
    }
    drawPath(
        outerPath,
        color = if (isGolden || evolutionStage >= 3) CosmicColors.NeonGold.copy(alpha = 0.9f) else CosmicColors.NeonCyan.copy(alpha = 0.85f),
        style = Stroke(width = 2.4.dp.toPx())
    )
}

private fun DrawScope.drawBlackHoleSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float,
    pulseGlow: Float
) {
    rotate(degrees = orbitAngle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color(0xFFF97316), Color(0xFF9333EA), Color(0xFFFDE047), Color(0xFF581C87)),
                center = center
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 0.52f),
            size = Size(radius * 2.36f, radius * 1.04f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    rotate(degrees = counterOrbitAngle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(CosmicColors.NeonCyan.copy(alpha = 0.7f), Color.Transparent, Color(0xFFC084FC), Color.Transparent),
                center = center
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.95f),
            size = Size(radius * 1.9f, radius * 1.9f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black, Color.Black, Color(0xFF6B21A8).copy(alpha = 0.85f), Color.Transparent),
            center = center,
            radius = radius * 0.86f * pulseGlow
        ),
        radius = radius * 0.86f * pulseGlow,
        center = center
    )
    drawCircle(
        color = Color.Black,
        radius = radius * 0.56f,
        center = center
    )
    drawCircle(
        color = Color(0xFFFDBA74).copy(alpha = 0.85f),
        radius = radius * 0.57f,
        center = center,
        style = Stroke(width = 2.2.dp.toPx())
    )
}

private fun DrawScope.drawSolarCoreSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    pulseGlow: Float
) {
    rotate(degrees = orbitAngle * 0.8f, pivot = center) {
        for (i in 0 until 12) {
            val a = i * (PI * 2.0 / 12.0)
            val start = Offset(center.x + (radius * 0.65f * cos(a)).toFloat(), center.y + (radius * 0.65f * sin(a)).toFloat())
            val end = Offset(center.x + (radius * 1.06f * pulseGlow * cos(a)).toFloat(), center.y + (radius * 1.06f * pulseGlow * sin(a)).toFloat())
            drawLine(
                brush = Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFEA580C).copy(alpha = 0.1f)), start, end),
                start = start,
                end = end,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF9C3), Color(0xFFFACC15), Color(0xFFEA580C), Color(0xFF9A3412)),
            center = center,
            radius = radius * 0.78f
        ),
        radius = radius * 0.78f,
        center = center
    )
}

private fun DrawScope.drawCyberCubeSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float
) {
    val drawWireCube: (Float, Float, Color) -> Unit = { r, angleDeg, col ->
        val rad = Math.toRadians(angleDeg.toDouble())
        val ptsFront = List(4) { i ->
            val a = rad + i * PI / 2.0
            Offset(center.x + (r * cos(a)).toFloat(), center.y - r * 0.14f + (r * 0.62f * sin(a)).toFloat())
        }
        val ptsBack = List(4) { i ->
            val a = rad + i * PI / 2.0
            Offset(center.x + (r * cos(a)).toFloat(), center.y + r * 0.22f + (r * 0.62f * sin(a)).toFloat())
        }
        for (i in 0 until 4) {
            val next = (i + 1) % 4
            drawLine(col, ptsFront[i], ptsFront[next], strokeWidth = 2.2.dp.toPx())
            drawLine(col.copy(alpha = 0.55f), ptsBack[i], ptsBack[next], strokeWidth = 1.6.dp.toPx())
            drawLine(col.copy(alpha = 0.8f), ptsFront[i], ptsBack[i], strokeWidth = 1.8.dp.toPx())
        }
    }
    drawWireCube(radius * 0.84f, orbitAngle, Color(0xFF4ADE80))
    drawWireCube(radius * 0.44f, counterOrbitAngle * 1.2f, CosmicColors.NeonCyan)
}

private fun DrawScope.drawIceRelicSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float
) {
    rotate(degrees = orbitAngle * 0.4f, pivot = center) {
        for (i in 0 until 6) {
            val a = i * (PI / 3.0)
            val end = Offset(center.x + (radius * 1.05f * cos(a)).toFloat(), center.y + (radius * 1.05f * sin(a)).toFloat())
            drawLine(Color(0xFFBAE6FD).copy(alpha = 0.55f), center, end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
    val top = Offset(center.x, center.y - radius * 0.90f)
    val right = Offset(center.x + radius * 0.62f, center.y)
    val bottom = Offset(center.x, center.y + radius * 0.90f)
    val left = Offset(center.x - radius * 0.62f, center.y)
    val path = Path().apply {
        moveTo(top.x, top.y)
        lineTo(right.x, right.y)
        lineTo(bottom.x, bottom.y)
        lineTo(left.x, left.y)
        close()
    }
    drawPath(path, Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0369A1)), top, bottom))
    drawLine(Color.White.copy(alpha = 0.7f), top, bottom, strokeWidth = 1.8.dp.toPx())
    drawLine(Color.White.copy(alpha = 0.5f), left, right, strokeWidth = 1.6.dp.toPx())
    drawPath(path, Color(0xFFE0F2FE), style = Stroke(width = 2.4.dp.toPx()))
    rotate(degrees = counterOrbitAngle, pivot = center) {
        drawCircle(Color(0xFF7DD3FC).copy(alpha = 0.4f), radius = radius * 1.04f, center = center, style = Stroke(1.5.dp.toPx()))
    }
}

private fun DrawScope.drawRainbowPrismSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    hue: Float
) {
    val c1 = Color.hsv(hue % 360f, 0.85f, 1f)
    val c2 = Color.hsv((hue + 110f) % 360f, 0.85f, 1f)
    val c3 = Color.hsv((hue + 220f) % 360f, 0.85f, 0.85f)

    rotate(degrees = orbitAngle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(listOf(c1, c2, c3, c1), center = center),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 1.1f, center.y - radius * 1.1f),
            size = Size(radius * 2.2f, radius * 2.2f),
            style = Stroke(width = 3.dp.toPx())
        )
    }

    val top = Offset(center.x, center.y - radius * 0.88f)
    val ur = Offset(center.x + radius * 0.6f, center.y - radius * 0.15f)
    val lr = Offset(center.x + radius * 0.42f, center.y + radius * 0.55f)
    val bot = Offset(center.x, center.y + radius * 0.88f)
    val ll = Offset(center.x - radius * 0.42f, center.y + radius * 0.55f)
    val ul = Offset(center.x - radius * 0.6f, center.y - radius * 0.15f)
    val path = Path().apply {
        moveTo(top.x, top.y); lineTo(ur.x, ur.y); lineTo(lr.x, lr.y)
        lineTo(bot.x, bot.y); lineTo(ll.x, ll.y); lineTo(ul.x, ul.y); close()
    }
    drawPath(path, Brush.linearGradient(listOf(c1, c2, c3), top, bot))
    drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(width = 2.4.dp.toPx()))
}

private fun DrawScope.drawBossAsteroid(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    bossState: BossUiState
) {
    val hpFrac = (bossState.currentHp / bossState.maxHp.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    rotate(degrees = orbitAngle * 0.35f, pivot = center) {
        val pts = List(9) { i ->
            val a = i * (PI * 2.0 / 9.0)
            val rFactor = if (i % 2 == 0) 0.92f else 0.72f
            Offset(center.x + (radius * rFactor * cos(a)).toFloat(), center.y + (radius * rFactor * sin(a)).toFloat())
        }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF991B1B), Color(0xFF451A03), Color(0xFF1F2937)),
                center = center,
                radius = radius
            )
        )
        drawPath(path, Color(0xFFF87171), style = Stroke(width = 3.dp.toPx()))

        // Damage glowing cracks when HP drops
        if (hpFrac < 0.75f) {
            drawLine(CosmicColors.NeonGold, center, pts[1], strokeWidth = 2.2.dp.toPx())
            drawLine(CosmicColors.NeonGold, center, pts[5], strokeWidth = 2.2.dp.toPx())
        }
        if (hpFrac < 0.4f) {
            drawLine(Color(0xFFEF4444), center, pts[3], strokeWidth = 2.5.dp.toPx())
            drawLine(Color(0xFFEF4444), center, pts[7], strokeWidth = 2.5.dp.toPx())
        }
    }
}

@Composable
private fun FlyingComet(
    arenaWidthPx: Float,
    isVibrationEnabled: Boolean,
    onCatch: (Float, Float) -> Unit
) {
    val progress = remember { Animatable(0f) }
    val startFromLeft = remember { Random.nextBoolean() }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6200, easing = LinearEasing)
        )
    }

    val safeTravelWidth = maxOf(220f, arenaWidthPx - 140f)
    val t = progress.value
    val xPx = if (startFromLeft) 16f + t * safeTravelWidth else (safeTravelWidth + 16f) - t * safeTravelWidth
    val yPx = 14f + sin(t * Math.PI).toFloat() * 14f

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                .size(54.dp)
                .clip(CircleShape)
                .pointerInput(isVibrationEnabled) {
                    detectTapGestures {
                        onCatch(xPx, yPx + 24f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val tailDir = if (startFromLeft) -1f else 1f

                // Outer golden tail
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(CosmicColors.NeonGold, Color(0xFFFB7185).copy(alpha = 0.55f), Color.Transparent),
                        start = c,
                        end = Offset(c.x + tailDir * 38.dp.toPx(), c.y + 4.dp.toPx())
                    ),
                    start = c,
                    end = Offset(c.x + tailDir * 38.dp.toPx(), c.y + 4.dp.toPx()),
                    strokeWidth = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Inner icy cyan plasma streak
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, CosmicColors.NeonCyan.copy(alpha = 0.6f), Color.Transparent),
                        start = c,
                        end = Offset(c.x + tailDir * 26.dp.toPx(), c.y + 2.dp.toPx())
                    ),
                    start = c,
                    end = Offset(c.x + tailDir * 26.dp.toPx(), c.y + 2.dp.toPx()),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Coma halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CosmicColors.NeonGold.copy(alpha = 0.9f), CosmicColors.NeonCyan.copy(alpha = 0.35f), Color.Transparent),
                        center = c,
                        radius = 20.dp.toPx()
                    ),
                    radius = 20.dp.toPx(),
                    center = c
                )
                drawCircle(color = Color.White, radius = 6.5.dp.toPx(), center = c)
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
    val curX = spark.startX + (spark.targetX - spark.startX) * p
    val curY = spark.startY + (spark.targetY - spark.startY) * p
    val alpha = if (spark.style == ParticleStyle.INWARD_VOID) {
        (1f - p * 0.7f).coerceIn(0f, 1f)
    } else {
        (1f - p).coerceIn(0f, 1f)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(curX, curY)
        when (spark.style) {
            ParticleStyle.MATRIX_BIT -> {
                val side = spark.radiusPx * 1.7f
                drawRect(
                    color = spark.color.copy(alpha = alpha),
                    topLeft = Offset(curX - side / 2f, curY - side / 2f),
                    size = Size(side, side)
                )
            }
            ParticleStyle.SNOWFLAKE -> {
                val arm = spark.radiusPx * 1.4f
                for (k in 0 until 3) {
                    val a = k * (PI / 3.0) + p * 1.5
                    drawLine(
                        color = spark.color.copy(alpha = alpha),
                        start = Offset(curX - (arm * cos(a)).toFloat(), curY - (arm * sin(a)).toFloat()),
                        end = Offset(curX + (arm * cos(a)).toFloat(), curY + (arm * sin(a)).toFloat()),
                        strokeWidth = 1.6f
                    )
                }
            }
            else -> {
                drawCircle(
                    color = spark.color.copy(alpha = alpha),
                    radius = spark.radiusPx * (1f - p * 0.35f),
                    center = c
                )
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
                targetValue = with(density) { -78.dp.toPx() },
                animationSpec = tween(durationMillis = 760)
            )
        }
        launch {
            delay(240L)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 520)
            )
            onFinished()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = item.text,
            color = if (item.isCritical) CosmicColors.NeonGold else CosmicColors.NeonCyan,
            fontSize = if (item.isComet) 15.sp else if (item.isCritical) 18.sp else 15.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (item.x - 26f).roundToInt(),
                        y = (item.y + offsetY.value - 26f).roundToInt()
                    )
                }
                .graphicsLayer { this.alpha = alpha.value }
        )
    }
}
